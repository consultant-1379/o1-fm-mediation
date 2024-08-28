package com.ericsson.oss.mediation.fm.o1.handlers.test

import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.DN_PREFIX_MISSING
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.FAILED_TO_GET_JMS_MESSAGE_FROM_INPUT

import javax.inject.Inject
import javax.jms.JMSException
import javax.jms.ObjectMessage

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity
import com.ericsson.oss.itpf.sdk.recording.EventLevel
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.common.event.ModeledEventSender
import com.ericsson.oss.mediation.fm.o1.handlers.IsNodeSuspendedHandler
import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics
import com.ericsson.oss.mediation.fm.o1.nodesuspender.NodeSuspenderConfigurationListener
import com.ericsson.oss.mediation.fm.o1.nodesuspender.O1NodeSuspenderAgent

class IsNodeSuspendedHandlerSpec extends CdiSpecification {

    @ObjectUnderTest
    private IsNodeSuspendedHandler isNodeSuspendedHandler

    @Inject
    private SystemRecorder systemRecorder

    @MockedImplementation
    private O1NodeSuspenderAgent o1NodeSuspenderAgent

    @MockedImplementation
    private NodeSuspenderConfigurationListener nodeSuspenderConfigurationListener

    @MockedImplementation
    private ModeledEventSender modeledEventSender

    @Inject
    private O1HandlerStatistics o1HandlerStatistics

    def setup() {
        def mockEventHandlerContext = mock(EventHandlerContext)
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> new HashMap<>()
        isNodeSuspendedHandler.init(mockEventHandlerContext)
    }

    def "Test when JMS message input and message cannot be retrieved from JMS object"() {

        given: "a input JMS object that throws an exception when getObject is called"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> { throw new JMSException("some internal issue") }

        when: "the handler is invoked"
            isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "an exception with an appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains(FAILED_TO_GET_JMS_MESSAGE_FROM_INPUT)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }

    def "Test when JMS message input and message does not contain map of alarm properties"() {

        given: "an input JMS message that has no alarm properties map"
            def inputEvent = mock(ObjectMessage)

        when: "the handler is invoked"
            isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "an exception with an appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains("Invalid inputEvent: JMS message does not contain the expected Map type")

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input with no href in payload"() {

        given: "an valid input event with null href value"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "systemDN": null, "href": null]

        and: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> true

        when: "the handler is invoked"
            isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "error is logged and an exception with appropriate message is thrown"
            1 * systemRecorder.recordError("IsNodeSuspendedHandler", _, _, _, DN_PREFIX_MISSING)
            def exception = thrown(EventHandlerException)
            exception.message.contains(DN_PREFIX_MISSING)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input with no dnPrefix in href"() {

        given: "an valid input event with no dnPrefix"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "systemDN": "ManagedElement=1,MnsAgent=FM", "href": "https://ManagedElement=ocp83vcu03o1/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> true

        when: "the handler is invoked"
            isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "error is logged and an exception with appropriate message is thrown"
            1 * systemRecorder.recordError("IsNodeSuspendedHandler", _, _, _, DN_PREFIX_MISSING)
            def exception = thrown(EventHandlerException)
            exception.message.contains(DN_PREFIX_MISSING)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input and message contains valid map of alarm properties"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"

            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["prop1": "value", "prop2": "value", "notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> true

        when: "the handler is invoked"
            final MediationComponentEvent result = isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "the handler returns a result that contains a payload with the alarm properties."
            result.getPayload() == ["prop1": "value", "prop2": "value", "notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input when alarm flow rate is disabled"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is disabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> false

        when: "the handler is invoked"
            final MediationComponentEvent result = isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "the handler returns a result that contains a payload with the alarm properties."
            result.getPayload() == ["notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "no call made to update node suspender cache and node suspended alarm is not raised"
            0 * isNodeSuspendedHandler.o1NodeSupendeAgent.notifyNotificationReceived(_)
            0 * modeledEventSender.sendEventNotifications(_)

        and: "header is added with choice operator attribute set to false"
            result.getHeaders().get("isNodeSuspended") == false

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input with alarm flow rate is enabled and node is suspended before incrementing the notification count"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "systemDN": "ManagedElement=1,MnsAgent=FM", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> true

        and: "node is suspended"
            1 * o1NodeSuspenderAgent.isNodeSuspended("NodeA") >> true

        and: "node is present in cache"
            o1NodeSuspenderAgent.isRegistered(_) >> true

        when: "the handler is invoked"
            final MediationComponentEvent result = isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "notification count is incremented"
            1 * isNodeSuspendedHandler.o1NodeSupendeAgent.notifyNotificationReceived(_)

        and: "header is added with choice operator attribute set to true"
            result.getHeaders().get("isNodeSuspended") == true

        and: "no call made to update node suspender cache and node suspended alarm is not raised"
            0 * modeledEventSender.sendEventNotifications(_)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }

    def "Test when JMS message input with alarm flow rate is enabled and node is not suspended"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "systemDN": "ManagedElement=1,MnsAgent=FM", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> true

        and: "node is not suspended"
            o1NodeSuspenderAgent.isNodeSuspended("NodeA") >> false

        and: "node is present in cache"
            o1NodeSuspenderAgent.isRegistered(_) >> true

        when: "the handler is invoked"
            final MediationComponentEvent result = isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "the handler returns a result that contains a payload with the alarm properties."
            result.getPayload() == ["notificationType": "notifyNewAlarm", "systemDN": "ManagedElement=1,MnsAgent=FM", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "notification count is incremented"
            1 * isNodeSuspendedHandler.o1NodeSupendeAgent.notifyNotificationReceived(_)

        and: "no call made to update node suspender cache and node suspended alarm is not raised"
            0 * isNodeSuspendedHandler.o1NodeSupendeAgent.notifyNotificationReceived(_)
            0 * modeledEventSender.sendEventNotifications(_)

        and: "the choice operator attribute is not added to the headers"
            result.getHeaders().get("isNodeSuspended") == false

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }

    def "Test when JMS message input with node is suspended after incrementing the notification count"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "systemDN": "ManagedElement=1,MnsAgent=FM", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> true

        and: "node is not initially suspended"
            1 * o1NodeSuspenderAgent.isNodeSuspended("NodeA") >> false

        and: "node is present in cache"
            o1NodeSuspenderAgent.isRegistered(_) >> true

        and: "node is suspended after its incremented"
            1 * o1NodeSuspenderAgent.isNodeSuspended("NodeA") >> true

        when: "the handler is invoked"
            final MediationComponentEvent result = isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "node is added to the cache"
            1 * isNodeSuspendedHandler.o1NodeSupendeAgent.notifyNotificationReceived(_)

        and: "node suspended alarm is raised"
            1 * modeledEventSender.sendEventNotification({
                it.getProbableCause().contains("Threshold crossed") &&
                        it.getSpecificProblem().contains("Alarm Rate Threshold Crossed") &&
                        it.getEventType().contains("Quality of service") &&
                        it.getPerceivedSeverity().contains("CRITICAL") &&
                        it.getRecordType().contains("NODE_SUSPENDED")
            })
            1 * systemRecorder.recordEvent('FM_O1_NODE_SUSPENDER', EventLevel.DETAILED, '', '', 'Node Suspended : Alarm raised for NetworkElement=NodeA')

        and: "header is added with choice operator attribute set to true"
            result.getHeaders().get("isNodeSuspended") == true

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }

    def "Test when JMS message input with node that is not in the cache and is added to cache"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "systemDN": "ManagedElement=1,MnsAgent=FM", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> true

        and: "node is not present in cache"
            o1NodeSuspenderAgent.isRegistered(_) >> false

        when: "the handler is invoked"
            isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "node is added to the cache"
            1 * isNodeSuspendedHandler.o1NodeSupendeAgent.register(_)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input with node that is not in the cache and exception is thrown when adding to cache"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "systemDN": "ManagedElement=1,MnsAgent=FM", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> true

        and: "exception is thrown when node is added to the cache"
            isNodeSuspendedHandler.o1NodeSupendeAgent.register(_) >> new Exception()

        and: "node is not present in cache"
            o1NodeSuspenderAgent.isRegistered(_) >> false

        when: "the handler is invoked"
            final MediationComponentEvent result = isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "exception is caught and error is logged"
            1 * systemRecorder.recordError("IsNodeSuspendedHandler", ErrorSeverity.WARNING, _, _, _)
            notThrown(Exception)

        and: "the alarm is processed"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
            Map payload = result.getPayload() as HashMap<String, Object>
            assert payload.get("notificationType") == "notifyNewAlarm"
    }

    def "Test when JMS message input with node that is in the cache and exception is thrown when updating to cache"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "systemDN": "ManagedElement=1,MnsAgent=FM", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl() >> true

        and: "exception is thrown when cache is updated"
            isNodeSuspendedHandler.o1NodeSupendeAgent.notifyNotificationReceived(_) >> new Exception()

        and: "node is present in cache"
            o1NodeSuspenderAgent.isRegistered(_) >> true

        when: "the handler is invoked"
            final MediationComponentEvent result = isNodeSuspendedHandler.onEventWithResult(inputEvent)

        then: "exception is caught and error is logged"
            1 * systemRecorder.recordError("IsNodeSuspendedHandler", ErrorSeverity.WARNING, _, _, _)
            notThrown(Exception)

        and: "the alarm is processed"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
            Map payload = result.getPayload() as HashMap<String, Object>
            assert payload.get("notificationType") == "notifyNewAlarm"

    }
}
