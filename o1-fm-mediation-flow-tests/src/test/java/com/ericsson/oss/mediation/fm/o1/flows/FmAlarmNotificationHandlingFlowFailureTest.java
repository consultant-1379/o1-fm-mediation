
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.HREF;
import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.SYSTEM_DN;
import static com.ericsson.oss.mediation.fm.o1.flows.O1JmsFlowBase.FM_NOTIFICATIONS_QUEUE_NAME;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.Attributes.NOTIFICATION_TYPE;
import static com.ericsson.oss.mediation.fm.o1.nodesuspender.O1NodeSuspenderCacheManager.NODESUSPENDER_CACHE_NAME;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.cache.Cache;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache;
import com.ericsson.cds.cdi.support.rule.ImplementationClasses;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.SpyImplementation;
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.engine.camel.test.Destination;
import com.ericsson.oss.mediation.engine.camel.test.JmsTestCase;
import com.ericsson.oss.mediation.engine.camel.test.TestMessage;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;
import com.ericsson.oss.mediation.fm.o1.handlers.AlarmTransformerHandler;
import com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler;
import com.ericsson.oss.mediation.fm.o1.handlers.IsNodeSuspendedHandler;
import com.ericsson.oss.mediation.fm.o1.nodesuspender.NodeSuspensionEntry;
import com.ericsson.oss.mediation.fm.o1.nodesuspender.O1NodeSuspenderAgent;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.google.common.collect.ImmutableMap;

@FlowRef(flowName = "FmAlarmNotificationHandlingFlow", version = "1.0.0", namespace = "O1_MED")
@JmsTestCase(destinations = { @Destination(name = FM_NOTIFICATIONS_QUEUE_NAME) })
public class FmAlarmNotificationHandlingFlowFailureTest extends O1JmsFlowBase {

    final static String NETWORK_ELEMENT_FDN = "NetworkElement=O1Node_Test";
    final static String ALARM_ID = "alarmId";

    @MockedImplementation
    CacheProviderBean cacheProviderBean;

    @ImplementationClasses
    List<Class> classes = new ArrayList<>(Arrays.asList(O1NodeSuspenderAgent.class));

    Cache<String, NodeSuspensionEntry> o1NodeSuspenderCache = new InMemoryCache(NODESUSPENDER_CACHE_NAME);

    @SpyImplementation
    IsNodeSuspendedHandler isNodeSuspendedHandler;

    @SpyImplementation
    AlarmTransformerHandler alarmTransformerHandler;

    @SpyImplementation
    EventNotificationDispatchHandler eventNotificationDispatchHandler;

    @MockedImplementation
    EventSender<EventNotificationBatch> eventNotificationBatchEventSender;

    @MockedImplementation
    SystemRecorder systemRecorder;

    @Before
    public void setupTest() {
        when(cacheProviderBean.createOrGetModeledCache(eq(NODESUSPENDER_CACHE_NAME))).thenReturn((Cache) o1NodeSuspenderCache);
    }

    @Test
    @TestMessage(destinations = { @Destination(name = FM_NOTIFICATIONS_QUEUE_NAME) })
    public void test_when_updating_node_suspender_cache_fails() throws Exception {
        // GIVEN: Node suspender cache is empty
        assertNull(o1NodeSuspenderCache.get(NETWORK_ELEMENT_FDN));

        // WHEN: an unsupported notification is sent
        sendJmsMessage(ImmutableMap.of(
                NOTIFICATION_TYPE, "notifyMOICreation",
                SYSTEM_DN, "ManagedElement=1,MnsAgent=FM",
                ALARM_ID, "1"));

        assertHandlerExecuted(isNodeSuspendedHandler);

        // THEN: Node suspender cache is not updated
        assertNull(o1NodeSuspenderCache.get(NETWORK_ELEMENT_FDN));

    }

    @Test
    @TestMessage(destinations = { @Destination(name = FM_NOTIFICATIONS_QUEUE_NAME) })
    public void test_when_transforming_alarm_fails() throws Exception {
        // WHEN: an invalid notification is sent
        sendJmsMessage(ImmutableMap.of(
                NOTIFICATION_TYPE, "notifyNewAlarm",
                SYSTEM_DN, "ManagedElement=1,MnsAgent=FM", ALARM_ID, "1"));

        // THEN: Handlers are executed
        assertHandlerExecuted(isNodeSuspendedHandler);
        assertHandlerExecuted(alarmTransformerHandler);

        // AND: an error is recorded with system recorder
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(systemRecorder)
                .recordError(
                        "Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: Alarm is missing mandatory 'href' field.]",
                        ErrorSeverity.WARNING, "ModelTransformer",
                        "", "For alarm with the following ID: 1"));
    }

    @Test
    @TestMessage(destinations = { @Destination(name = FM_NOTIFICATIONS_QUEUE_NAME) })
    public void test_when_sending_alarm_fails() throws Exception {
        // GIVEN: exception is thrown when event notification is sent
        doThrow(new UnsupportedOperationException()).when(eventNotificationBatchEventSender).send((EventNotificationBatch) any());

        // WHEN: a valid notification is sent
        sendJmsMessage(ImmutableMap.of(
                NOTIFICATION_TYPE, "notifyNewAlarm",
                HREF, "https://O1Node_Test.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1",
                SYSTEM_DN, "ManagedElement=1,MnsAgent=FM", ALARM_ID, "1"));

        // THEN: Handlers are executed
        assertHandlerExecuted(isNodeSuspendedHandler);
        assertHandlerExecuted(alarmTransformerHandler);
        assertHandlerExecuted(eventNotificationDispatchHandler);

        // AND: an error is recorded with system recorder
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(systemRecorder)
                .recordError("O1NODE_FM_NOTIFICATION.SENDING_ALARM_ERROR", ErrorSeverity.ERROR,
                        "com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler",
                        "MeContext=O1Node_Test,ManagedElement=NodeA,GNBCUCPFunction=1", " Failed to send alarm(s) to FM service."));
    }
}
