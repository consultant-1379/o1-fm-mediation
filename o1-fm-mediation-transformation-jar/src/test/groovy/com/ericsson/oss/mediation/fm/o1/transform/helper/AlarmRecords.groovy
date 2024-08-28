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
package com.ericsson.oss.mediation.fm.o1.transform.helper


class AlarmRecords {

    static final Map<String, Object> ALARM_ONE = [
            'alarmId'              : '124578512',
            'objectInstance'       : 'ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
            'notificationId'       : 599801250,
            'alarmChangedTime'     : '2023-10-20T07:01:25.02+00:00',
            'alarmType'            : 'COMMUNICATIONS_ALARM',
            'probableCause'        : '14',
            'specificProblem'      : 'GNBCUCP Service Unavailable',
            'perceivedSeverity'    : 'MINOR',
            'additionalText'       : 'No F1-C link exists to gNodeB-CU-CP listed in Additional Information',
            'additionalInformation': ['PLMN': '262-80', 'UserPlaneDomain': 'defaultUPD'],
            'discriminatorList'    : ['discriminatorList1', 'discriminatorList2', 'discriminatorList3']
    ]
    static final Map<String, Object> ALARM_TWO = [
            'alarmId'              : 'dd03de9f-1fa3-4733-8688-9b2f23b6d13c',
            'objectInstance'       : 'ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
            'notificationId'       : 599801250,
            'alarmRaisedTime'      : '2023-10-20T07:01:26.02+00:00',
            'alarmType'            : 'COMMUNICATIONS_ALARM',
            'probableCause'        : '2',
            'specificProblem'      : 'GNBCUUP Service Unavailable',
            'perceivedSeverity'    : 'WARNING',
            'additionalText'       : 'No F1-C link exists to gNodeB-CU-UP listed in Additional Information',
            'additionalInformation': '{\"PLMN\": \"262-80\", \"UserPlaneDomain\": \"defaultUPD\"}',
            'discriminatorList'    : ['discriminatorList1', 'discriminatorList2', 'discriminatorList3']
    ]
    static final Map<String, Object> ALARM_THREE = [
            'alarmId'              : 'dd03de9f-1fa3-4733-8688-9b2f23b6d13d',
            'objectInstance'       : 'ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
            'notificationId'       : 599801250,
            'alarmChangedTime'     : '2023-10-20T07:01:28.02+00:00',
            'alarmRaisedTime'      : '2023-10-20T07:01:27.02+00:00',
            'alarmType'            : 'COMMUNICATIONS_ALARM',
            'probableCause'        : '1',
            'specificProblem'      : 'GNBDU Service Unavailable',
            'perceivedSeverity'    : 'MAJOR',
            'additionalText'       : 'No F1-C link exists to gNodeB-DU listed in Additional Information',
            'additionalInformation': '{\"PLMN\": \"262-80\", \"UserPlaneDomain\": \"defaultUPD\"}',
            'discriminatorList'    : ['discriminatorList1', 'discriminatorList2', 'discriminatorList3']
    ]

    static final Map<String, Object> ALARM_NO_ALARM_ID = [
            'objectInstance'       : 'ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
            'notificationId'       : 599801250,
            'alarmChangedTime'     : '2023-10-20T07:01:28.02+00:00',
            'alarmRaisedTime'      : '2023-10-20T07:01:27.02+00:00',
            'alarmType'            : 'COMMUNICATIONS_ALARM',
            'probableCause'        : '1',
            'specificProblem'      : 'GNBDU Service Unavailable',
            'perceivedSeverity'    : 'MAJOR',
            'additionalText'       : 'No F1-C link exists to gNodeB-DU listed in Additional Information',
            'additionalInformation': '{\"PLMN\": \"262-80\", \"UserPlaneDomain\": \"defaultUPD\"}'
    ]
    static final Map<String, Object> INVALID_ALARM = [
            'alarmId'          : 1,
            'objectInstance'   : 2,
            'notificationId'   : 599801250,
            'alarmChangedTime' : 3,
            'alarmType'        : 3,
            'probableCause'    : 3,
            'specificProblem'  : 3,
            'perceivedSeverity': 3,
            'additionalText'   : 3
    ]

    static final Map<String, Object> INVALID_ALARM_ADDITIONAL_INFORMATION = [
            'alarmId'              : 'dd03de9f-1fa3-4733-8688-9b2f23b6d13e',
            'objectInstance'       : 'ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
            'notificationId'       : 599801250,
            'alarmChangedTime'     : '2023-10-20T07:01:27.02+00:00',
            'alarmType'            : 'COMMUNICATIONS_ALARM',
            'probableCause'        : '1',
            'specificProblem'      : 'GNBDU Service Unavailable',
            'perceivedSeverity'    : 'MAJOR',
            'additionalText'       : 'No F1-C link exists to gNodeB-DU listed in Additional Information',
            'additionalInformation': '{' +
                    '\"SDI\": ' +
                    '{\"P\":{\"I\":\"3763343d-3617-4fae-b42d-bbca30179473\",' +
                    '\"N\":\"eric-ran-cu-cp-rc-central-rc-4-0\" ' +
                    '},' +
                    '\"S\":\"1\"' +
                    ' } ' +
                    ' } '
    ]
    static final String ME_CONTEXT_FDN = 'SubNetwork=5G141O1001,MeContext=5G141O1001'


    static final List<Map<String, Object>> ALARM_RECORDS = Arrays.asList(ALARM_ONE, ALARM_TWO, ALARM_THREE);

    static final List<Map<String, Object>> ALARM_RECORDS_INVALID_ADDITIONAL_INFORMATION = Arrays.asList(ALARM_ONE, ALARM_TWO, ALARM_THREE, INVALID_ALARM_ADDITIONAL_INFORMATION);

    static final List<Map<String, Object>> ALARM_RECORDS_WITH_SINGLE_ALARM_NO_ALARM_ID = Arrays.asList(ALARM_ONE, ALARM_TWO, ALARM_THREE, ALARM_NO_ALARM_ID);

    static final List<Map<String, Object>> ALARM_RECORDS_NO_ALARM_ID = Arrays.asList(ALARM_NO_ALARM_ID);

    static final List<Map<String, Object>> FAULTY_ALARM_RECORDS = Arrays.asList(ALARM_ONE, ALARM_TWO, ALARM_THREE, INVALID_ALARM);
}
