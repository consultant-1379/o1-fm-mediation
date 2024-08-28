/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.fm.o1.handlers;

import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.HREF;
import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.SYSTEM_DN;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.DN_PREFIX_MISSING;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.FAILED_TO_CHECK_ALARM_RATE;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.event.ModeledEventSender;
import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics;
import com.ericsson.oss.mediation.fm.o1.nodesuspender.NodeSuspenderConfigurationListener;
import com.ericsson.oss.mediation.fm.o1.nodesuspender.O1NodeSuspenderAgent;
import com.ericsson.oss.mediation.fm.util.EventNotificationUtil;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.o1.util.HrefUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler responsible for receiving JMS messages from the 'O1FmNotifications' queue. It extracts the object from the message. It should be a
 * {@code Map<String, Object>} containing the alarm fields. It adds the object extracted to the payload of the
 * {@code MediationComponentEvent} sent to the next handler. The node suspended status is checked if alarm flow rate detection is enabled. If the node
 * is suspended, the header 'isNodeSuspended' is set to true and the remaining handlers in the flow will ignore the notification.
 * <p>
 * This Handler must be triggered as follows:
 * <ul>
 * <li>via JMS messages received on the 'O1FmNotifications' queue.</li>
 * <ul>
 * <p>
 */
@Slf4j
@EventHandler
public class IsNodeSuspendedHandler extends AbstractFmMediationHandler {

    private static final String HANDLER = IsNodeSuspendedHandler.class.getSimpleName();

    public static final String NETWORK_ELEMENT = "NetworkElement";

    @Inject
    private O1NodeSuspenderAgent o1NodeSupendeAgent;

    @Inject
    O1HandlerStatistics o1HandlerStatistics;

    @Inject
    ModeledEventSender modeledEventSender;

    @Inject
    private NodeSuspenderConfigurationListener nodeSuspenderConfigurationListener;

    @Override
    public Object onEventWithResult(final Object inputEvent) {

        log.debug("Received event of type {} ", inputEvent.getClass().getName());
        o1HandlerStatistics.incrementTotalNoOfAlarmsReceived();

        super.onEventWithResult(inputEvent);

        final Map<String, Object> alarmObject = getNotificationObject();

        if (nodeSuspenderConfigurationListener.getAlarmRateFlowControl()) {

            checkNotificationRate(alarmObject);

        } else {

            processAlarmNotification();
        }

        return new MediationComponentEvent(headers, alarmObject);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private String getDnPrefix(final String hrefValue, final String systemDN) {
        final String dnPrefix = HrefUtil.extractDnPrefix(hrefValue);
        if (StringUtils.isBlank(dnPrefix)) {
            getRecorder().recordError(HANDLER, systemDN, hrefValue, DN_PREFIX_MISSING);
            log.error("Discarded alarm event as dnPrefix is missing from href");
            throw new EventHandlerException(DN_PREFIX_MISSING);
        }
        return dnPrefix;
    }

    private void checkNotificationRate(final Map<String, Object> alarmObject) {

        final String hrefValue = (String) alarmObject.get(HREF);
        final String systemDN = (String) alarmObject.get(SYSTEM_DN);
        final String networkElementId = FdnUtil.getMeContextId(getDnPrefix(hrefValue, systemDN));

        try {
            if (o1NodeSupendeAgent.isRegistered(networkElementId)) {
                if (o1NodeSupendeAgent.isNodeSuspended(networkElementId)) {
                    handleSuspendedNode(networkElementId);
                } else {
                    o1NodeSupendeAgent.notifyNotificationReceived(networkElementId);
                    handleNodeSuspendedAfterIncrement(networkElementId);
                }
            } else {
                o1NodeSupendeAgent.register(networkElementId);
                processAlarmNotification();
            }

        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            getRecorder().recordWarning(HANDLER, systemDN, hrefValue,
                    String.format(FAILED_TO_CHECK_ALARM_RATE, e.getMessage()));
        }
    }

    private void handleSuspendedNode(final String networkElementId) {
        o1NodeSupendeAgent.notifyNotificationReceived(networkElementId);
        discardAlarmNotification(networkElementId);
    }

    private void handleNodeSuspendedAfterIncrement(final String networkElementId) {

        if (o1NodeSupendeAgent.isNodeSuspended(networkElementId)) {
            final String networkElementFdn = NETWORK_ELEMENT + "=" + networkElementId;
            modeledEventSender.sendEventNotification(createNodeSuspendedEvent(networkElementFdn));
            discardAlarmNotification(networkElementId);
        } else {
            processAlarmNotification();
        }
    }

    private EventNotification createNodeSuspendedEvent(final String networkElementFdn) {
        final EventNotification nodeSuspendedEvent =
                EventNotificationUtil.createNodeSuspensedAlarm(networkElementFdn, "", "");
        getRecorder().recordEvent("FM_O1_NODE_SUSPENDER", "", "", "Node Suspended : Alarm raised for " + networkElementFdn);
        return nodeSuspendedEvent;
    }

    private void discardAlarmNotification(final String networkElementId) {
        headers.put("isNodeSuspended", true);
        log.trace("Node is suspended and the notification will not be processed for network element : {}", networkElementId);
    }

    private void processAlarmNotification() {
        headers.put("isNodeSuspended", false);
    }

}
