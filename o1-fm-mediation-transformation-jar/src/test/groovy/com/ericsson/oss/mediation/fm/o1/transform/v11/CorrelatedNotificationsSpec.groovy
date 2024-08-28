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

class CorrelatedNotificationsSpec extends CdiSpecification {

    @ObjectUnderTest
    private TransformManager transformManager

    @Inject
    private O1NotificationNormalizer o1NotificationNormalizer

    @ImplementationClasses
    def classes = [AlarmRecordsMapperImpl, NotifyAlarmMapperImpl, NotifyAlarmListRebuiltMapperImpl, NamedConverters]

    def setup() {
        transformManager.initCache()
    }

    def "Test one valid correlatedNotification gets transformed correctly"() {
        given:
            def correlatedNotification = '''
                {
                    "alarmId": "07f13fd9-1100-4a4c-858a-e237bd927774",
                    "href": "https://ocp83vcu09ne.MeContext.ocp83vcu09sn.SubNetwork/ManagedElement=ocp83vcu09ne/GNBCUCPFunction=1",
                    "notificationType": "notifyNewAlarm",
                    "correlatedNotifications":
                    [
                        {
                            "sourceObjectInstance": "ManagedElement=Node1",
                            "notificationId": [573, 575, 577, 580]
                        }
                    ]
                }
            '''

        when: "normalize the JSON Notification"
            O1Notification o1Notification = o1NotificationNormalizer.normalize(correlatedNotification)
            EventNotification eventNotification = transformManager.translateAlarm(o1Notification.getNormalisedNotification())

        then: "correlatedNotification attributes are added to EventNotification"
            eventNotification.getAdditionalAttributes() == [
                generatedAlarmId        : '841988793',
                fdn                     : 'NetworkElement=ocp83vcu09ne',
                correlatedNotifications_1_sourceObjectInstance: 'ManagedElement=Node1',
                correlatedNotifications_1_Ids_1: '573',
                correlatedNotifications_1_Ids_2: '575',
                correlatedNotifications_1_Ids_3: '577',
                correlatedNotifications_1_Ids_4: '580'
            ]

        and: "correlatedNotifications are in the correct order"
            eventNotification.getAdditionalAttributes().keySet().collect() ==
                [
                    'generatedAlarmId',
                    'fdn',
                    'correlatedNotifications_1_sourceObjectInstance',
                    'correlatedNotifications_1_Ids_1',
                    'correlatedNotifications_1_Ids_2',
                    'correlatedNotifications_1_Ids_3',
                    'correlatedNotifications_1_Ids_4'
                ]
    }

    def "Test more valid correlatedNotifications get transformed correctly"() {
        given:
            def correlatedNotification = '''
                {
                    "alarmId": "07f13fd9-1100-4a4c-858a-e237bd927774",
                    "href": "https://ocp83vcu09ne.MeContext.ocp83vcu09sn.SubNetwork/ManagedElement=ocp83vcu09ne/GNBCUCPFunction=1",
                    "notificationType": "notifyNewAlarm",
                    "correlatedNotifications": [
                        {
                            "sourceObjectInstance": "ManagedElement=Node1",
                            "notificationId": [573, 575]
                        },
                        {
                            "sourceObjectInstance": "ManagedElement=Node2",
                            "notificationId": [64]
                        }
                    ]
                }
            '''


        when: "normalize the JSON Notification"
            O1Notification o1Notification = o1NotificationNormalizer.normalize(correlatedNotification)
            EventNotification eventNotification = transformManager.translateAlarm(o1Notification.getNormalisedNotification())

        then: "correlatedNotification attributes are added to EventNotification"
            eventNotification.getAdditionalAttributes() == [
                generatedAlarmId        : '841988793',
                fdn                     : 'NetworkElement=ocp83vcu09ne',
                correlatedNotifications_1_sourceObjectInstance: 'ManagedElement=Node1',
                correlatedNotifications_1_Ids_1: '573',
                correlatedNotifications_1_Ids_2: '575',
                correlatedNotifications_2_sourceObjectInstance: 'ManagedElement=Node2',
                correlatedNotifications_2_Ids_1: '64'
            ]

        and: "correlatedNotifications are in the correct order"
            eventNotification.getAdditionalAttributes().keySet().collect() ==
                [
                    'generatedAlarmId',
                    'fdn',
                    'correlatedNotifications_1_sourceObjectInstance',
                    'correlatedNotifications_1_Ids_1',
                    'correlatedNotifications_1_Ids_2',
                    'correlatedNotifications_2_sourceObjectInstance',
                    'correlatedNotifications_2_Ids_1'
                ]
    }

    def "Test invalid correlatedNotification with empty sourceObjectInstance is not transformed"() {
        given:
            def correlatedNotification = '''
                {
                    "alarmId": "07f13fd9-1100-4a4c-858a-e237bd927774",
                    "href": "https://ocp83vcu09ne.MeContext.ocp83vcu09sn.SubNetwork/ManagedElement=ocp83vcu09ne/GNBCUCPFunction=1",
                    "notificationType": "notifyNewAlarm",
                    "correlatedNotifications":
                    [
                        {
                            "sourceObjectInstance": "",
                            "notificationId": [573, 575]
                        }
                    ]
                }
            '''

        when: "normalize the JSON Notification"
            O1Notification o1Notification = o1NotificationNormalizer.normalize(correlatedNotification)
            EventNotification eventNotification = transformManager.translateAlarm(o1Notification.getNormalisedNotification())

        then: "correlatedNotification attributes are not added to EventNotification"
            eventNotification.getAdditionalAttributes() == [
                generatedAlarmId        : '841988793',
                fdn                     : 'NetworkElement=ocp83vcu09ne'
            ]
    }

    def "Test invalid correlatedNotification with empty notificationId is not transformed"() {
        given:
            def correlatedNotification = '''
                {
                    "alarmId": "07f13fd9-1100-4a4c-858a-e237bd927774",
                    "href": "https://ocp83vcu09ne.MeContext.ocp83vcu09sn.SubNetwork/ManagedElement=ocp83vcu09ne/GNBCUCPFunction=1",
                    "notificationType": "notifyNewAlarm",
                    "correlatedNotifications":
                    [
                        {
                            "sourceObjectInstance": "ManagedElement=Node1",
                            "notificationId": []
                        }
                    ]
                }
            '''

        when: "normalize the JSON Notification"
            O1Notification o1Notification = o1NotificationNormalizer.normalize(correlatedNotification)
            EventNotification eventNotification = transformManager.translateAlarm(o1Notification.getNormalisedNotification())

        then: "correlatedNotification attributes are not added to EventNotification"
            eventNotification.getAdditionalAttributes() == [
                generatedAlarmId        : '841988793',
                fdn                     : 'NetworkElement=ocp83vcu09ne'
            ]
    }

    def "Test only valid correlatedNotifications from list are transformed correctly"() {
        given:
            def correlatedNotification = '''
                {
                    "alarmId": "07f13fd9-1100-4a4c-858a-e237bd927774",
                    "href": "https://ocp83vcu09ne.MeContext.ocp83vcu09sn.SubNetwork/ManagedElement=ocp83vcu09ne/GNBCUCPFunction=1",
                    "notificationType": "notifyNewAlarm",
                    "correlatedNotifications":
                    [
                        {
                            "sourceObjectInstance": "ManagedElement=Node1",
                            "notificationId": [70]
                        },
                        {
                            "sourceObjectInstance": "ManagedElement=Node2",
                            "notificationId": [800, 900]
                        },
                        {
                            "sourceObjectInstance": "",
                            "notificationId": [1]
                        },
                        {
                            "sourceObjectInstance": "ManagedElement=Node3",
                            "notificationId": []
                        },
                        {
                            "notificationId": [1000, 1001]
                        },
                        {
                            "sourceObjectInstance": "ManagedElement=Node4"
                        },
                        {
                            "sourceObjectInstance": "ManagedElement=Node5",
                            "notificationId": [20]
                        }
                    ]
                }
            '''

        when: "normalize the JSON Notification"
            O1Notification o1Notification = o1NotificationNormalizer.normalize(correlatedNotification)
            EventNotification eventNotification = transformManager.translateAlarm(o1Notification.getNormalisedNotification())

        then: "only valid correlatedNotification attributes are added to EventNotification"
            eventNotification.getAdditionalAttributes() == [
                generatedAlarmId        : '841988793',
                fdn                     : 'NetworkElement=ocp83vcu09ne',
                correlatedNotifications_1_sourceObjectInstance: 'ManagedElement=Node1',
                correlatedNotifications_1_Ids_1: '70',
                correlatedNotifications_2_sourceObjectInstance: 'ManagedElement=Node2',
                correlatedNotifications_2_Ids_1: '800',
                correlatedNotifications_2_Ids_2: '900',
                correlatedNotifications_3_sourceObjectInstance: 'ManagedElement=Node5',
                correlatedNotifications_3_Ids_1: '20'
            ]

        and: "correlatedNotifications are in the correct order"
            eventNotification.getAdditionalAttributes().keySet().collect() ==
                [
                    'generatedAlarmId',
                    'fdn',
                    'correlatedNotifications_1_sourceObjectInstance',
                    'correlatedNotifications_1_Ids_1',
                    'correlatedNotifications_2_sourceObjectInstance',
                    'correlatedNotifications_2_Ids_1',
                    'correlatedNotifications_2_Ids_2',
                    'correlatedNotifications_3_sourceObjectInstance',
                    'correlatedNotifications_3_Ids_1'
                ]
    }
}
