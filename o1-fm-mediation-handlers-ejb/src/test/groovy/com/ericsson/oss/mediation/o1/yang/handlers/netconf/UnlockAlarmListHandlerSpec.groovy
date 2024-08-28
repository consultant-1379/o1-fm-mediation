/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.model.ModelPattern
import com.ericsson.cds.cdi.support.providers.custom.model.local.LocalModelServiceProvider
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatusHelper
import org.apache.commons.lang3.StringUtils

import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.FDN
import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_SUCCESS
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.INCLUDE_NS_PREFIX

class UnlockAlarmListHandlerSpec extends CdiSpecification {

    private static final boolean SUPERVISION_ON = true;
    private static final boolean SUPERVISION_OFF = false;
    private static final String NETWORK_ELEMENT_FDN = "NetworkElement=5G141O1001"
    private static final String MECONTEXT_FDN = "SubNetwork=Ireland,SubNetwork=Athlone,MeContext=5G141O1001"
    private static final String FM_ALARM_SUPERVISION_FDN = "NetworkElement=5G141O1001,FmAlarmSupervision=1"
    public static final String SUPERVISION_ATTRIBUTES = "supervisionAttributes";

    @ObjectUnderTest
    private UnlockAlarmListHandler unlockAlarmListHandler

    @MockedImplementation
    private EventHandlerContext mockEventHandlerContext

    @MockedImplementation
    NetconfManagerOperation netconfManagerOperation

    private final Map<String, Object> headers = new HashMap<>()

    private static filteredModels = [
            new ModelPattern('.*', '.*', '.*', '.*')
    ]

    private static LocalModelServiceProvider localModelServiceProvider = new LocalModelServiceProvider(filteredModels)

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        super.addAdditionalInjectionProperties(injectionProperties)
        injectionProperties.addInjectionProvider(localModelServiceProvider)
    }

    def setup() {
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        addO1NodeToDps()
    }

    def "Test when NetConf connection manager throws exception the operations result is failed"() {
        given: "headers for event with all required attributes"
            prepareO1HandlersSuccess()
            prepareHeaders(SUPERVISION_ON)

        and: "the netconf manager provided is setup to fail on execution"
            headers.put('netconfManager', netconfManagerOperation)

        and: "the UnlockAlarmListHandler is initialized"
            unlockAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = unlockAlarmListHandler.onEventWithResult(inputEvent)

        then: "exception is thrown"
            netconfManagerOperation.executeXAResourceOperation(_) >> { throw new RuntimeException("Error") }

        and: "handler operation is set to operation failed"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "Error")
    }

    def "Test when supervision is disabled then there is no update to AlarmList.administrativeState"() {
        given: "headers for event with all required attributes"
            prepareO1HandlersSuccess()
            prepareHeaders(SUPERVISION_OFF)

        and: "a valid netconf manager is provided"
            headers.put('netconfManager', netconfManagerOperation)

        and: "the UnlockAlarmListHandler is initialized"
            unlockAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = unlockAlarmListHandler.onEventWithResult(inputEvent)

        then: "there is no call to the connection manager"
            0 * netconfManagerOperation.executeXAResourceOperation(_)

        and: "there is no exception thrown"
            notThrown(Exception)

        and: "handler operation is set to operation aborted"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_ABORTED, null)
    }

    def "Test when supervision is enabled then AlarmList.administrativeState set to UNLOCKED"() {
        given: "headers for event with all required attributes"
            prepareO1HandlersSuccess()
            prepareHeaders(true)

        and: "a valid netconf manager is provided"
            headers.put('netconfManager', netconfManagerOperation)

        and: "the UnlockAlarmListHandler is initialized"
            unlockAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = unlockAlarmListHandler.onEventWithResult(inputEvent)

        then: "there is one call to the connection manager with the expected request details"
            1 * netconfManagerOperation.executeXAResourceOperation({
                String operationString = it.toString()
                operationString.contains(getExpectedRequestDetails("MERGE")) &&
                        operationString.contains("Attribute name=administrativeState Attribute value=UNLOCKED")
            }) >> YangNetconfOperationResult.OK

        and: "handler operation is set to operation success"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_SUCCESS, null)
    }

    def addO1NodeToDps() {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn(MECONTEXT_FDN)
                .build();

        ManagedObject networkElement = configurableDps.addManagedObject()
                .withFdn(NETWORK_ELEMENT_FDN)
                .addAttributes([neType: "O1Node"])
                .withAssociation("nodeRootRef", meContext)
                .build()

        meContext.addAssociation('networkElementRef', networkElement)
    }

    def prepareHeaders(boolean active) {
        def map = [active: active]
        headers.put(FDN, FM_ALARM_SUPERVISION_FDN)
        headers.put(SUPERVISION_ATTRIBUTES, map)
        headers.put(INCLUDE_NS_PREFIX, true)
        headers.put('ManagedElementId', '5G141O1001')
        headers.put('flowUrn', 'AlarmSyncFlow')
    }

    def prepareO1HandlersSuccess() {
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(ReadAlarmListHandler.class, OPERATION_SUCCESS, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(ReadAlarmListHandler.class, OPERATION_SUCCESS, null), headers);
    }


    def createOperation(final Class handler, final O1NetconfOperationResult result, final Exception e) {
        final O1NetconfOperationStatus operationStatus = new O1NetconfOperationStatus(handler, "NetworkElement=TestNode,FmAlarmSupervision=1")
        operationStatus.setResult(result)
        operationStatus.setException(e)
        return operationStatus
    }

    private String getExpectedRequestDetails(String operation) {
        return "operation=" + operation + ", " +
                "fdn=SubNetwork=Ireland,SubNetwork=Athlone,MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1, " +
                "namespace=urn:3gpp:sa5:_3gpp-common-managed-element, " +
                "name=AlarmList, nodeType=O1Node"
    }

    private void verifyOperation(final Map<String, Object> headers, final O1NetconfOperationResult result, final String exceptionMessage) {
        final O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(headers)
        final O1NetconfOperationStatus operationStatus = operationsStatus.getOperation(UnlockAlarmListHandler.class)
        assert operationStatus.getResult() == result
        if (!StringUtils.isBlank(exceptionMessage)) {
            assert operationStatus.getException().getMessage() == exceptionMessage
        }
    }
}
