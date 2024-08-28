package com.ericsson.oss.mediation.fm.o1.transform.helper

class CommonAlarmFields {

    static final String EVENT_ID = "eventId"
    static final String EVENT_ID_VAL = "fault000001"
    static final String EVENT_NAME = "eventName"
    static final String EVENT_NAME_VAL = "stndDefined_Vscf:Acs-Ericsson_ProcessingErrorAlarm-00001"
    static final String EVENT_TYPE = "eventType"
    static final String EVENT_TYPE_VAL = "stndDefined_Vscf:Acs-Ericsson_ProcessingErrorAlarm"
    static final String START_EPOCH_MICRO_SEC = "startEpochMicrosec"
    static final String START_EPOCH_MICRO_SEC_VAL = "1698399774107999200"
    static final String LAST_EPOCH_MICRO_SEC = "lastEpochMicrosec"
    static final String LAST_EPOCH_MICRO_SEC_VAL = "1678271515731560000"
    static final String PRIORITY = "priority"
    static final String PRIORITY_VAL = "Normal"
    static final String REPORTING_ENTITY_NAME = "reportingEntityName"
    static final String REPORTING_ENTITY_NAME_VAL = "ibcx0001vm002oam001"
    static final String SEQUENCE = "sequence"
    static final String SEQUENCE_VAL = "5"
    static final String SOURCE_NAME = "sourceName"
    static final String SOURCE_NAME_VAL = "scfx0001vm002cap001"
    static final String VERSION = "version"
    static final String VERSION_VAL = "4.1"
    static final String EVENT_LISTENER_VERSION = "vesEventListenerVersion"
    static final String EVENT_LISTENER_VERSION_VAL = "7.2"


    def static createCommonAlarmFields() {

        final Map<String, Object> alarmFields = new HashMap<>();
        alarmFields.put(EVENT_ID, EVENT_ID_VAL)
        alarmFields.put(EVENT_NAME, EVENT_NAME_VAL)
        alarmFields.put(EVENT_TYPE, EVENT_TYPE_VAL)
        alarmFields.put(START_EPOCH_MICRO_SEC, START_EPOCH_MICRO_SEC_VAL)
        alarmFields.put(LAST_EPOCH_MICRO_SEC, LAST_EPOCH_MICRO_SEC_VAL)
        alarmFields.put(PRIORITY, PRIORITY_VAL)
        alarmFields.put(REPORTING_ENTITY_NAME, REPORTING_ENTITY_NAME_VAL)
        alarmFields.put(SEQUENCE, SEQUENCE_VAL)
        alarmFields.put(SOURCE_NAME, SOURCE_NAME_VAL)
        alarmFields.put(VERSION, VERSION_VAL)
        alarmFields.put(EVENT_LISTENER_VERSION, EVENT_LISTENER_VERSION_VAL)

        return alarmFields
    }

    def static createCommonNotifyAlarmFields() {

        final Map<String, Object> ALARM_FIELDS = [
                'href'             : 'https://5G141O1001.MeContext.skylight.SubNetwork.top.SubNetwork',
                'notificationId'   : '1234567',
                'eventTime'        : '2023-09-06T11:32:01.743777Z',
                'systemDN'         : 'ManagedElement=1,MnsAgent=FM',
                'alarmType'        : 'PROCESSING_ERROR_ALARM',
                'perceivedSeverity': 'MAJOR'
        ]
    }
}
