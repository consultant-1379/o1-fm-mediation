package com.ericsson.oss.mediation.fm.o1.handlers.test

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.ImplementationInstance
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.handlers.FmSupervisionServiceStateHandler
import com.ericsson.oss.mediation.fm.o1.nodesuspender.O1NodeSuspenderAgent
import com.ericsson.oss.mediation.fm.supervision.response.AlarmSupervisionResponse
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest

import spock.lang.Unroll

class FmSupervisionServiceStateHandlerSpec extends CdiSpecification {

    private static final String NETWORK_ELEMENT_FDN = "NetworkElement=NodeA"
    private static final String FM_ALARM_SUPERVISION_FDN = NETWORK_ELEMENT_FDN + ",FmAlarmSupervision=1"
    private static final String FM_FUNCTION_FDN = NETWORK_ELEMENT_FDN + ",FmFunction=1"

    @ObjectUnderTest
    private FmSupervisionServiceStateHandler fmSupervisionServiceStateHandler

    @ImplementationInstance
    private RetryManager retryManager = new RetryManagerTest()

    @Inject
    private DataPersistenceService dataPersistenceService

    @Inject
    @Modeled
    private EventSender<AlarmSupervisionResponse> alarmSupervisionResponseSender

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> fmMediationAlarmSyncRequestSender;

    @MockedImplementation
    private O1NodeSuspenderAgent o1NodeSuspenderAgent

    private final Map<String, Object> headers = new HashMap<>()
    private EventHandlerContext mockEventHandlerContext = Mock()

    def setup() {
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        createNormalizedMoTree()
    }

    @Unroll
    def "Test supervision on/off success scenario"(supervisionOn, hasSupervisionAttributes, autoSyncOn, initialState,
                                                   updatedState, alarmResponseCalls, autoSyncCalls) {
        given: "Prepare headers for event with mandatory attributes"
            setSupervisionHeaderProperty(supervisionOn, hasSupervisionAttributes)
            headers.put("fdn", FM_ALARM_SUPERVISION_FDN)

        and: "Initialize the handler with the headers"
            def inputEvent = new MediationComponentEvent(headers, null)
            fmSupervisionServiceStateHandler.init(mockEventHandlerContext)

        and: "Initialize FmFunction.currentServiceState"
            setCurrentServiceState(initialState)

        and: "Initialize FmAlarmSupervision.automaticSynchronization"
            setAutomaticSynchronization(autoSyncOn)

        when: "invoke the handler"
            fmSupervisionServiceStateHandler.onEventWithResult(inputEvent)

        then: "validate currentServiceSate value and expected events are sent"
            updatedState == getCurrentServiceState()
            alarmResponseCalls * alarmSupervisionResponseSender.send(_)
            autoSyncCalls * fmMediationAlarmSyncRequestSender.send(_)

        where: ""
            supervisionOn | hasSupervisionAttributes | autoSyncOn | initialState | updatedState | alarmResponseCalls | autoSyncCalls
            true          | true                     | true       | null         | "IN_SERVICE" | 1                  | 1
            true          | true                     | true       | "IDLE"       | "IN_SERVICE" | 1                  | 1
            true          | true                     | true       | "IN_SERVICE" | "IN_SERVICE" | 0                  | 0
            false         | true                     | true       | "IN_SERVICE" | "IDLE"       | 1                  | 0
            false         | true                     | true       | "IDLE"       | "IDLE"       | 0                  | 0
            true          | true                     | false      | null         | "IN_SERVICE" | 1                  | 0
            true          | true                     | false      | "IDLE"       | "IN_SERVICE" | 1                  | 0
            true          | false                    | true       | null         | "IN_SERVICE" | 1                  | 1
            true          | false                    | true       | "IDLE"       | "IN_SERVICE" | 1                  | 1
            true          | false                    | true       | "IN_SERVICE" | "IN_SERVICE" | 0                  | 0
            false         | false                    | true       | "IN_SERVICE" | "IDLE"       | 1                  | 0
            false         | false                    | true       | "IDLE"       | "IDLE"       | 0                  | 0
            true          | false                    | false      | null         | "IN_SERVICE" | 1                  | 0
            true          | false                    | false      | "IDLE"       | "IN_SERVICE" | 1                  | 0
    }

    def "Test supervision on scenario"() {
        given: "Prepare headers for event with mandatory attributes"
            headers.put("supervisionAttributes", ["active": true])
            headers.put("fdn", FM_ALARM_SUPERVISION_FDN)

        and: "Initialize the handler with the headers"
            def inputEvent = new MediationComponentEvent(headers, null)
            fmSupervisionServiceStateHandler.init(mockEventHandlerContext)

        and: "Initialize FmFunction.currentServiceState"
            setCurrentServiceState("IDLE")

        and: "Initialize FmAlarmSupervision.automaticSynchronization"
            setAutomaticSynchronization(true)

        when: "invoke the handler"
            fmSupervisionServiceStateHandler.onEventWithResult(inputEvent)

        then: "The AlarmSupervisionResponse event is sent with the expected details"
            1 * alarmSupervisionResponseSender.send({ it.toString() == getExpectedAlarmSupervisionResponse(true, "SUCCESS").toString() })

        and: "The FmMediationAlarmSyncRequest event is sent as a MTR for the node"
            1 * fmMediationAlarmSyncRequestSender.send({ it.toString().contains("MediationTaskRequest [nodeAddress=NetworkElement=NodeA,FmAlarmSupervision=1") })

        and: "The alarm service is called to add the node to nodeSuspenderCache"
            1 * o1NodeSuspenderAgent.register("TestNode")
    }

    def "Test supervision off scenario"() {
        given: "Prepare headers for event with mandatory attributes"
            headers.put("supervisionAttributes", ["active": false])
            headers.put("fdn", FM_ALARM_SUPERVISION_FDN)

        and: "Initialize the handler with the headers"
            def inputEvent = new MediationComponentEvent(headers, null)
            fmSupervisionServiceStateHandler.init(mockEventHandlerContext)

        and: "Initialize FmFunction.currentServiceState"
            setCurrentServiceState("IN_SERVICE")

        and: "Initialize FmAlarmSupervision.automaticSynchronization"
            setAutomaticSynchronization(true)

        when: "invoke the handler"
            fmSupervisionServiceStateHandler.onEventWithResult(inputEvent)

        then: "The AlarmSupervisionResponse event is sent with the expected details"
            1 * alarmSupervisionResponseSender.send({ it.toString() == getExpectedAlarmSupervisionResponse(false, "SUCCESS").toString() })

        and: "The FmMediationAlarmSyncRequest event is not sent"
            0 * fmMediationAlarmSyncRequestSender.send(_)

        and: "The node is call to remove the node from nodeSuspenderCache"
            1 * o1NodeSuspenderAgent.unregister("TestNode")
    }

    def "Test handler with missing 'fdn' header"() {
        given: "Prepare headers for event with missing fdn attribute"
            headers.put("supervisionAttributes", ["active": true])

        and: "Initialize the handler with the headers"
            def inputEvent = new MediationComponentEvent(headers, null)
            fmSupervisionServiceStateHandler.init(mockEventHandlerContext)

        when: "invoke the handler"
            fmSupervisionServiceStateHandler.onEventWithResult(inputEvent)

        then: "an exception with an appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains("Supervision Request Failed : FmAlarmSupervision FDN not available")
    }

    def "Test handler with missing 'active' header"() {
        given: "Handler is initialized with empty headers map"
            def inputEvent = new MediationComponentEvent(headers, null)
            fmSupervisionServiceStateHandler.init(mockEventHandlerContext)

        when: "invoke the handler"
            fmSupervisionServiceStateHandler.onEventWithResult(inputEvent)

        then: "an exception with an appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains("Supervision Request Failed : supervision status not available")
    }

    def createNormalizedMoTree() {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn("MeContext=TestNode").build();

        configurableDps.addManagedObject().
                withFdn(NETWORK_ELEMENT_FDN).
                withAssociation("nodeRootRef", meContext).build()

        configurableDps.addManagedObject()
                .withFdn(FM_FUNCTION_FDN)
                .addAttributes([currentServiceState: "IDLE"]).build()

        configurableDps.addManagedObject()
                .withFdn(FM_ALARM_SUPERVISION_FDN)
                .addAttributes([automaticSynchronization: true]).build()
    }

    def setSupervisionHeaderProperty(boolean supervisionOn, boolean hasSupervisionAttributes) {
        if (hasSupervisionAttributes) {
            headers.put("supervisionAttributes", ["active": supervisionOn])
        } else {
            headers.put("active", supervisionOn)
        }
    }

    def getCurrentServiceState() {
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(FM_FUNCTION_FDN)
        return mo.getAttribute("currentServiceState")
    }

    def setCurrentServiceState(value) {
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(FM_FUNCTION_FDN)
        mo.setAttribute("currentServiceState", value)
    }

    def setAutomaticSynchronization(value) {
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(FM_ALARM_SUPERVISION_FDN)
        mo.setAttribute("automaticSynchronization", value)
    }

    def getExpectedAlarmSupervisionResponse(final boolean isSupervisionActive, final String result) {
        final AlarmSupervisionResponse event = new AlarmSupervisionResponse()
        event.setNodeFdn(NETWORK_ELEMENT_FDN)
        event.setActive(isSupervisionActive)
        event.setResult(result)

        return event
    }
}

