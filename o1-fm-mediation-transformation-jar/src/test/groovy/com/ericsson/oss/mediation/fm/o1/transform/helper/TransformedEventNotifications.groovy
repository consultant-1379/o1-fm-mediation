package com.ericsson.oss.mediation.fm.o1.transform.helper

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.AdditionalAttributesConverter
import com.ericsson.oss.mediation.translator.model.EventNotification

import java.beans.BeanInfo
import java.beans.Introspector
import java.beans.PropertyDescriptor

import java.lang.reflect.Method

class TransformedEventNotifications {

    static void assertEventNotificationEquals(EventNotification en1, EventNotification en2) {
        assertNotNull(en1);
        assertNotNull(en2);
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(EventNotification.class)
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors()
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                Method readMethod = propertyDescriptor.getReadMethod();
                Object v1 = readMethod.invoke(en1)
                Object v2 = readMethod.invoke(en2)
                assertEquals(v1, v2)
            }
        } catch (Exception ex) {
            ex.printStackTrace()
            fail()
        }
    }

    static EventNotification createExpectedTransformedEventNotification(String notificationType) {
        AdditionalAttributesConverter additionalAttributesConverter = new AdditionalAttributesConverter()

        EventNotification en = new EventNotification()
        en.setRecordType("ALARM")
        en.setAcknowledged(false)
        en.addAdditionalAttribute("vesEventId", "fault000001");
        en.addAdditionalAttribute("vesEventName", "stndDefined_Vscf:Acs-Ericsson_ProcessingErrorAlarm-00001")
        en.addAdditionalAttribute("vesEventType", "stndDefined_Vscf:Acs-Ericsson_ProcessingErrorAlarm")
        en.addAdditionalAttribute("vesStartEpochMicrosec", "1698399774107999200")
        en.addAdditionalAttribute("vesLastEpochMicrosec", "1678271515731560000")
        en.addAdditionalAttribute("vesPriority", "Normal")
        en.addAdditionalAttribute("vesReportingEntityName", "ibcx0001vm002oam001")
        en.addAdditionalAttribute("vesSequence", "5")
        en.addAdditionalAttribute("vesSourceName", "scfx0001vm002cap001")
        en.addAdditionalAttribute("vesVersion", "4.1")
        en.addAdditionalAttribute("vesEventListenerVersion", "7.2")
        en.addAdditionalAttribute("notificationId", "1234567")
        en.addAdditionalAttribute("fdn", "NetworkElement=5G141O1001")
        en.setManagedObjectInstance("SubNetwork=top,SubNetwork=skylight,MeContext=5G141O1001")
        en.setEventTime("20230906113201.743")
        en.setTimeZone("GMT+00:00")
        en.addAdditionalAttribute("systemDN", "ManagedElement=1,MnsAgent=FM")
        en.setEventType("PROCESSING_ERROR_ALARM")
        en.setPerceivedSeverity("MAJOR")
        en.setProbableCause("m3100CallSetupFailure")

        if (notificationType.matches("notifyNewAlarm|notifyChangedAlarmGeneral|notifyChangedAlarm")) {
            en.setEventAgentId("9cf9a4a0-5271-490d-87ce-3727d823f32c")
            en.addAdditionalAttribute("generatedAlarmId", Integer.toString(additionalAttributesConverter.generateAlarmId("9cf9a4a0-5271-490d-87ce-3727d823f32c")))
            en.setSpecificProblem("some problem")
            en.addAdditionalAttribute("additionalKeyOne", "additionalValue1")
            en.addAdditionalAttribute("additionalKeyTwo", "additionalValue2")
        }

        if (notificationType.matches("notifyClearedAlarm")) {
            en.setEventAgentId("9cf9a4a0-5271-490d-87ce-3727d823f32c")
            en.addAdditionalAttribute("generatedAlarmId", Integer.toString(additionalAttributesConverter.generateAlarmId("9cf9a4a0-5271-490d-87ce-3727d823f32c")))
            en.setSpecificProblem("")
        }

        if (notificationType.matches("notifyAlarmListRebuilt")) {
            en.addAdditionalAttribute("reason", "System restarts")
            en.setRecordType("CLEARALL")
            en.setProbableCause("reinitialized")
            en.setEventType("PROCESSING ERROR")
            en.setPerceivedSeverity("WARNING")
            en.setSpecificProblem("NE and OSS alarms are not in sync")
        }
        return en
    }

    static List<EventNotification> createExpectedTransformedEventNotificationList() {
        AdditionalAttributesConverter additionalAttributesConverter = new AdditionalAttributesConverter()
        List<String> discriminatorList = ["discriminatorList1", "discriminatorList2", "discriminatorList3"] as List<String>

        List<EventNotification> enList = new ArrayList<>()
        EventNotification enOne = new EventNotification()
        enOne.setRecordType("SYNCHRONIZATION_ALARM")
        enOne.setAcknowledged(false)
        enOne.addAdditionalAttribute("generatedAlarmId", Integer.toString(additionalAttributesConverter.generateAlarmId("124578512")))
        enOne.addAdditionalAttribute("notificationId", "599801250")
        enOne.addAdditionalAttribute("fdn", "NetworkElement=5G141O1001")
        enOne.addAdditionalAttribute("PLMN", "262-80")
        enOne.addAdditionalAttribute("UserPlaneDomain", "defaultUPD")
        enOne.setManagedObjectInstance("SubNetwork=5G141O1001,MeContext=5G141O1001,ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3")
        enOne.setEventTime("20231020070125.020")
        enOne.setEventAgentId("124578512")
        enOne.setEventType("COMMUNICATIONS_ALARM")
        enOne.setPerceivedSeverity("MINOR")
        enOne.setProbableCause("m3100Unavailable")
        enOne.setSpecificProblem("GNBCUCP Service Unavailable")
        enOne.setDiscriminatorList(discriminatorList)
        enList.add(enOne)

        EventNotification enTwo = new EventNotification()
        enTwo.setRecordType("SYNCHRONIZATION_ALARM")
        enTwo.setAcknowledged(false)
        enTwo.addAdditionalAttribute("generatedAlarmId", Integer.toString(additionalAttributesConverter.generateAlarmId("dd03de9f-1fa3-4733-8688-9b2f23b6d13c")))
        enTwo.addAdditionalAttribute("notificationId", "599801250")
        enTwo.addAdditionalAttribute("fdn", "NetworkElement=5G141O1001")
        enTwo.addAdditionalAttribute("PLMN", "262-80")
        enTwo.addAdditionalAttribute("UserPlaneDomain", "defaultUPD")
        enTwo.setManagedObjectInstance("SubNetwork=5G141O1001,MeContext=5G141O1001,ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3")
        enTwo.setEventTime("20231020070126.020")
        enTwo.setEventAgentId("dd03de9f-1fa3-4733-8688-9b2f23b6d13c")
        enTwo.setEventType("COMMUNICATIONS_ALARM")
        enTwo.setPerceivedSeverity("WARNING")
        enTwo.setProbableCause("m3100CallSetupFailure")
        enTwo.setSpecificProblem("GNBCUUP Service Unavailable")
        enTwo.setDiscriminatorList(discriminatorList)
        enList.add(enTwo)

        EventNotification enThree = new EventNotification()
        enThree.setRecordType("SYNCHRONIZATION_ALARM")
        enThree.setAcknowledged(false)
        enThree.addAdditionalAttribute("generatedAlarmId", Integer.toString(additionalAttributesConverter.generateAlarmId("dd03de9f-1fa3-4733-8688-9b2f23b6d13d")))
        enThree.addAdditionalAttribute("notificationId", "599801250")
        enThree.addAdditionalAttribute("fdn", "NetworkElement=5G141O1001")
        enThree.addAdditionalAttribute("PLMN", "262-80")
        enThree.addAdditionalAttribute("UserPlaneDomain", "defaultUPD")
        enThree.setManagedObjectInstance("SubNetwork=5G141O1001,MeContext=5G141O1001,ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3")
        enThree.setEventTime("20231020070128.020")
        enThree.setEventAgentId("dd03de9f-1fa3-4733-8688-9b2f23b6d13d")
        enThree.setEventType("COMMUNICATIONS_ALARM")
        enThree.setPerceivedSeverity("MAJOR")
        enThree.setProbableCause("m3100AlarmIndicationSignal")
        enThree.setSpecificProblem("GNBDU Service Unavailable")
        enThree.setDiscriminatorList(discriminatorList)
        enList.add(enThree)

        return enList
    }

    static EventNotification createExpectedTransformedEventNotificationAlarmWithInvalidAdditionalInfo() {
        AdditionalAttributesConverter additionalAttributesConverter = new AdditionalAttributesConverter()

        EventNotification event = new EventNotification()
        event.setRecordType("SYNCHRONIZATION_ALARM")
        event.setAcknowledged(false)
        event.addAdditionalAttribute("generatedAlarmId", Integer.toString(additionalAttributesConverter.generateAlarmId("dd03de9f-1fa3-4733-8688-9b2f23b6d13e")))
        event.addAdditionalAttribute("notificationId", "599801250")
        event.addAdditionalAttribute("fdn", "NetworkElement=5G141O1001")
        event.setManagedObjectInstance("SubNetwork=5G141O1001,MeContext=5G141O1001,ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3")
        event.setEventTime("20231020070127.020")
        event.setEventAgentId("dd03de9f-1fa3-4733-8688-9b2f23b6d13e")
        event.setEventType("COMMUNICATIONS_ALARM")
        event.setPerceivedSeverity("MAJOR")
        event.setProbableCause("m3100AlarmIndicationSignal")
        event.setSpecificProblem("GNBDU Service Unavailable")
        return event
    }
}
