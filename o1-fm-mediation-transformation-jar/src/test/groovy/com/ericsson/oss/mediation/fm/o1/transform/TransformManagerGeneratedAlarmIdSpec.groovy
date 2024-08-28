package com.ericsson.oss.mediation.fm.o1.transform

import static com.ericsson.oss.mediation.fm.o1.transform.helper.TransformedEventNotifications.assertEventNotificationEquals
import static com.ericsson.oss.mediation.fm.o1.transform.helper.TransformedEventNotifications.createExpectedTransformedEventNotification;
import static com.ericsson.oss.mediation.fm.o1.transform.helper.TransformedEventNotifications.createExpectedTransformedEventNotificationList;

import org.apache.commons.lang3.StringUtils

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.NamedConverters
import com.ericsson.oss.mediation.fm.o1.transform.helper.AlarmRecords
import com.ericsson.oss.mediation.fm.o1.transform.helper.NotifyAlarm
import com.ericsson.oss.mediation.fm.o1.transform.mapper.AlarmRecordsMapperImpl
import com.ericsson.oss.mediation.fm.o1.transform.mapper.NotifyAlarmListRebuiltMapperImpl
import com.ericsson.oss.mediation.fm.o1.transform.mapper.NotifyAlarmMapperImpl
import com.ericsson.oss.mediation.translator.model.EventNotification

import spock.lang.Unroll

import javax.inject.Inject

class TransformManagerGeneratedAlarmIdSpec extends CdiSpecification {

    @Inject
    private SystemRecorder systemRecorder

    @ObjectUnderTest
    private TransformManager transformManager

    @ImplementationClasses
    def classes = [AlarmRecordsMapperImpl, NotifyAlarmMapperImpl, NotifyAlarmListRebuiltMapperImpl, NamedConverters]

    public Map<String, Object> BASE_ALARM = [
            'notificationType'     : 'notifyNewAlarm',
            'alarmId'              : '1',
            'href'                 : 'href',
            'objectInstance'       : 'ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
            'notificationId'       : 599801250,
            'alarmChangedTime'     : '2023-10-20T07:01:25.02+00:00',
            'alarmType'            : 'COMMUNICATIONS_ALARM',
            'probableCause'        : '14',
            'specificProblem'      : 'GNBCUCP Service Unavailable',
            'perceivedSeverity'    : 'MINOR',
            'additionalText'       : 'No F1-C link exists to gNodeB-CU-CP listed in Additional Information',
            'additionalInformation': ['PLMN': '262-80', 'UserPlaneDomain': 'defaultUPD']
    ]

    @Unroll
    def "Test generatedAlarmId is numeric"() {

        given: "a valid alarm"
            BASE_ALARM['alarmId'] = alarmId

        when: "transform manager is called to translate the alarm"
            def translatedAlarm = transformManager.translateAlarm(BASE_ALARM)

        then: "the generatedAlarmId is created and validated as numeric"
            def generatedAlarmId = translatedAlarm.additionalAttributes['generatedAlarmId']
            assert StringUtils.isNumeric(generatedAlarmId)

        where: "alarmId is:"
            alarmId << [1, "1", "9cf9a4a0-5271-490d-87ce-3727d823f32c"]
    }

    @Unroll
    def "Test generatedAlarmId uniqueness in ranges of integers"() {
        given:
            Set<Integer> generatedAlarmIds = new HashSet<>(to - from + 1);
        expect:
            for (int i = from; i <= to; i++) {
                BASE_ALARM['alarmId'] = String.valueOf(i)
                def translatedAlarm = transformManager.translateAlarm(BASE_ALARM)
                assert generatedAlarmIds.add(translatedAlarm.additionalAttributes['generatedAlarmId'])
            }
        where:
            from    | to
            1       | 100000
            5000000 | 5100000
    }

    def "Test generatedAlarmId is not created when alarmId is not provided"() {

        given: "an alarm with no alarmId"
            BASE_ALARM.remove('alarmId')

        when: "transform manager is called to translate the alarm"
            transformManager.translateAlarm(BASE_ALARM)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Error while converting alarm map to eventnotification")

        and: "An error is recorded for an invalid alarm without an alarmId"
            1 * systemRecorder.recordError("Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: Missing mandatory field alarmId]", ErrorSeverity.WARNING, "ModelTransformer", "",
                    "For alarm with the following ID: null")
    }

    @Unroll
    def "Test generatedAlarmId is not created when alarmId is blank"() {

        given: "an alarm"
            BASE_ALARM['alarmId'] = alarmId

        when: "transform manager is called to translate the alarm"
            transformManager.translateAlarm(BASE_ALARM)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.getCause().message.contains("Missing mandatory field alarmId")

        and: "An error is recorded for an invalid alarm with a blank alarmId"
            1 * systemRecorder.recordError("Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: Missing mandatory field alarmId]", ErrorSeverity.WARNING, "ModelTransformer", "",
                    "For alarm with the following ID: " + alarmId)

        where: "alarmId is blank"
            alarmId << [null, "", " "]
    }

    def "Test list of AlarmList.alarmRecords with no alarmId(s) gets discarded"() {

        given: "a list of alarmRecords with no alarmId(s)"
            def alarmRecords = AlarmRecords.ALARM_RECORDS_NO_ALARM_ID

        and: "TransformManager is initialized"
            transformManager.initCache()

        when: "the transform manager is called to translate the alarmRecords"
            final List<EventNotification> event = transformManager.transformAlarmRecords(alarmRecords, AlarmRecords.ME_CONTEXT_FDN)

        then: "no alarmRecords are present in the AlarmList"
            assert event.size() == 0

        and: "An error is recorded for an invalid alarm without an alarmId"
            1 * systemRecorder.recordError("Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: Missing mandatory field alarmId]", ErrorSeverity.WARNING, "ModelTransformer", "",
                    "For the alarm with the following ID: null")
    }

    @Unroll
    def "Test that alarms without alarmId get discarded for #notificationType that has alarmId as a mandatory attribute"() {

        given: "a NotifyAlarm for all NotificationTypes that require alarmId"
            def alarmProperties = NotifyAlarm.createNotifyAlarm(notificationType)

        and: "alarmId is removed"
            alarmProperties.remove('alarmId')

        and: "TransformManager is initialized"
            transformManager.initCache()

        when: "the transform manager is called to translate alarm"
            transformManager.translateAlarm(alarmProperties)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.getCause().message.contains("Missing mandatory field alarmId")

        and: "An error is recorded for the invalid alarm without an alarmId"
            1 * systemRecorder.recordError("Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: Missing mandatory field alarmId]", ErrorSeverity.WARNING, "ModelTransformer", "",
                    "For alarm with the following ID: null")

        where: "notificationType to transform is"
            notificationType            | _
            "notifyClearedAlarm"        | _
            "notifyNewAlarm"            | _
            "notifyChangedAlarmGeneral" | _
            "notifyChangedAlarm"        | _
    }

    def "Test alarmRecords in AlarmList are still transformed when single alarmRecord has no alarmId"() {

        given: "a list of valid alarmRecords and an invalid alarmRecord containing no alarmId"
            def alarmRecords = AlarmRecords.ALARM_RECORDS_WITH_SINGLE_ALARM_NO_ALARM_ID

        and: "TransformManager is initialized"
            transformManager.initCache()

        when: "the transform manager is called to translate the alarmRecords"
            final List<EventNotification> event = transformManager.transformAlarmRecords(alarmRecords, AlarmRecords.ME_CONTEXT_FDN)

        then: "a list of transformed alarms are returned except for the invalid alarmRecord without alarmId"
            assert event.size() == 3
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(0), event.get(0))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(1), event.get(1))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(2), event.get(2))

        and: "the alarm without an alarmId is not transformed and a warning is recorded"
            1 * systemRecorder.recordError("Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: Missing mandatory field alarmId]", ErrorSeverity.WARNING, "ModelTransformer", "",
                    "For the alarm with the following ID: null")
    }

    def "Test valid notification for notifyAlarmListRebuilt without an alarmId"() {

        given: "a valid notifyAlarmListRebuilt without alarmId"
            def alarmProperties = NotifyAlarm.createNotifyAlarm("notifyAlarmListRebuilt")

        and: "TransformManager is initialized"
            transformManager.initCache()

        when: "the transform manager is called to translate alarm"
            final EventNotification event = transformManager.translateAlarm(alarmProperties)

        then: "the generatedAlarmId is not created"
            def generatedAlarmId = event.additionalAttributes['generatedAlarmId']
            assert generatedAlarmId == null

        and: "the event is translated"
            EventNotification eventNotification = createExpectedTransformedEventNotification("notifyAlarmListRebuilt")

        and: "a transformed alarm is returned"
            assertEventNotificationEquals(eventNotification, event)
    }
}
