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
package com.ericsson.oss.mediation.fm.o1.transform.v11

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.transform.TransformManager
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.NamedConverters
import com.ericsson.oss.mediation.fm.o1.transform.mapper.AlarmRecordsMapperImpl
import com.ericsson.oss.mediation.fm.o1.transform.mapper.NotifyAlarmListRebuiltMapperImpl
import com.ericsson.oss.mediation.fm.o1.transform.mapper.NotifyAlarmMapperImpl
import com.ericsson.oss.mediation.o1.event.model.O1Notification
import com.ericsson.oss.mediation.o1.event.service.O1NotificationNormalizer
import com.ericsson.oss.mediation.translator.model.EventNotification

import javax.inject.Inject

class TransformManagerSpecV11 extends CdiSpecification {

    @ObjectUnderTest
    private TransformManager transformManager

    @Inject
    private O1NotificationNormalizer o1NotificationNormalizer

    @ImplementationClasses
    def classes = [AlarmRecordsMapperImpl, NotifyAlarmMapperImpl, NotifyAlarmListRebuiltMapperImpl, NamedConverters]

    def setup() {
        transformManager.initCache()
    }

    def "Test v11 notifyNewAlarm notification gets transformed correctly"() {

        given: "a valid v11 Notification"
            def v11Notification = '''
                {
                    "event": {
                        "commonEventHeader": {
                            "domain": "stndDefined",
                            "eventId": "1698399774107997687",
                            "eventName": "stndDefined_Vscf:Acs-Ericsson_ProcessingErrorAlarm",
                            "lastEpochMicrosec": 1698399774107999200,
                            "priority": "Normal",
                            "reportingEntityName": "ibcx0001vm002oam001",
                            "sequence": 0,
                            "sourceName": "scfx0001vm002cap001",
                            "startEpochMicrosec": 1698399774107999200,
                            "stndDefinedNamespace": "3GPP-FaultSupervision",
                            "timeZoneOffset": "UTC+00:00",
                            "version": "4.1",
                            "vesEventListenerVersion": "7.2"
                        },
                        "stndDefinedFields": {
                            "data": {
                                "additionalInformation": {
                                    "AddionalInformationTAG_A": "testExecution"
                                },
                                "additionalText": "testExecution",
                                "alarmId": "07f13fd9-1100-4a4c-858a-e237bd927774",
                                "alarmType": "PROCESSING_ERROR_ALARM",
                                "eventTime": "2023-10-27T09:42:54.107996408Z",
                                "href": "https://ocp83vcu09ne.MeContext.ocp83vcu09sn.SubNetwork/ManagedElement=ocp83vcu09ne/GNBCUCPFunction=1",
                                "notificationId": 2060789746,
                                "notificationType": "notifyNewAlarm",
                                "perceivedSeverity": "MINOR",
                                "probableCause": 157,
                                "specificProblem": "test 1 specific problem for the alarm.",
                                "systemDN": "SubNetwork=ocp83vcu09sn,MeContext=ocp83vcu09ne,ManagedElement=ocp83vcu01hine,MnsAgent=FM"
                            },
                            "schemaReference": "https://forge.3gpp.org/rep/sa5/MnS/-/blob/Rel-18/OpenAPI/TS28532_FaultMnS.yaml#/components/schemas/NotifyNewAlarm",
                            "stndDefinedFieldsVersion": "1.0"
                        }
                    }
                }'''

        when: "normalize the JSON Notification"
            O1Notification o1Notification = o1NotificationNormalizer.normalize(v11Notification)
            EventNotification eventNotification = transformManager.translateAlarm(o1Notification.getNormalisedNotification())

        then: "a transformed alarm is returned"
            convertToMap(eventNotification) == [
                    eventType            : 'PROCESSING_ERROR_ALARM',
                    probableCause        : 'm3100LossOfRealTimel',
                    perceivedSeverity    : 'MINOR',
                    recordType           : 'ALARM',
                    specificProblem      : 'test 1 specific problem for the alarm.',
                    eventAgentId         : '07f13fd9-1100-4a4c-858a-e237bd927774',
                    timeZone             : 'GMT+00:00',
                    eventTime            : '20231027094254.107',
                    managedObjectInstance: 'SubNetwork=ocp83vcu09sn,MeContext=ocp83vcu09ne,ManagedElement=ocp83vcu09ne,GNBCUCPFunction=1',
                    externalEventId      : '',
                    sourceType           : '',
                    translateResult      : 'FORWARD_ALARM',
                    acknowledged         : false,
                    ackTime              : '',
                    operator             : '',
                    visibility           : true,
                    processingType       : 'NOT_SET',
                    fmxGenerated         : 'NOT_SET',
                    additionalAttributes : [
                            vesStartEpochMicrosec   : '1698399774107999200',
                            vesEventName            : 'stndDefined_Vscf:Acs-Ericsson_ProcessingErrorAlarm',
                            vesSequence             : '0',
                            vesLastEpochMicrosec    : '1698399774107999200',
                            vesPriority             : 'Normal',
                            vesSourceName           : 'scfx0001vm002cap001',
                            vesReportingEntityName  : 'ibcx0001vm002oam001',
                            AddionalInformationTAG_A: 'testExecution',
                            systemDN                : 'SubNetwork=ocp83vcu09sn,MeContext=ocp83vcu09ne,ManagedElement=ocp83vcu01hine,MnsAgent=FM',
                            generatedAlarmId        : '841988793',
                            fdn                     : 'NetworkElement=ocp83vcu09ne',
                            vesEventListenerVersion : '7.2',
                            vesVersion              : '4.1',
                            notificationId          : '2060789746',
                            vesEventId              : '1698399774107997687'],
                    discriminatorIdList  : [],
                    serialVersionUID     : 1
            ]
    }


    def "Test v11 netconf response xml of alarm records gets transformed correctly"() {
        given: "a valid netconf xml response"
            def v11NetconfResponseXml =
                    '''<ManagedElement xmlns="urn:3gpp:sa5:_3gpp-common-managed-element">
                          <AlarmList>
                            <attributes>
                              <administrativeState>UNLOCKED</administrativeState>
                              <operationalState>ENABLED</operationalState>
                              <numOfAlarmRecords>1</numOfAlarmRecords>
                              <lastModification>2024-06-05T12:35:55.077+00:00</lastModification>
                              <alarmRecords>
                                <alarmId>f4a872b0-35d9-4aa9-927a-120b453f69a0</alarmId>
                                <objectInstance>ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3</objectInstance>
                                <notificationId>2093795295</notificationId>
                                <alarmChangedTime>2024-06-05T12:35:55.077+00:00</alarmChangedTime>
                                <alarmType>PROCESSING_ERROR_ALARM</alarmType>
                                <probableCause>157</probableCause>
                                <specificProblem>test 1 specific problem for the alarm.</specificProblem>
                                <perceivedSeverity>MINOR</perceivedSeverity>
                                <additionalText>Mandatory deployment parameter(s) not specified</additionalText>
                                <additionalInformation>{"AddionalInformationTAG_A": "ABCDefg", "TAG_B": "tagB"}</additionalInformation>
                              </alarmRecords>
                            </attributes>
                          </AlarmList>
                        </ManagedElement>
                    '''

        when: "normalize the xml response"
            List<EventNotification> eventNotificationList = transformManager.transformAlarmRecords(v11NetconfResponseXml, "cucp-070-02")

        then: "a transformed alarm records is returned"


            convertEventNotificationListToMap(eventNotificationList).get(0) == [
                    serialVersionUID     : 1,
                    eventType            : 'PROCESSING_ERROR_ALARM',
                    additionalAttributes : [
                            AddionalInformationTAG_A: 'ABCDefg',
                            generatedAlarmId        : '1015041234',
                            fdn                     : '',
                            notificationId          : '2093795295',
                            TAG_B                   : 'tagB'],
                    probableCause        : 'm3100LossOfRealTimel',
                    perceivedSeverity    : 'MINOR',
                    recordType           : 'SYNCHRONIZATION_ALARM',
                    specificProblem      : 'test 1 specific problem for the alarm.',
                    eventAgentId         : 'f4a872b0-35d9-4aa9-927a-120b453f69a0',
                    timeZone             : '',
                    eventTime            : '20240605123555.077',
                    managedObjectInstance: 'cucp-070-02,ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
                    externalEventId      : '',
                    sourceType           : '',
                    translateResult      : 'FORWARD_ALARM',
                    acknowledged         : false,
                    ackTime              : '',
                    operator             : '',
                    visibility           : true,
                    processingType       : 'NOT_SET',
                    fmxGenerated         : 'NOT_SET',
                    discriminatorIdList  : []
            ]
    }

    static List<Map<String, Object>> convertEventNotificationListToMap(List<EventNotification> eventNotificationList) {
        def List<Map<String, Object>> eventNotificationMap = new ArrayList<>()
        eventNotificationList.each { eventNotificationMap.add(convertToMap(it)) }
        return eventNotificationMap
    }

    static Map<String, Object> convertToMap(EventNotification eventNotification) {
        return eventNotification.getClass().getDeclaredFields()
                .each { it.setAccessible(true) }
                .collectEntries { [it.getName(), it.get(eventNotification)] }

    }

}
