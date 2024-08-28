package com.ericsson.oss.mediation.fm.o1.nodesuspender

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification

import spock.lang.Subject

class NodeSuspenderConfigurationListenerSpec extends CdiSpecification {

    @Subject
    @ObjectUnderTest
    private NodeSuspenderConfigurationListener nodeSuspenderConfigurationListener

    def "Test listener is listening for alarm rate flow control changes"() {
        given: "AlarmRateFlowControl value set to true"
            nodeSuspenderConfigurationListener.alarmRateFlowControl = true
        when: "AlarmRateFlowControl value is updated"
            nodeSuspenderConfigurationListener.listenForAlarmRateFlowControlChanges(false)
        then: "AlarmRateFlowControl is updated with the latest"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() == false

    }

    def "Test listener is listening for alarm check interval changes"() {
        given: "AlarmRateCheckInterval value set to 12"
            nodeSuspenderConfigurationListener.alarmRateCheckInterval = 12
        when: "AlarmRateCheckInterval value is updated"
            nodeSuspenderConfigurationListener.listenForAlarmCheckIntervalChanges(34)
        then: "AlarmRateCheckInterval is updated with the latest"
            nodeSuspenderConfigurationListener.getAlarmRateCheckInterval() == 34

    }

    def "Test listener is listening for alarm rate normal threshold changes"() {
        given: "AlarmRateNormalThreshold value set to 12"
            nodeSuspenderConfigurationListener.alarmRateNormalThreshold = 12
        when: "AlarmRateNormalThreshold value is updated"
            nodeSuspenderConfigurationListener.listenForAlarmRateNormalThresholdChanges(23)
        then: "AlarmRateNormalThreshold is updated with the latest"
            nodeSuspenderConfigurationListener.getAlarmRateNormalThreshold() == 23

    }

    def "Test listener is listening for alarm rate threshold changes"() {
        given: "AlarmRateThreshold value set to 14"
            nodeSuspenderConfigurationListener.alarmRateThreshold = 14
        when: "AlarmRateThreshold value is updated"
            nodeSuspenderConfigurationListener.listenForAlarmRateThresholdChanges(12)
        then: "AlarmRateThreshold is updated with the latest"
            nodeSuspenderConfigurationListener.getAlarmRateThreshold() == 12

    }

}
