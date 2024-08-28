
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.itpf.sdk.recording.ErrorSeverity.ERROR;

import org.junit.Test;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult;
import com.ericsson.oss.mediation.engine.flow.FlowProcessingException;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;
import com.ericsson.oss.mediation.fm.o1.flows.Matchers.EventNotificationTypesMatcher;
import com.ericsson.oss.mediation.fm.o1.handlers.util.MediationTaskRequestUtil;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;

@FlowRef(flowName = "FmAlarmSyncFlow", version = "1.0.0", namespace = "O1_MED")
public class FmAlarmSyncFlowFailTest extends FmAlarmSyncFlowBaseTest {

    @Test(expected = FlowProcessingException.class)
    public void test_when_netconf_operation_fails_in_ReadAlarmListHandler() throws Exception {
        try {
            // GIVEN: the O1Node exists and supervision is on.
            dpsNodeCreator.createDpsMos(NODE_NAME);

            // AND: the netconf operation result is null
            when(netconfManagerOperation.executeXAResourceOperation(any())).thenReturn(null);

            // WHEN: the flow is invoked.
            final MediationTaskRequest request = MediationTaskRequestUtil.createFmMediationAlarmSyncRequest(NETWORK_ELEMENT_FDN);
            invokeFlow(request);

        } finally {
            verify(netconfManagerOperation, times(1)).disconnect();
            // THEN: the event sender is invoked with alarm with the expected alarm record type
            verify(eventNotificationBatchEventSender)
                    .send(argThat(new EventNotificationTypesMatcher("SYNCHRONIZATION_ABORTED")));
        }
    }

    @Test
    public void test_when_transforming_alarm_fails() throws Exception {
        // GIVEN: the O1Node exists and supervision is on.
        dpsNodeCreator.createDpsMos(NODE_NAME);

        // AND: invalid alarmRecords are returned in netconf result
        when(netconfManagerOperation.executeXAResourceOperation(any(NetconfOperationRequest.class)))
                .thenReturn(new YangNetconfOperationResult(okResponseWithAlarmRecords(invalidAlarmRecords()),
                        YangNetconfOperationResult.YangNetconfOperationResultCode.OK));

        // WHEN: the flow is invoked.
        final MediationTaskRequest request = MediationTaskRequestUtil.createFmMediationAlarmSyncRequest(NETWORK_ELEMENT_FDN);
        invokeFlow(request);

        verify(netconfManagerOperation, times(1)).disconnect();

        // THEN: the event sender is invoked with alarms with the expected alarm record types
        verify(eventNotificationBatchEventSender)
                .send(argThat(new EventNotificationTypesMatcher("SYNCHRONIZATION_STARTED", "SYNCHRONIZATION_ENDED")));
        verify(eventNotificationBatchEventSender, never())
                .send(argThat(new EventNotificationTypesMatcher("SYNCHRONIZATION_ABORTED")));

        // AND: an error is recorded with system recorder
        verify(systemRecorder)
                .recordError(
                        "Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: Invalid input value for date: [3] Date should in OpenAPI (rfc3339) format]",
                        ErrorSeverity.WARNING, "ModelTransformer", "",
                        "For the alarm with the following ID: 1");
    }

    @Test
    public void test_when_sending_alarm_fails() throws Exception {
        // GIVEN: the O1Node exists and supervision is on.
        dpsNodeCreator.createDpsMos(NODE_NAME);

        // AND: valid alarmRecords are returned in netconf result
        when(netconfManagerOperation.executeXAResourceOperation(any(NetconfOperationRequest.class)))
                .thenReturn(new YangNetconfOperationResult(okResponseWithAlarmRecords(validAlarmRecords()),
                        YangNetconfOperationResult.YangNetconfOperationResultCode.OK));

        // AND: exception is thrown when event notifications are sent
        doThrow(new UnsupportedOperationException()).when(eventNotificationBatchEventSender).send((EventNotificationBatch) any());

        // WHEN: the flow is invoked.
        final MediationTaskRequest request = MediationTaskRequestUtil.createFmMediationAlarmSyncRequest(NETWORK_ELEMENT_FDN);
        invokeFlow(request);

        verify(netconfManagerOperation, times(1)).disconnect();

        // THEN: the event sender is invoked with alarms with the expected alarm record types
        verify(eventNotificationBatchEventSender)
                .send(argThat(new EventNotificationTypesMatcher("SYNCHRONIZATION_STARTED", "SYNCHRONIZATION_ALARM", "SYNCHRONIZATION_ENDED")));
        verify(eventNotificationBatchEventSender, never())
                .send(argThat(new EventNotificationTypesMatcher("SYNCHRONIZATION_ABORTED")));

        // AND: an error is recorded with system recorder
        verify(systemRecorder)
                .recordError("O1NODE_FM_NOTIFICATION.SENDING_ALARM_ERROR", ERROR,
                        "com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler",
                        "MeContext=O1Node_Test", " Failed to send alarm(s) to FM service.");
    }
}
