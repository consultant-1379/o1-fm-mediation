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

import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.SYNCHRONIZATION_ALARM;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.event.ModeledEventSender;
import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler extracts the {@code EventNotification} received and sends it to FM via the {@code O1AlarmService}.
 */
@Slf4j
@EventHandler
public class EventNotificationDispatchHandler extends AbstractFmMediationHandler {

    @Inject
    protected SystemRecorder readSystemRecorder;

    @Inject
    protected O1HandlerStatistics o1HandlerStatistics;

    @Inject
    private ModeledEventSender modeledEventSender;

    public static final String EVENT_ID_FM_NOTIFICATION_SENDING_ALARM_ERROR = "O1NODE_FM_NOTIFICATION.SENDING_ALARM_ERROR";

    public static final String MSG_ALARM_CANNOT_BE_SENT = " Failed to send alarm(s) to FM service.";

    protected String getHandlerName() {
        return EventNotificationDispatchHandler.class.getName();
    }

    @Override
    public Object onEventWithResult(final Object inputEvent) {
        super.onEventWithResult(inputEvent);
        try {
            if (payload instanceof EventNotification) {
                log.debug("Sending alarm");
                modeledEventSender.sendEventNotification((EventNotification) payload);
                o1HandlerStatistics.incrementTotalNoOfForwardedAlarmEventNotifications();
            }

            if (payload instanceof List && containsInstancesOf((List<?>) payload, EventNotification.class)) {
                log.debug("Sending alarm list");
                modeledEventSender.sendEventNotifications((List<EventNotification>) payload);
                o1HandlerStatistics.incrementTotalNoOfForwardedSyncAlarmEventNotifications(((List<EventNotification>) payload).stream()
                        .filter(eventNotification -> eventNotification.getRecordType().equals(SYNCHRONIZATION_ALARM))
                        .count());
            }

        } catch (final Exception e) {
            if (payload instanceof List) {
                String resource = ((List<EventNotification>) payload).get(0).getManagedObjectInstance();
                readSystemRecorder.recordError(EVENT_ID_FM_NOTIFICATION_SENDING_ALARM_ERROR, ErrorSeverity.ERROR, getHandlerName(), resource,
                        MSG_ALARM_CANNOT_BE_SENT);
            } else {
                String resource = ((EventNotification) payload).getManagedObjectInstance();
                readSystemRecorder.recordError(EVENT_ID_FM_NOTIFICATION_SENDING_ALARM_ERROR, ErrorSeverity.ERROR, getHandlerName(), resource,
                        MSG_ALARM_CANNOT_BE_SENT);
            }
            log.error(MSG_ALARM_CANNOT_BE_SENT, e);
        }

        return new MediationComponentEvent(headers, StringUtils.EMPTY);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
