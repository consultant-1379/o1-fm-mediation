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

package com.ericsson.oss.mediation.fm.o1.flows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static com.ericsson.oss.mediation.fm.o1.flows.SupervisionFlowTest.deleteNtfSubscriptionControl;
import static com.ericsson.oss.mediation.fm.o1.flows.SupervisionFlowTest.setDnPrefix;
import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.CREATE;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.MERGE;

import java.time.Duration;
import java.util.Arrays;

import org.junit.Test;

import com.ericsson.oss.mediation.engine.test.flow.FlowRef;

@FlowRef(flowName = "FmSupervisionFlow", version = "1.0.0", namespace = "O1_MED")
public class FmSupervisionFlowTest extends FmSupervisionFlowBase {

    @Test
    public void test_activate_success() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_TRUE, "IDLE", true);
        mockHeartbeatControlMoExists(false);
        // WHEN
        invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_TRUE));

        // THEN changes are mediated
        assertYangOperations(setDnPrefix(), unlockAlarmList(), createNtfSubscriptionControl(), createHeartbeatControl());

        // AND events are sent
        verifyAlarmSupervisionResponseSent("SUCCESS");
        verifyFmMediationAlarmSyncRequestSent();
        // AND caches are updated
        assertTrue(o1NodeSuspenderAgent.isRegistered("O1Node_Test"));
        assertTrue(o1HeartbeatAgent.isRegistered(FM, "NetworkElement=O1Node_Test"));
        // AND service state is IN_SERVICE
        assertEquals("IN_SERVICE", dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1").getAttribute("currentServiceState"));
    }

    @Test
    public void test_activate_already_active_success() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_TRUE, "IN_SERVICE", true);
        mockHeartbeatControlMoExists(true);
        // WHEN
        invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_TRUE));
        // THEN
        // Behaviour will change with TORF-710433
        assertYangOperations();
        verifyAlarmSupervisionResponseNotSent();
        verifyFmMediationAlarmSyncRequestNotSent();
        assertEquals("IN_SERVICE", dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1").getAttribute("currentServiceState"));
    }

    @Test
    public void test_deactivate_success() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_FALSE, "IN_SERVICE", true);
        o1NodeSuspenderAgent.register("O1Node_Test");
        o1HeartbeatAgent.register(FM, "NetworkElement=O1Node_Test", Duration.ofSeconds(10));
        mockHeartbeatControlMoExists(true);
        // WHEN
        invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_FALSE));
        // THEN
        assertYangOperations(deleteNtfSubscriptionControl());
        verifyAlarmSupervisionResponseSent("SUCCESS");
        assertFalse(o1NodeSuspenderAgent.isRegistered("O1Node_Test"));
        assertFalse(o1HeartbeatAgent.isRegistered(FM, "NetworkElement=O1Node_Test"));
        assertEquals("IDLE", dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1").getAttribute("currentServiceState"));
    }

    @Test
    public void test_deactivate_already_deactivated_success() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_FALSE, "IDLE", true);
        mockHeartbeatControlMoExists(false);
        // WHEN
        invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_FALSE));
        // THEN
        // Behaviour will change with TORF-710551
        assertYangOperations();
        verifyAlarmSupervisionResponseNotSent();
        verifyFmMediationAlarmSyncRequestNotSent();
        assertEquals("IDLE", dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1").getAttribute("currentServiceState"));
    }

    @Test
    public void test_execute_flow_success_automatic_sync_false() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_TRUE, "IDLE", false);
        mockHeartbeatControlMoExists(false);
        // WHEN
        invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_TRUE));
        // THEN
        verifyFmMediationAlarmSyncRequestNotSent();
        // AND service state is IN_SERVICE
        assertEquals("IN_SERVICE", dps.getLiveBucket().findMoByFdn("NetworkElement=O1Node_Test,FmFunction=1").getAttribute("currentServiceState"));
    }

    protected static SimpleNetconfOperation createNtfSubscriptionControl() {
        return SimpleNetconfOperation.builder().operation(CREATE)
                .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1,NtfSubscriptionControl=ENMFM")
                .attribute("attributes")
                .attribute("notificationRecipientAddress", "http://1.2.3.4:8099/FM/eventListener/v1")
                .attribute("notificationTypes", Arrays.asList(
                        "notifyChangedAlarm", "notifyNewAlarm", "notifyChangedAlarmGeneral",
                        "notifyClearedAlarm", "notifyAlarmListRebuilt"))
                .build();
    }

    protected static SimpleNetconfOperation unlockAlarmList() {
        return SimpleNetconfOperation.builder().operation(MERGE)
                .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1,AlarmList=1")
                .attribute("attributes")
                .attribute("administrativeState", "UNLOCKED").build();
    }

    protected static SimpleNetconfOperation createHeartbeatControl() {
        return SimpleNetconfOperation.builder().operation(CREATE)
                .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1,NtfSubscriptionControl=ENMFM,HeartbeatControl=1")
                .attribute("attributes")
                .attribute("heartbeatNtfPeriod", "100").build();
    }
}
