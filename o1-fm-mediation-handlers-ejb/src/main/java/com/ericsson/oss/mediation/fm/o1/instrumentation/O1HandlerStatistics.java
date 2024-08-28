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

import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;
import com.ericsson.oss.mediation.fm.o1.handlers.dps.DpsQuery;

@ApplicationScoped
@InstrumentedBean(displayName = "O1 FM Handler Monitoring", description = "O1 FM Handler Monitoring")
public class O1HandlerStatistics {

    @Inject
    private DpsQuery dpsQuery;

    private final AtomicLong totalNoOfAlarmsReceived = new AtomicLong(0L);

    private final AtomicLong totalNoOfHeartbeatsReceived = new AtomicLong(0L);

    private final AtomicLong totalNoOfSuccessfulTransformations = new AtomicLong(0L);

    private final AtomicLong totalNoOfForwardedAlarmEventNotifications = new AtomicLong(0L);

    private final AtomicLong totalNoOfForwardedSyncAlarmEventNotifications = new AtomicLong(0L);

    public void incrementTotalNoOfAlarmsReceived() {
        totalNoOfAlarmsReceived.incrementAndGet();
    }

    public void incrementTotalNoOfHeartbeatsReceived() {
        totalNoOfHeartbeatsReceived.incrementAndGet();
    }

    public void incrementTotalNoOfSuccessfulTransformations() {
        totalNoOfSuccessfulTransformations.incrementAndGet();
    }

    public void incrementTotalNoOfForwardedAlarmEventNotifications() {
        totalNoOfForwardedAlarmEventNotifications.incrementAndGet();
    }

    public void incrementTotalNoOfForwardedSyncAlarmEventNotifications(long numberOfEventNotifications) {
        totalNoOfForwardedSyncAlarmEventNotifications.addAndGet(numberOfEventNotifications);
    }

    @MonitoredAttribute(displayName = "Total number of O1Node sync alarms successfully forwarded to the FM alarm processor",
            visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
            category = MonitoredAttribute.Category.THROUGHPUT, interval = MonitoredAttribute.Interval.ONE_MIN,
            collectionType = MonitoredAttribute.CollectionType.TRENDSUP)
    public long getTotalNoOfForwardedSyncAlarmEventNotifications() {
        return totalNoOfForwardedSyncAlarmEventNotifications.get();
    }

    @MonitoredAttribute(displayName = "Total number of spontaneous alarms received from O1Nodes",
            visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
            category = MonitoredAttribute.Category.THROUGHPUT, interval = MonitoredAttribute.Interval.ONE_MIN,
            collectionType = MonitoredAttribute.CollectionType.TRENDSUP)
    public long getTotalNoOfAlarmsReceived() {
        return totalNoOfAlarmsReceived.get();
    }

    @MonitoredAttribute(displayName = "Total number of heartbeat notifications received from O1Nodes",
            visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
            category = MonitoredAttribute.Category.THROUGHPUT, interval = MonitoredAttribute.Interval.ONE_MIN,
            collectionType = MonitoredAttribute.CollectionType.TRENDSUP)
    public long getTotalNoOfHeartbeatsReceived() {
        return totalNoOfHeartbeatsReceived.get();
    }

    @MonitoredAttribute(displayName = "Total number of O1Node alarm notifications which were successfully transformed into EventNotifications",
            visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
            category = MonitoredAttribute.Category.THROUGHPUT, interval = MonitoredAttribute.Interval.ONE_MIN,
            collectionType = MonitoredAttribute.CollectionType.TRENDSUP)
    public long getTotalNoOfSuccessfulTransformations() {
        return totalNoOfSuccessfulTransformations.get();
    }

    @MonitoredAttribute(displayName = "Total number of O1Node alarms successfully forwarded to the FM alarm processor",
            visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
            category = MonitoredAttribute.Category.THROUGHPUT, interval = MonitoredAttribute.Interval.ONE_MIN,
            collectionType = MonitoredAttribute.CollectionType.TRENDSUP)
    public long getTotalNoOfForwardedAlarmEventNotifications() {
        return totalNoOfForwardedAlarmEventNotifications.get();
    }

    @MonitoredAttribute(displayName = "Total number of O1Nodes supervised for FM",
            visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
            category = MonitoredAttribute.Category.THROUGHPUT, interval = MonitoredAttribute.Interval.ONE_MIN,
            collectionType = MonitoredAttribute.CollectionType.DYNAMIC)
    public int getTotalNoOfSupervisedNodes() {
        return dpsQuery.getSupervisedListOfNodesFromDatabase().size();
    }
}
