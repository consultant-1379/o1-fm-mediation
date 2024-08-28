
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.mediation.fm.o1.nodesuspender.O1NodeSuspenderCacheManager.NODESUSPENDER_CACHE_NAME;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache;
import com.ericsson.cds.cdi.support.rule.ImplementationClasses;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationResult;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult;
import com.ericsson.oss.mediation.fm.o1.nodesuspender.O1NodeSuspenderAgent;
import com.ericsson.oss.mediation.fm.supervision.response.AlarmSupervisionResponse;
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgent;
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgentImpl;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.mediation.sdk.event.SupervisionMediationTaskRequest;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;
import com.ericsson.oss.services.fm.service.model.FmMediationAlarmSyncRequest;

public class FmSupervisionFlowBase extends O1BaseFlowTest {

    @MockedImplementation
    EventSender<AlarmSupervisionResponse> alarmSupervisionResponseEventSender;

    @MockedImplementation
    EventSender<MediationTaskRequest> mediationTaskRequestEventSender;

    @Inject
    DataPersistenceService dps;

    @Inject
    O1NodeSuspenderAgent o1NodeSuspenderAgent;

    @Inject
    O1HeartbeatAgent o1HeartbeatAgent;

    @ImplementationClasses
    protected final Class<?>[] definedImplementations = {
        O1HeartbeatAgentImpl.class
    };

    @Before
    public void setupMocks() throws NetconfManagerException {
        when(cacheProviderBean.createOrGetModeledCache(eq(NODESUSPENDER_CACHE_NAME))).thenReturn(new InMemoryCache(NODESUSPENDER_CACHE_NAME));
        when(netconfManagerOperation.executeXAResourceOperation(any(NetconfOperationRequest.class)))
                .thenReturn(createOkResponse());
        mockReadManagedElementId();
    }

    protected void verifyAlarmSupervisionResponseSent(final String expectedResult) {
        verify(alarmSupervisionResponseEventSender)
                .send((AlarmSupervisionResponse) argThat(e -> ((AlarmSupervisionResponse) e).getResult() == expectedResult));
    }

    protected void verifyAlarmSupervisionResponseNotSent() {
        verify(alarmSupervisionResponseEventSender, never()).send(any(AlarmSupervisionResponse.class));
    }

    protected void verifyFmMediationAlarmSyncRequestSent() {
        verify(mediationTaskRequestEventSender)
                .send((MediationTaskRequest) argThat(mtr -> mtr instanceof FmMediationAlarmSyncRequest));
    }

    protected void verifyFmMediationAlarmSyncRequestNotSent() {
        verify(mediationTaskRequestEventSender, never()).send(any(MediationTaskRequest.class));
    }

    protected static SupervisionMediationTaskRequest createSupervisionMediationTaskRequest(final String nodeName, final boolean active) {
        final Map<String, Object> supervisionAttributes = new HashMap<>();
        supervisionAttributes.put("name", "FmAlarmSupervision");
        supervisionAttributes.put("active", active);
        supervisionAttributes.put("version", "1.1.0");

        final SupervisionMediationTaskRequest request = new SupervisionMediationTaskRequest();
        request.setJobId("JOB_ID_1");
        request.setClientType("SUPERVISION");
        request.setNodeAddress("NetworkElement=" + nodeName + ",FmAlarmSupervision=1");
        request.setProtocolInfo("FM");
        request.setSupervisionAttributes(supervisionAttributes);
        return request;
    }

    protected void mockHeartbeatControlMoExists(boolean exists) throws NetconfManagerException {
        NetconfResponse response = new NetconfResponse();
        response.setError(!exists);
        when(netconfManagerOperation.get(argThat(f -> f != null && "/ManagedElement/NtfSubscriptionControl/HeartbeatControl".equals(f.asString()))))
                .thenReturn(response);
    }

    protected void mockReadManagedElementId() throws NetconfManagerException {
        String testString = "<ManagedElement " +
                "xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">" +
                "<id>ocp83vcu03o1</id>" +
                "<ExternalDomain " +
                "xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-external-domain-cr\">" +
                "<id>1</id>" +
                "</ExternalDomain>" +
                "</ManagedElement>";
        NetconfResponse response = new NetconfResponse();
        response.setData(testString);
        when(netconfManagerOperation.get(argThat(f -> f != null && "/ManagedElement/attributes".equals(f.asString()))))
                .thenReturn(response);
    }

    protected static NetconfOperationResult createOkResponse() {
        return new YangNetconfOperationResult(new NetconfResponse(),
                YangNetconfOperationResult.YangNetconfOperationResultCode.OK);
    }
}
