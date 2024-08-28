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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractYangHandler;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler responsible for updating the AlarmList.administrativeState attribute to UNLOCKED.
 */
@Slf4j
@EventHandler
public class UnlockAlarmListHandler extends O1AbstractYangHandler {

    public static final String ALARM_LIST = "AlarmList";
    public static final String ALARM_LIST_RDN = "AlarmList=1";
    public static final String ADMINISTRATIVE_STATE = "administrativeState";

    public static final String UNLOCKED = "UNLOCKED";

    @Override
    protected boolean skipHandler() {
        log.trace("Skipping as supervision is set to [{}]", isActivate());
        return !isActivate();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected void addSpecificYangData() {

        final String fmAlarmSupervisionFdn = (String) moData.getHeaders().get(FDN);
        final String networkElementFdn = FdnUtil.getParentFdn(fmAlarmSupervisionFdn);
        final String meContextFdn = dpsRead.getMeContextFdn(networkElementFdn);
        moData.setFdn(FdnUtil.createFdn(FdnUtil.getManagedElementFdn(meContextFdn, (String) headers.get(MANAGED_ELEMENT_ID)), ALARM_LIST_RDN));

        moData.setType(ALARM_LIST);
        moData.setName("1");

        final Map<String, Object> modifiedAttributes = new HashMap<>();
        modifiedAttributes.put(ADMINISTRATIVE_STATE, UNLOCKED);

        moData.setOperation(Operation.MERGE);
        moData.setModifyAttributes(modifiedAttributes);
    }
}
