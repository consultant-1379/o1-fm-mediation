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
package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatusHelper
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException
import org.apache.commons.lang3.StringUtils

import javax.inject.Inject

import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_ABORTED
import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_FAILED
import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_SUCCESS
import static com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus.CONNECTED

class CheckSupervisionMosHandlerSpec extends CdiSpecification {

    private final Map<String, Object> headers = new HashMap<>()

    private EventHandlerContext mockEventHandlerContext = Mock()

    @ObjectUnderTest
    private CheckSupervisionMosHandler checkSupervisionMosHandler

    @Inject
    private NetconfManager mockNetconfManager

    @Inject
    private RetryManager mockRetryManager

    def setup() {
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        mockNetconfManager.getStatus() >>> [CONNECTED]
    }

    def "Test when previous O1 handler is failed then execution is aborted"() {
        given: "An event based client request"
        prepareO1HandlerFailed()
        def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
        checkSupervisionMosHandler.init(mockEventHandlerContext)

        when: "handler is invoked"
        def outputEvent = checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "it does not execute the handler logic"
        0 * mockNetconfManager._

        and: "handler operation is aborted"
        verifyOperation(outputEvent.getHeaders(), OPERATION_ABORTED, null)
    }

    def "Test when supervision enabled and heartbeat control MO already exists then handler result is failed (idempotency case)"() {
        given: "A request with active true and session created successfully"
        prepareO1HandlersSuccess()
        headers.put("active", true)
        headers.put('netconfManager', mockNetconfManager)
        def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
        checkSupervisionMosHandler.init(mockEventHandlerContext)

        and: "the node response is mocked to indicate the HeartbeatControl MO exists"
        mockRetryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> getHeartbeatControlMoExistsResponse()

        when: "handler is invoked"
        def outputEvent = checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "handler operation is set to failed as is valid idempotency case"
        verifyOperation(outputEvent.getHeaders(), OPERATION_FAILED, null)

        and: "flag to suppress terminating flow is set"
        assert O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(outputEvent.getHeaders()).suppressTerminateFlow == true
    }

    def "Test when supervision enabled and heartbeat control MO does not exist then handler result is success"() {
        given: "A request with active true and session created successfully"
        prepareO1HandlersSuccess()
        headers.put("active", true)
        headers.put('netconfManager', mockNetconfManager)
        def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
        checkSupervisionMosHandler.init(mockEventHandlerContext)

        and: "the node response is mocked to indicate the HeartbeatControl MO does not exist"
        mockRetryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> getHeartbeatControlMoDoesNotExistResponse()

        when: "handler is invoked"
        def outputEvent = checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "handler operation is set to success"
        verifyOperation(outputEvent.getHeaders(), OPERATION_SUCCESS, null)
    }

    def "Test when supervision disabled and heartbeat control MO already exists then handler result is success"() {
        given: "A request with active false and session created successfully"
        prepareO1HandlersSuccess()
        headers.put("active", false)
        headers.put('netconfManager', mockNetconfManager)
        def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
        checkSupervisionMosHandler.init(mockEventHandlerContext)

        and: "the node response is mocked to indicate the HeartbeatControl MO exists"
        mockRetryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> getHeartbeatControlMoExistsResponse()

        when: "handler is invoked"
        def outputEvent = checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "handler operation is set to success"
        verifyOperation(outputEvent.getHeaders(), OPERATION_SUCCESS, null)

    }

    def "Test when supervision disabled and and heartbeat control MO does not exist then handler result is failed (idempotency case)"() {
        given: "A request with active true and session created successfully"
        prepareO1HandlersSuccess()
        headers.put("active", false)
        headers.put('netconfManager', mockNetconfManager)
        def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
        checkSupervisionMosHandler.init(mockEventHandlerContext)

        and: "the node response is mocked to indicate the HeartbeatControl MO does not exist"
        mockRetryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> getHeartbeatControlMoDoesNotExistResponse()

        when: "handler is invoked"
        def outputEvent = checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "handler operation is set to failed as its valid idempotency case"
        verifyOperation(outputEvent.getHeaders(), OPERATION_FAILED, null)

        and: "flag to suppress terminating flow is set"
        assert O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(outputEvent.getHeaders()).suppressTerminateFlow == true
    }

    def "Test when netconf get operation throws an exception then result is operation failed"() {
        given: "A request with active true and session created successfully"
        prepareO1HandlersSuccess()
        headers.put("active", true)
        headers.put('netconfManager', mockNetconfManager)
        def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
        checkSupervisionMosHandler.init(mockEventHandlerContext)

        and: "the node response is mocked to throw an exception"
        mockRetryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> [{ throw new NetconfManagerException("ah crap") }, getOkResponse()]

        when: "handler is invoked"
        def outputEvent = checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "handler operation is set to operation failed"
        verifyOperation(outputEvent.getHeaders(), OPERATION_FAILED,
                "Failed to read MO from node using filter XPathFilter{filter='/ManagedElement/NtfSubscriptionControl/HeartbeatControl'}")
    }


    def prepareO1HandlersSuccess() {
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(UnlockAlarmListHandler.class, OPERATION_SUCCESS, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(UnlockAlarmListHandler.class, OPERATION_SUCCESS, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(UnlockAlarmListHandler.class, OPERATION_SUCCESS, null), headers);
    }

    def prepareO1HandlerFailed() {
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(UnlockAlarmListHandler.class, OPERATION_SUCCESS, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(UnlockAlarmListHandler.class, OPERATION_FAILED, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(UnlockAlarmListHandler.class, OPERATION_ABORTED, null), headers);
    }

    def createOperation(final Class handler, final O1NetconfOperationResult result, final Exception e) {
        final O1NetconfOperationStatus operationStatus = new O1NetconfOperationStatus(handler, "NetworkElement=Test")
        operationStatus.setResult(result)
        operationStatus.setException(e)
        return operationStatus
    }

    NetconfResponse getHeartbeatControlMoExistsResponse() {
        def response = "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n" +
                "<data>\n" +
                "    <ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "        <id>ocp83vcu03o1</id>\n" +
                "        <NtfSubscriptionControl>\n" +
                "            <id>ENMFM</id>\n" +
                "            <HeartbeatControl>\n" +
                "                <id>1</id>\n" +
                "                <attributes>\n" +
                "                    <heartbeatNtfPeriod>100</heartbeatNtfPeriod>\n" +
                "                </attributes>\n" +
                "            </HeartbeatControl>\n" +
                "        </NtfSubscriptionControl>\n" +
                "    </ManagedElement>\n" +
                "</data>\n" +
                "</rpc-reply>"

        def netconfResponse = new NetconfResponse()
        netconfResponse.setData(response)

        return netconfResponse
    }

    NetconfResponse getHeartbeatControlMoDoesNotExistResponse() {
        def response = "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n" +
                "<rpc-error>\n" +
                "    <error-type>rpc</error-type>\n" +
                "    <error-tag>bad-element</error-tag>\n" +
                "    <error-severity>error</error-severity>\n" +
                "    <error-info>\n" +
                "        <bad-element>filter</bad-element>\n" +
                "    </error-info>\n" +
                "</rpc-error>\n" +
                "</rpc-reply>"

        def netconfResponse = new NetconfResponse()
        netconfResponse.setError(true)
        netconfResponse.setData(response)

        return netconfResponse
    }

    String getOkResponse() {
        return "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1001\">  \n" +
                "  <ok/>  \n" +
                "</rpc-reply>"
    }

    private void verifyOperation(final Map<String, Object> headers, final O1NetconfOperationResult result, final String exceptionMessage) {
        final O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(headers)
        final O1NetconfOperationStatus operationStatus = operationsStatus.getOperation(CheckSupervisionMosHandler.class)
        assert operationStatus.getResult() == result
        if (!StringUtils.isBlank(exceptionMessage)) {
            assert operationStatus.getException().getMessage() == exceptionMessage
        }
    }
}
