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
package com.ericsson.oss.mediation.fm.o1.handlers.test

import static com.ericsson.oss.itpf.sdk.recording.ErrorSeverity.ERROR

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.common.event.ModeledEventSender
import com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler
import com.ericsson.oss.mediation.translator.model.EventNotification

class EventNotificationDispatchHandlerSpec extends CdiSpecification {

    @ObjectUnderTest
    private EventNotificationDispatchHandler eventNotifDispatchHandler

    @MockedImplementation
    private ModeledEventSender modeledEventSender

    @Inject
    private SystemRecorder SystemRecorder

    def setup() {
        def mockEventHandlerContext = mock(EventHandlerContext)
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> Collections.emptyMap()
        eventNotifDispatchHandler.init(mockEventHandlerContext)
    }

    def "Test handler with event that contains empty payload"() {

        given: "an input event that contains an empty body"
            def inputEvent = new MediationComponentEvent(Collections.emptyMap(), "")

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent) as MediationComponentEvent

        then: "the event is ignored and the payload is set to empty"
            final String payload = result.getPayload()
            assert payload.isEmpty()

        and: "there are no alarms sent to the alarm service"
            0 * eventNotifDispatchHandler.modeledEventSender.sendEventNotifications(_)
    }

    def "Test handler with event that contains empty list"() {

        given: "an input event that contains an empty body"
            List<EventNotification> alarmList = new ArrayList<>()
            def inputEvent = new MediationComponentEvent(Collections.emptyMap(), alarmList)

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent)

        then: "the event is ignored and the payload is set to empty"
            final String payload = result.getPayload()
            assert payload.isEmpty()

        and: "there are no alarms sent to the alarm service"
            0 * eventNotifDispatchHandler.modeledEventSender.sendEventNotifications(alarmList)
    }

    def "Test handler with a valid input event"() {

        given: "a valid input event that contains a translated alarm"
            final EventNotification alarm = new EventNotification()
            def inputEvent = new MediationComponentEvent(Collections.emptyMap(), alarm)

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent) as MediationComponentEvent

        then: "the model event sender is successfully invoked for the translated alarm"
            1 * eventNotifDispatchHandler.modeledEventSender.sendEventNotification(alarm)
    }

    def "Test handler with a valid list input event"() {

        given: "a valid input event that contains a translated alarm"
            final EventNotification alarm = new EventNotification()
            List<EventNotification> testAlarmList = new ArrayList<>()
            testAlarmList.add(alarm)
            testAlarmList.add(alarm)
            testAlarmList.add(alarm)

            def inputEvent = new MediationComponentEvent(Collections.emptyMap(), testAlarmList)


        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent)

        then: "the model event sender is successfully invoked for the translated alarm"
            1 * eventNotifDispatchHandler.modeledEventSender.sendEventNotifications(testAlarmList)
    }


    def "Test handler with an invalid event"() {

        given: "an invalid input event"

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(null) as MediationComponentEvent

        then: "Exception was thrown from handler"
            EventHandlerException exception = thrown()
            exception.message == "Internal error - input event received is null"

    }

    def "Test handler with exception sending event to FM"(){

        setup: "model event sender to throw exception"
            eventNotifDispatchHandler.modeledEventSender.sendEventNotification(_) >> { throw new Exception() }

        and: "an alarm to be sent to FM service"
            final EventNotification alarm = new EventNotification()
            alarm.setManagedObjectInstance("ManagedElement=1,GNBCUCPFunction=1")

        when: "the handler is invoked for the alarm"
            def inputEvent = new MediationComponentEvent(Collections.emptyMap(), alarm)
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent) as MediationComponentEvent

        then: "The exception is caught"
            notThrown(Exception)

        and: "an error is logged with the system recorder"
            1 * SystemRecorder.recordError('O1NODE_FM_NOTIFICATION.SENDING_ALARM_ERROR',
                    ERROR,
                    'com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler',
                    alarm.getManagedObjectInstance(),
                    ' Failed to send alarm(s) to FM service.');
    }

    def "Test handler with exception sending list of events to FM"(){

        setup: "An exception occurs when sending alarm"
            eventNotifDispatchHandler.modeledEventSender.sendEventNotifications(_) >> { throw new Exception() }

        and: "a list of alarms to be sent to FM service"
            final EventNotification alarm = new EventNotification()
            List<EventNotification> testAlarmList = new ArrayList<>()
            alarm.setManagedObjectInstance("ManagedElement=1,GNBCUCPFunction=1")
            testAlarmList.add(alarm)
            testAlarmList.add(alarm)

        when: "the handler is invoked for the alarm"
            def inputEvent = new MediationComponentEvent(Collections.emptyMap(), testAlarmList)
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent) as MediationComponentEvent

        then: "The exception is caught"
            notThrown(Exception)

        then: "an error is logged with the system recorder"
            1 * SystemRecorder.recordError('O1NODE_FM_NOTIFICATION.SENDING_ALARM_ERROR',
                    ERROR,
                    'com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler',
                    alarm.getManagedObjectInstance(),
                    ' Failed to send alarm(s) to FM service.');
    }

    def "Test handler with event that contains invalid list"() {

        given: "an input event that contains a String list"
            List<String> alarmList = new ArrayList<>()
            alarmList.add("TestValue")
            def inputEvent = new MediationComponentEvent(Collections.emptyMap(), alarmList)

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent)

        then: "the event is ignored and the payload is set to empty"
            final String payload = result.getPayload()
            assert payload.isEmpty()

        and: "there are no alarms sent to the model event sender"
            0 * eventNotifDispatchHandler.modeledEventSender.sendEventNotifications(alarmList)
    }
}

