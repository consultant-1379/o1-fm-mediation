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
package com.ericsson.oss.mediation.fm.heartbeat.service

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender
import com.ericsson.oss.mediation.o1.fm.heartbeat.service.O1FmHeartbeatIntervalTimer
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgent
import com.ericsson.oss.mediation.translator.model.EventNotification
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch

import javax.ejb.Timer

import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM

class O1FmHeartbeatIntervalTimerSpec extends CdiSpecification {

    @ObjectUnderTest
    O1FmHeartbeatIntervalTimer o1HeartBeatIntervalTimer

    @MockedImplementation
    EventSender<EventNotificationBatch> eventSender

    @MockedImplementation
    O1HeartbeatAgent o1HeartbeatAgent

    def "Test timeout when there are no heartbeat alarms to be raised or cleared"() {
        given:
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM) >> []
            o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM) >> []
        when:
            o1HeartBeatIntervalTimer.timeout(Mock(Timer.class))
        then:
            0 * eventSender.send(_)
    }

    def "Test timeout when there is a heartbeat alarm to be raised"() {
        given:
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM) >> ["NetworkElement=node1"]
            o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM) >> []
        when:
            o1HeartBeatIntervalTimer.timeout(Mock(Timer.class))
        then:
            1 * eventSender.send(_) >> { EventNotificationBatch it ->
                assertEventNotification(it, "HEARTBEAT_ALARM", "CRITICAL")
            }
    }

    def "Test timeout when there is a heartbeat alarm to be cleared"() {
        given:
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM) >> []
            o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM) >> ["NetworkElement=node1"]
        when:
            o1HeartBeatIntervalTimer.timeout(Mock(Timer.class))
        then:
            1 * eventSender.send(_) >> { EventNotificationBatch it ->
                assertEventNotification(it, "HEARTBEAT_ALARM", "CLEARED")
            }
    }

    private void assertEventNotification(EventNotificationBatch actual,
                                         String expectedRecordType, String expectedPerceivedSeverity) {
        List<EventNotification> eventNotifications = new ObjectInputStream(new ByteArrayInputStream(actual.serializedData)).readObject()
        assert eventNotifications.size() == 1
        assert eventNotifications.get(0).getRecordType() == expectedRecordType
        assert eventNotifications.get(0).getPerceivedSeverity() == expectedPerceivedSeverity
    }
}
