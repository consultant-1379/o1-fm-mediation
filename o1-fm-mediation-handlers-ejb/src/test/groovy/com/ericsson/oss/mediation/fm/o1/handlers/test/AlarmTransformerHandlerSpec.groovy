package com.ericsson.oss.mediation.fm.o1.handlers.test

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.handlers.AlarmTransformerHandler
import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics
import com.ericsson.oss.mediation.fm.o1.transform.TransformManager
import com.ericsson.oss.mediation.translator.model.EventNotification

import javax.inject.Inject

class AlarmTransformerHandlerSpec extends CdiSpecification {

    @ObjectUnderTest
    private AlarmTransformerHandler alarmTransformerHandler

    @Inject
    private TransformManager transformManager

    @Inject
    private O1HandlerStatistics o1HandlerStatistics

    @MockedImplementation
    private SystemRecorder systemRecorder

    def setup() {
        def mockEventHandlerContext = mock(EventHandlerContext)
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> Collections.emptyMap()
        alarmTransformerHandler.init(mockEventHandlerContext)
    }

    def "Test handler with event that contains empty payload"() {
        given: "an input event that contains an empty payload"
            def inputEvent = new MediationComponentEvent(Collections.emptyMap(), "")

        when: "the handler is invoked"
            final MediationComponentEvent result = alarmTransformerHandler.onEventWithResult(inputEvent) as MediationComponentEvent

        then: "the event is ignored and the payload is set to empty"
            final String payload = result.getPayload()
            assert payload.isEmpty()

        and: "the alarm is not sent to the transform manager"
            0 * transformManager.translateAlarm(_)
    }

    def "Test handler translates alarm successfully"() {
        given: "a valid input event that contains a map of alarm properties"
            def inputEvent = new MediationComponentEvent(Collections.emptyMap(), ["notificationType": "notifyNewAlarm",
                                                                                  "href"            : "http://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1", "alarmId": "1"])

        when: "the handler is invoked"
            final MediationComponentEvent result = alarmTransformerHandler.onEventWithResult(inputEvent) as MediationComponentEvent

        then: "the handler returns a result that contains a payload with the transformed properties."
            assert result.getPayload() instanceof EventNotification

        and: "successful transformation statistics updated"
            o1HandlerStatistics.getTotalNoOfSuccessfulTransformations() == 1
    }

    def "Test handler throws exception when payload is empty"() {
        given: "an input event that contains payload with empty properties"
            def inputEvent = new MediationComponentEvent(Collections.emptyMap() as Map<String, Object>, Collections.emptyMap())

        when: "the handler is invoked"
            final MediationComponentEvent result = alarmTransformerHandler.onEventWithResult(inputEvent) as MediationComponentEvent

        then: "the handler throws an exception with correct message."
            def exception = thrown(EventHandlerException)
            exception.message.contains("Failed to transform alarm(s)")

        and: "system recorder message was created"
            1 * systemRecorder.recordError("com.ericsson.oss.mediation.fm.o1.handlers.AlarmTransformerHandler",
                    ErrorSeverity.ERROR, _, _,
                    "Discarded alarm event due to issue during transformation: Received request to transform alarm with no properties.")
    }

}

