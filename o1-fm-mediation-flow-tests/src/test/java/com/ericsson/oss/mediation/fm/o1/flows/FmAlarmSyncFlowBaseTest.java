
package com.ericsson.oss.mediation.fm.o1.flows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ericsson.cds.cdi.support.rule.ImplementationClasses;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.SpyImplementation;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.fm.o1.handlers.AlarmListTransformerHandler;
import com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler;
import com.ericsson.oss.mediation.fm.o1.transform.mapper.AlarmRecordsMapperImpl;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadManagedElementIdHandler;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

public abstract class FmAlarmSyncFlowBaseTest extends O1BaseFlowTest {

    final static String NODE_NAME = "O1Node_Test";
    final static String NETWORK_ELEMENT_FDN = "NetworkElement=O1Node_Test";
    final static String MECONTEXT_FDN = "MeContext=O1Node_Test";
    final static String MANAGED_ELEMENT_FDN = MECONTEXT_FDN + ",ManagedElement=ocp83vcu03o1";
    final static String ALARM_LIST_FDN = MANAGED_ELEMENT_FDN + ",AlarmList=1";

    @MockedImplementation
    EventSender<EventNotificationBatch> eventNotificationBatchEventSender;

    @MockedImplementation
    SystemRecorder systemRecorder;

    @SpyImplementation
    ReadManagedElementIdHandler spy_readManagedElementIdHandler;

    @SpyImplementation
    ReadAlarmListHandler spy_readAlarmListHandler;

    @SpyImplementation
    AlarmListTransformerHandler spy_alarmListTransformerHandler;

    @SpyImplementation
    EventNotificationDispatchHandler spy_eventNotificationDispatchHandler;

    @ImplementationClasses
    List<Class> classes = new ArrayList<>(Arrays.asList(AlarmRecordsMapperImpl.class));

    public NetconfResponse okResponseWithAlarmRecords(String alarmRecords) {
        String testString = "<ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">" +
                "<id>ocp83vcu03o1</id>" +
                "<AlarmList>" +
                "<id>1</id>" +
                "<attributes>" +
                "<operationalState>ENABLED</operationalState>" +
                "<administrativeState>UNLOCKED</administrativeState>" +
                "<triggerHeartbeatNtf>true</triggerHeartbeatNtf>" +
                "<lastModification>2023-03-31T14:45:03.545512+00:00</lastModification>" +
                "<numOfAlarmRecords>1</numOfAlarmRecords>" +
                "<alarmRecords>" +
                alarmRecords +
                "</alarmRecords>" +
                "</attributes>" +
                "</AlarmList>" +
                "</ManagedElement>";
        NetconfResponse response = new NetconfResponse();
        response.setData(testString);
        return response;
    }

    public String validAlarmRecords() {
        return "<additionalText>The trusted certificate has expired and should be renewed</additionalText>" +
                "<perceivedSeverity>CRITICAL</perceivedSeverity>" +
                "<specificProblem>Certificate Management, Trusted Certificate is about to Expire</specificProblem>" +
                "<probableCause>351</probableCause>" +
                "<alarmType>PROCESSING_ERROR_ALARM</alarmType>" +
                "<alarmChangedTime>2023-04-06T08:31:41.244466+00:00</alarmChangedTime>" +
                "<notificationId>590443372</notificationId>" +
                "<objectInstance>ManagedElement=ocp83vcu03o1,GNBCUCPFunction=1</objectInstance>" +
                "<alarmId>6c46f46b-ee28-4d71-b998-d575d87efa99</alarmId>" +
                "<additionalInformation>{\"AdditionalInformationTAG_A\": \"testExecution\"}</additionalInformation>";
    }

    public String invalidAlarmRecords() {
        return "<additionalText>3</additionalText>" +
                "<perceivedSeverity>3</perceivedSeverity>" +
                "<specificProblem>3</specificProblem>" +
                "<probableCause>3</probableCause>" +
                "<alarmType>3</alarmType>" +
                "<alarmChangedTime>3</alarmChangedTime>" +
                "<notificationId>599801250</notificationId>" +
                "<objectInstance>2</objectInstance>" +
                "<alarmId>1</alarmId>";
    }
}
