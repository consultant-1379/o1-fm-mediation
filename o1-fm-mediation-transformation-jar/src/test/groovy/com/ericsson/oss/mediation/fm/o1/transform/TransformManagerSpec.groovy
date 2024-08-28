package com.ericsson.oss.mediation.fm.o1.transform

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

import static com.ericsson.oss.mediation.fm.o1.transform.helper.TransformedEventNotifications.assertEventNotificationEquals
import static com.ericsson.oss.mediation.fm.o1.transform.helper.TransformedEventNotifications.createExpectedTransformedEventNotification
import static com.ericsson.oss.mediation.fm.o1.transform.helper.TransformedEventNotifications.createExpectedTransformedEventNotificationAlarmWithInvalidAdditionalInfo
import static com.ericsson.oss.mediation.fm.o1.transform.helper.TransformedEventNotifications.createExpectedTransformedEventNotificationList

class TransformManagerSpec extends CdiSpecification {

    @ObjectUnderTest
    private TransformManager transformManager

    @Inject
    private SystemRecorder systemRecorder

    @ImplementationClasses
    def classes = [AlarmRecordsMapperImpl, NotifyAlarmMapperImpl, NotifyAlarmListRebuiltMapperImpl, NamedConverters]

    def setup() {
        transformManager.initCache()
    }

    def "Test translate alarm when alarm map is null"() {

        when: "translateAlarm is called with null alarm map"
            transformManager.translateAlarm(null)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Received request to transform alarm with no properties")
    }

    def "Test translate empty alarm records list"() {

        given: "an empty arrayList"
            final List<Map<String, Object>> alarmList = new ArrayList<>()

        when: "transformAlarmRecords is called with empty alarm records list"
            transformManager.transformAlarmRecords("", null)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Received request to transform null or empty alarm records")
    }

    def "Test translate null alarm records list"() {

        given: "a null arrayList"
            final List<Map<String, Object>> alarmList = null;

        when: "transformAlarmRecords is called with null alarm records"
            transformManager.transformAlarmRecords((String) null, null)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Received request to transform null or empty alarm records.")
    }

    @Unroll
    def "Test valid notification #notificationType gets transformed correctly when probableCause is an integer value"() {

        given: "a valid NotifyAlarm when probableCause is a valid integer value"
            def alarmProperties = NotifyAlarm.createNotifyAlarm(notificationType)
            alarmProperties.put('probableCause', 2)

        when: "the transform manager is called to translate alarm"
            final EventNotification event = transformManager.translateAlarm(alarmProperties)

        then: "a transformed alarm is returned"
            assertEventNotificationEquals(createExpectedTransformedEventNotification(notificationType), event)

        where: "notificationType to transform is"
            notificationType            | _
            "notifyClearedAlarm"        | _
            "notifyNewAlarm"            | _
            "notifyChangedAlarmGeneral" | _
            "notifyChangedAlarm"        | _
            "notifyAlarmListRebuilt"    | _
    }

    @Unroll
    def "Test valid notification #notificationType when probableCause is a string value which can be parsed into an integer"() {

        given: "a valid NotifyAlarm when probableCause is a valid string value that can be parsed into an integer"
            def alarmProperties = NotifyAlarm.createNotifyAlarm(notificationType)
            alarmProperties.put('probableCause', "2")

        when: "the transform manager is called to translate alarm"
            final EventNotification event = transformManager.translateAlarm(alarmProperties)

        then: "a transformed alarm is returned"
            assertEventNotificationEquals(createExpectedTransformedEventNotification(notificationType), event)

        where: "notificationType to transform is"
            notificationType            | _
            "notifyClearedAlarm"        | _
            "notifyNewAlarm"            | _
            "notifyChangedAlarmGeneral" | _
            "notifyChangedAlarm"        | _
    }

    @Unroll
    def "Test valid notification #notificationType when probableCause is an unmapped integer value "() {

        given: "a valid NotifyAlarm when probableCause is an integer value that is not found in mapping"
            def alarmProperties = NotifyAlarm.createNotifyAlarm(notificationType)
            alarmProperties.put('probableCause', 30)

        when: "the transform manager is called to translate alarm"
            final EventNotification event = transformManager.translateAlarm(alarmProperties)

        then: "the event is translated"
            EventNotification eventNotification = createExpectedTransformedEventNotification(notificationType)

        and: "the probableCause remains as an unmapped integer"
            eventNotification.setProbableCause("30")
            assertEventNotificationEquals(eventNotification, event)

        where: "notificationType to transform is"
            notificationType            | _
            "notifyClearedAlarm"        | _
            "notifyNewAlarm"            | _
            "notifyChangedAlarmGeneral" | _
            "notifyChangedAlarm"        | _
    }

    @Unroll
    def "Test valid notification #notificationType when probableCause is an unmapped string value"() {

        given: "a valid NotifyAlarm when probableCause is a string value that is not found in mapping"
            def alarmProperties = NotifyAlarm.createNotifyAlarm(notificationType)
            alarmProperties.put('probableCause', "invalidProbableCause")

        when: "the transform manager is called to translate alarm"
            final EventNotification event = transformManager.translateAlarm(alarmProperties)

        then: "the event is translated"
            EventNotification eventNotification = createExpectedTransformedEventNotification(notificationType)

        and: "the probableCause remains as an unmapped string"
            eventNotification.setProbableCause("invalidProbableCause")
            assertEventNotificationEquals(eventNotification, event)

        where: "notificationType to transform is"
            notificationType            | _
            "notifyClearedAlarm"        | _
            "notifyNewAlarm"            | _
            "notifyChangedAlarmGeneral" | _
            "notifyChangedAlarm"        | _
    }

    @Unroll
    def "Test invalid notification #notificationType"() {

        given: "an invalid NotifyAlarm"
            def alarmProperties = NotifyAlarm.createNotifyAlarm(notificationType)

        when: "the transform manager is called to translate alarm"
            transformManager.translateAlarm(alarmProperties)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.getCause().message.contains(expectedMessage)

        and: "An error is recorded for NotifyAlarm(s) with invalid notificationType"
            1 * systemRecorder.recordError("Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: " + expectedMessage + "]", ErrorSeverity.WARNING, "ModelTransformer", "", "For alarm with the following ID: " + null)

        where: "notificationType to transform is"
            notificationType     | expectedMessage
            "notifyInvalidAlarm" | "notificationType is not valid - cannot transform alarm"
            123                  | "notificationType was not a string type - cannot transform alarm"
            null                 | "notificationType from alarm map was null - cannot transform alarm"
    }

    def "Test valid list of AlarmList.alarmRecords gets transformed correctly"() {

        given: "a valid list of alarmRecords"
            def alarmRecords = AlarmRecords.ALARM_RECORDS

        when: "the transform manager is called to translate the AlarmRecords"
            final List<EventNotification> event = transformManager.transformAlarmRecords(alarmRecords, AlarmRecords.ME_CONTEXT_FDN)

        then: "a list of transformed alarms are returned"
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(0), event.get(0))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(1), event.get(1))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(2), event.get(2))
    }

    def "Test list of AlarmList.alarmRecords with incorrectly formatted additionalInformation gets discarded"() {

        given: "a list of alarmRecords including one with incorrectly formatted additionalInformation"
            def alarmRecords = AlarmRecords.ALARM_RECORDS_INVALID_ADDITIONAL_INFORMATION

        when: "the transform manager is called to translate the AlarmRecords"
            final List<EventNotification> event = transformManager.transformAlarmRecords(alarmRecords, AlarmRecords.ME_CONTEXT_FDN)

        then: "a list of transformed alarms are returned"
            assert event.size() == 4
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(0), event.get(0))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(1), event.get(1))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(2), event.get(2))

        and: "the alarm with invalid additionalInformation is present but additionalInformation is not populated"
            assertEventNotificationEquals(createExpectedTransformedEventNotificationAlarmWithInvalidAdditionalInfo(), event.get(3))

    }

    def "Test translate alarm when alarm map is missing href"() {

        given: "an invalid alarm when href is not present"
            def alarmProperties = NotifyAlarm.createNotifyAlarm("notifyNewAlarm")
            alarmProperties.remove("href")

        when: "the transform manager is called"
            transformManager.translateAlarm(alarmProperties)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.getCause().message.contains("Alarm is missing mandatory 'href' field")

        and: "An error is recorded for an invalid alarm without href"
            1 * systemRecorder.recordError("Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: Alarm is missing mandatory 'href' field.]", ErrorSeverity.WARNING, "ModelTransformer", "", "For alarm with the following ID: 9cf9a4a0-5271-490d-87ce-3727d823f32c")

    }

    def "Test that other alarms are transformed when an alarm in the list throws an exception"() {

        given: "a list of alarms to be transformed including an invalid alarm"
            def alarmRecords = AlarmRecords.FAULTY_ALARM_RECORDS
            assert alarmRecords.size() == 4

        when: "the transform manager is called"
            def transformedAlarm = transformManager.transformAlarmRecords(alarmRecords, AlarmRecords.ME_CONTEXT_FDN)

        then: "the transform manager is successfully invoked for the valid translated alarms"
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(0), transformedAlarm.get(0))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(1), transformedAlarm.get(1))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(2), transformedAlarm.get(2))
            assert transformedAlarm.size() == 3

        and: "An error is recorded for the invalid alarm"
            1 * systemRecorder.recordError("Transformation failed due to: [com.ericsson.oss.mediation.fm.o1.transform.TransformerException: Invalid input value for date: [3] Date should in OpenAPI (rfc3339) format]", ErrorSeverity.WARNING, "ModelTransformer", "", "For the alarm with the following ID: " + 1,)

    }
}
