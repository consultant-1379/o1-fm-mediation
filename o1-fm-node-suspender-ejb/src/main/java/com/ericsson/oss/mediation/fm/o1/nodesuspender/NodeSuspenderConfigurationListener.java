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

package com.ericsson.oss.mediation.fm.o1.nodesuspender;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

import lombok.extern.slf4j.Slf4j;

/**
 * Listener class for the configuration parameters related to node suspender functionality.
 */
@ApplicationScoped
@Slf4j
public class NodeSuspenderConfigurationListener {

    @Inject
    @NotNull
    @Configured(propertyName = "ALARMRATE_FLOW_CONTROL")
    private Boolean alarmRateFlowControl;

    @Inject
    @NotNull
    @Configured(propertyName = "ALARMRATE_CHECK_INTERVAL")
    private int alarmRateCheckInterval;

    @Inject
    @NotNull
    @Configured(propertyName = "ALARMRATE_NORMAL_THRESHOLD")
    private int alarmRateNormalThreshold;

    @Inject
    @NotNull
    @Configured(propertyName = "ALARMRATE_THRESHOLD")
    private int alarmRateThreshold;

    public Boolean getAlarmRateFlowControl() {
        return alarmRateFlowControl;
    }

    public int getAlarmRateCheckInterval() {
        return alarmRateCheckInterval;
    }

    public int getAlarmRateNormalThreshold() {
        return alarmRateNormalThreshold;
    }

    public int getAlarmRateThreshold() {
        return alarmRateThreshold;
    }

    public void listenForAlarmRateFlowControlChanges(
            @Observes @ConfigurationChangeNotification(propertyName = "ALARMRATE_FLOW_CONTROL") final Boolean newValue) {
        log.info("alarmRateFlowControl received change notification, previous value [{}] new value [{}]", alarmRateFlowControl, newValue);
        this.alarmRateFlowControl = newValue;

    }

    public void
            listenForAlarmRateThresholdChanges(@Observes @ConfigurationChangeNotification(propertyName = "ALARMRATE_THRESHOLD") final int newValue) {
        log.info("alarmRateThreshold received change notification, previous value [{}] new value [{}]", alarmRateThreshold, newValue);
        this.alarmRateThreshold = newValue;
    }

    public void listenForAlarmRateNormalThresholdChanges(
            @Observes @ConfigurationChangeNotification(propertyName = "ALARMRATE_NORMAL_THRESHOLD") final int newValue) {
        log.info("alarmRateNormalThreshold received change notification, previous value [{}] new value [{}]", alarmRateNormalThreshold, newValue);
        this.alarmRateNormalThreshold = newValue;
    }

    public void listenForAlarmCheckIntervalChanges(
            @Observes @ConfigurationChangeNotification(propertyName = "ALARMRATE_CHECK_INTERVAL") final int newValue) {
        log.info("alarmRateCheckInterval received change notification, previous value [{}] new value [{}]", alarmRateCheckInterval, newValue);
        this.alarmRateCheckInterval = newValue;
    }

}
