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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf;

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.FDN;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.MANAGED_ELEMENT_ID;

import java.util.Collections;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult;
import com.ericsson.oss.mediation.fm.o1.common.event.ModeledEventSender;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractYangHandler;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler responsible for reading the existing alarms from an O1Node type.
 * <p>
 * Alarms are read from the AlarmList.alarmRecords attribute.
 */
@Slf4j
@EventHandler
public class ReadAlarmListHandler extends O1AbstractYangHandler {

    @Inject
    private ModeledEventSender modeledEventSender;

    public static final String ALARM_LIST = "AlarmList";
    public static final String ALARM_LIST_RDN = "AlarmList=1";
    public static final String ALARM_RECORDS = "alarmRecords";

    @Override
    protected void processYangResponse(final YangNetconfOperationResult yangResult) {

        headers.put("fdn", moData.getFdn());
        payload = ((NetconfResponse) yangResult.getOperationResult()).getData();

    }

    @Override
    protected void postExecuteYangOperation(final YangNetconfOperationResult yangResult, final O1NetconfOperationStatus operationStatus) {

        final O1NetconfOperationResult operationResult = operationStatus.getResult();

        if (!operationResult.equals(O1NetconfOperationResult.OPERATION_SUCCESS)) {
            if (operationResult.equals(O1NetconfOperationResult.OPERATION_ABORTED)) {
                recordWarning(operationStatus, operationResult.name());
            } else if (operationResult.equals(O1NetconfOperationResult.OPERATION_FAILED)) {
                recordError(operationStatus);
            }
            modeledEventSender.sendEventNotification(createSyncAbortedAlarm(getAlarmListFdn()));
        }
    }

    @Override
    protected boolean skipHandler() {
        return !isActivate();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected void addSpecificYangData() {
        moData.setFdn(getAlarmListFdn());
        moData.setType(ALARM_LIST);
        moData.setName("1");
        moData.setReadAttributes(Collections.singletonList(ALARM_RECORDS));
    }

    private EventNotification createSyncAbortedAlarm(final String fdn) {
        EventNotification eventNotification = com.ericsson.oss.mediation.fm.util.EventNotificationUtil
                .createSyncAlarm(fdn, "", "", "SYNCHRONIZATION_ABORTED");
        eventNotification.addAdditionalAttribute("fdn", FdnUtil.getNetworkElementFdn(fdn));
        log.debug("Created EventNotification: {}", eventNotification);
        return eventNotification;
    }

    private String getAlarmListFdn() {
        if (StringUtils.isBlank(moData.getFdn())) {
            final String fmAlarmSupervisionFdn = (String) moData.getHeaders().get(FDN);
            final String networkElementFdn = FdnUtil.getParentFdn(fmAlarmSupervisionFdn);
            final String meContextFdn = dpsRead.getMeContextFdn(networkElementFdn);
            return FdnUtil.createFdn(FdnUtil.getManagedElementFdn(meContextFdn, (String) headers.get(MANAGED_ELEMENT_ID)), ALARM_LIST_RDN);
        }
        return moData.getFdn();
    }
}
