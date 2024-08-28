
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache;
import com.ericsson.cds.cdi.support.rule.ImplementationClasses;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.SpyImplementation;
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.engine.camel.test.Destination;
import com.ericsson.oss.mediation.engine.camel.test.JmsTestCase;
import com.ericsson.oss.mediation.engine.camel.test.TestMessage;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;
import com.ericsson.oss.mediation.fm.o1.handlers.AlarmTransformerHandler;
import com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler;
import com.ericsson.oss.mediation.fm.o1.handlers.IsNodeSuspendedHandler;
import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics;
import com.ericsson.oss.mediation.fm.o1.nodesuspender.NodeSuspensionEntry;
import com.ericsson.oss.mediation.fm.o1.nodesuspender.O1NodeSuspenderAgent;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.google.common.collect.ImmutableMap;

@FlowRef(flowName = "FmAlarmNotificationHandlingFlow", version = "1.0.0", namespace = "O1_MED")
@JmsTestCase(destinations = { @Destination(name = FM_NOTIFICATIONS_QUEUE_NAME) })
public class FmAlarmNotificationHandlingFlowSuccessTest extends O1JmsFlowBase {

    @MockedImplementation
    CacheProviderBean cacheProviderBean;

    @MockedImplementation
    EventSender<EventNotificationBatch> eventNotificationBatchEventSender;

    @Inject
    O1HandlerStatistics o1HandlerStatistics;

    @ImplementationClasses
    List<Class> classes = new ArrayList<>(Arrays.asList(O1NodeSuspenderAgent.class));

    Cache<String, NodeSuspensionEntry> o1NodeSuspenderCache = new InMemoryCache(NODESUSPENDER_CACHE_NAME);

    @SpyImplementation
    IsNodeSuspendedHandler isNodeSuspendedHandler;

    @SpyImplementation
    AlarmTransformerHandler alarmTransformerHandler;

    @SpyImplementation
    EventNotificationDispatchHandler eventNotificationDispatchHandler;

    @Before
    public void setupTest() {
        when(cacheProviderBean.createOrGetModeledCache(eq(NODESUSPENDER_CACHE_NAME))).thenReturn((Cache) o1NodeSuspenderCache);
    }

    @Test
    @TestMessage(destinations = { @Destination(name = FM_NOTIFICATIONS_QUEUE_NAME) })
    public void test_fm_notification_handling_positive_flow() throws Exception {
        // GIVEN
        assertEquals(o1HandlerStatistics.getTotalNoOfForwardedAlarmEventNotifications(), 0);

        // WHEN: a valid notification is sent
        sendJmsMessage(ImmutableMap.of(
                NOTIFICATION_TYPE, "notifyNewAlarm",
                HREF, "https://O1Node_Test.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1",
                SYSTEM_DN, "ManagedElement=1,MnsAgent=FM",
                "alarmId", "1"));

        // THEN: each handler performs it expected operation
        assertHandlerExecuted(isNodeSuspendedHandler);
        assertHandlerExecuted(alarmTransformerHandler);
        assertHandlerExecuted(eventNotificationDispatchHandler);

        // AND: event notification is sent to FM
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(eventNotificationBatchEventSender).send((EventNotificationBatch) any()));
        assertEquals(o1HandlerStatistics.getTotalNoOfForwardedAlarmEventNotifications(), 1);
    }
}
