/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.fm.o1.instrumentation;

import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgent;

@ApplicationScoped
@InstrumentedBean(displayName = "O1 FM Heartbeat Statistics Monitoring", description = "O1 FM Heartbeat Statistics Monitoring")
public class O1FmHeartbeatStatistics {

    @EServiceRef
    private O1HeartbeatAgent o1HeartbeatAgent;

    @MonitoredAttribute(displayName = "Total number of O1Nodes in heartbeat failure",
            visibility = MonitoredAttribute.Visibility.ALL,
            units = MonitoredAttribute.Units.NONE,
            category = MonitoredAttribute.Category.THROUGHPUT,
            interval = MonitoredAttribute.Interval.ONE_MIN,
            collectionType = MonitoredAttribute.CollectionType.DYNAMIC)
    public int getTotalNoOfHeartbeatFailures() {
        return o1HeartbeatAgent.getHbFailures(FM).size();
    }
}
