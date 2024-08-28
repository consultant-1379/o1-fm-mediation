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

package com.ericsson.oss.mediation.fm.o1.handlers.util;

import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.FM_ALARM_SUPERVISION_RDN;

import java.util.UUID;

import com.ericsson.oss.mediation.core.events.MediationClientType;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.services.fm.service.model.FmMediationAlarmSyncRequest;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class MediationTaskRequestUtil {

    public static FmMediationAlarmSyncRequest createFmMediationAlarmSyncRequest(final String networkElementFdn) {

        final FmMediationAlarmSyncRequest event = new FmMediationAlarmSyncRequest();
        event.setClientType(MediationClientType.EVENT_BASED.name());
        event.setProtocolInfo("FM");
        event.setJobId(UUID.randomUUID().toString());
        event.setNodeAddress(FdnUtil.createFdn(networkElementFdn, FM_ALARM_SUPERVISION_RDN));

        return event;
    }
}
