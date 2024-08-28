
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;
import com.ericsson.oss.mediation.fm.o1.flows.Matchers.EventNotificationTypesMatcher;
import com.ericsson.oss.mediation.fm.o1.handlers.util.MediationTaskRequestUtil;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;

@FlowRef(flowName = "FmAlarmSyncFlow", version = "1.0.0", namespace = "O1_MED")
public class FmAlarmSyncFlowSuccessTest extends FmAlarmSyncFlowBaseTest {
    @Test
    public void test_execute_flow_success() throws Exception {
        // GIVEN: the O1Node exists and supervision is on.
        dpsNodeCreator.createDpsMos(NODE_NAME);

        // AND: valid alarmRecords are returned in netconf result
        when(netconfManagerOperation.executeXAResourceOperation(any(NetconfOperationRequest.class)))
                .thenReturn(new YangNetconfOperationResult(okResponseWithAlarmRecords(validAlarmRecords()),
                        YangNetconfOperationResult.YangNetconfOperationResultCode.OK));

        // WHEN: the flow is invoked.
        final MediationTaskRequest request = MediationTaskRequestUtil.createFmMediationAlarmSyncRequest(NETWORK_ELEMENT_FDN);
        invokeFlow(request);

        // THEN: each handler performs it's expected operations.
        verifyReadManagedElementIdHandler();
        verifyReadAlarmListHandler();
        verifyAlarmListTransformerHandler();
        verifyEventNotificationDispatchHandler();

        verify(netconfManagerOperation, times(1)).disconnect();

        // AND: the event sender is invoked with alarms with the expected alarm record types
        verify(eventNotificationBatchEventSender)
                .send(argThat(new EventNotificationTypesMatcher("SYNCHRONIZATION_STARTED", "SYNCHRONIZATION_ALARM", "SYNCHRONIZATION_ENDED")));
        verify(eventNotificationBatchEventSender, never())
                .send(argThat(new EventNotificationTypesMatcher("SYNCHRONIZATION_ABORTED")));
    }

    private void verifyReadManagedElementIdHandler() {
        verify(spy_readManagedElementIdHandler, times(1)).onEventWithResult(any());
    }

    private void verifyReadAlarmListHandler() throws NetconfManagerException {
        verify(spy_readAlarmListHandler, times(1)).onEventWithResult(any());
        assertYangOperations(SimpleNetconfOperation.builder()
                .fdn(ALARM_LIST_FDN)
                .attribute("attributes")
                .attribute("alarmRecords")
                .build());
    }

    private void verifyAlarmListTransformerHandler() {
        verify(spy_alarmListTransformerHandler, times(1)).onEventWithResult(any());
    }

    private void verifyEventNotificationDispatchHandler() {
        verify(spy_eventNotificationDispatchHandler, times(1)).onEventWithResult(any());
    }
}
