
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.ericsson.oss.itpf.common.event.handler.ResultEventInputHandler;
import com.ericsson.oss.mediation.engine.camel.resource.JmsUtil;
import com.ericsson.oss.mediation.engine.camel.test.JmsRule;
import com.ericsson.oss.mediation.engine.camel.test.JmsTestCase;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class O1JmsFlowBase extends O1BaseFlowTest {

    @ClassRule
    public static JmsRule jmsRule = new JmsRule();

    protected static final String FM_NOTIFICATIONS_QUEUE_NAME = "O1FmAlarmNotifications";
    protected static final String FM_HEARTBEAT_NOTIFICATIONS_QUEUE_NAME = "O1HeartbeatNotifications";
    private static final String TARGET_INSTANCE = "flow_tests";
    private static Set<String> initializedFlows = new HashSet<>();

    @BeforeClass
    public static void setupProperties() {
        System.setProperty("o1_notifications_channelId", FM_NOTIFICATIONS_QUEUE_NAME);
        System.setProperty("o1_heartbeat_notifications_channelId", FM_HEARTBEAT_NOTIFICATIONS_QUEUE_NAME);
        System.setProperty("com.ericsson.oss.sdk.node.identifier", TARGET_INSTANCE);
    }

    @AfterClass
    public static void stopJms() throws Exception {
        JmsRule.getEmbeddedJms().stop();
    }

    public void sendJmsMessage(Serializable message) throws JMSException {
        initializeJmsFlow();
        Connection connection = JmsUtil.getConnectionFactory().createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        String destination = getClass().getAnnotation(JmsTestCase.class).destinations()[0].name();
        MessageProducer producer = session.createProducer((javax.jms.Destination) JmsRule.getEmbeddedJms().getRegistry().lookup(destination));

        Message objMessage = session.createObjectMessage(message);
        objMessage.setStringProperty("__target_ms_instance", TARGET_INSTANCE);
        producer.send(objMessage);

        connection.close();
        session.close();
    }

    private void initializeJmsFlow() {
        String flowName = getClass().getAnnotation(FlowRef.class).flowName();
        if (initializedFlows.add(flowName)) {
            // Every JMS based flow needs to be initialized before sending messages to JMS by invoking it once
            dpsNodeCreator.configurableDps.addManagedObject().withFdn("A=a").build();
            MediationTaskRequest mtr = new MediationTaskRequest();
            mtr.setNodeAddress("A=a");
            invokeFlow(mtr);
            log.info("Initialized JMS based flow [{}]", flowName);
        }
    }

    protected void assertHandlerExecuted(ResultEventInputHandler handler) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> verify(handler).onEventWithResult(any()));
    }
}
