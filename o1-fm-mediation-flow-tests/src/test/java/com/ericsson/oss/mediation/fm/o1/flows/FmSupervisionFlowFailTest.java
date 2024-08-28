
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.mediation.fm.o1.flows.SupervisionFlowTest.deleteNtfSubscriptionControl;
import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM;
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgentImpl.REGISTRATION_CACHE_NAME;

import java.time.Duration;

import javax.cache.Cache;

import org.junit.Test;

import com.ericsson.oss.itpf.datalayer.dps.DeleteOptions;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.engine.flow.FlowProcessingException;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;

@FlowRef(flowName = "FmSupervisionFlow", version = "1.0.0", namespace = "O1_MED")
public class FmSupervisionFlowFailTest extends FmSupervisionFlowBase {

    @Test(expected = FlowProcessingException.class)
    public void test_enable_supervision_flow_netconf_error() throws Exception {
        try {
            // GIVEN
            dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_TRUE, "IDLE", true);
            mockHeartbeatControlMoExists(false);
            // WHEN
            when(netconfManagerOperation.executeXAResourceOperation(any(NetconfOperationRequest.class))).thenReturn(null);
            // AND
            invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_TRUE));
        } finally {
            // THEN events are sent
            verifyAlarmSupervisionResponseSent("FAILURE");
            verifyFmMediationAlarmSyncRequestNotSent();
            // AND caches are NOT updated
            assertFalse(o1NodeSuspenderAgent.isRegistered("O1Node_Test"));
            assertFalse(o1HeartbeatAgent.isRegistered(FM, "NetworkElement=O1Node_Test"));
            // AND service state remains IDLE
            assertEquals("IDLE", dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1").getAttribute("currentServiceState"));
        }
    }

    @Test(expected = FlowProcessingException.class)
    public void test_disable_supervision_flow_netconf_error() throws Exception {
        try {
            // GIVEN
            dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_FALSE, "IN_SERVICE", false);
            o1NodeSuspenderAgent.register("O1Node_Test");
            o1HeartbeatAgent.register(FM, "NetworkElement=O1Node_Test", Duration.ofSeconds(10));
            mockHeartbeatControlMoExists(true);
            // WHEN
            when(netconfManagerOperation.executeXAResourceOperation(any(NetconfOperationRequest.class))).thenReturn(null);
            invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_FALSE));
        } finally {
            // THEN
            assertYangOperations(deleteNtfSubscriptionControl());
            verifyAlarmSupervisionResponseSent("FAILURE");
            verifyFmMediationAlarmSyncRequestNotSent();
            assertTrue(o1NodeSuspenderAgent.isRegistered("O1Node_Test"));
            assertTrue(o1HeartbeatAgent.isRegistered(FM, "NetworkElement=O1Node_Test"));
            assertEquals("IN_SERVICE",
                    dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1").getAttribute("currentServiceState"));
        }
    }

    @Test(expected = FlowProcessingException.class)
    public void test_enable_supervision_flow_heartbeat_registration_fails_supervision_state_updated() throws Exception {
        try {
            // GIVEN
            dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_TRUE, "IDLE", true);
            mockHeartbeatControlMoExists(false);
            // WHEN
            Cache heartbeatRegistrationCache = mock(Cache.class);
            when(cacheProviderBean.createOrGetModeledCache(eq(REGISTRATION_CACHE_NAME))).thenReturn(heartbeatRegistrationCache);
            doThrow(UnsupportedOperationException.class).when(heartbeatRegistrationCache).put(any(Object.class), any(Object.class));
            invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_TRUE));
        } finally {
            // THEN
            // Behaviour will be refined with TORF-710433
            verifyAlarmSupervisionResponseSent("SUCCESS");
            assertTrue(o1NodeSuspenderAgent.isRegistered("O1Node_Test"));
            assertEquals("IN_SERVICE",
                    dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1").getAttribute("currentServiceState"));
        }
    }

    @Test(expected = FlowProcessingException.class)
    public void test_disable_supervision_flow_heartbeat_unregistration_fails_supervision_state_updated() {
        try {
            // GIVEN
            dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_FALSE, "IN_SERVICE", true);
            o1NodeSuspenderAgent.register("O1Node_Test");
            // WHEN
            Cache heartbeatRegistrationCache = mock(Cache.class);
            when(cacheProviderBean.createOrGetModeledCache(eq(REGISTRATION_CACHE_NAME))).thenReturn(heartbeatRegistrationCache);
            when(heartbeatRegistrationCache.containsKey(any())).thenThrow(UnsupportedOperationException.class);
            invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_FALSE));
        } finally {
            // THEN
            // Behaviour will be refined with TORF-710551
            verifyAlarmSupervisionResponseSent("SUCCESS");
            assertFalse(o1NodeSuspenderAgent.isRegistered("O1Node_Test"));
            assertEquals("IDLE",
                    dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1").getAttribute("currentServiceState"));
        }
    }

    @Test
    public void test_enable_supervision_flow_update_service_state_fails() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_TRUE, "IDLE", true);
        mockHeartbeatControlMoExists(false);
        ManagedObject fmFunction = dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1");
        dps.getLiveBucket().deleteManagedObject(fmFunction, DeleteOptions.defaultDelete());
        // WHEN
        invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_TRUE));

        // THEN
        verifyAlarmSupervisionResponseSent("FAILURE");
        verifyFmMediationAlarmSyncRequestNotSent();
        assertFalse(o1NodeSuspenderAgent.isRegistered("O1Node_Test"));
    }
}
