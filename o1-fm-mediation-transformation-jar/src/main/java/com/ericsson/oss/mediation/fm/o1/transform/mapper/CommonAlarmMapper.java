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

package com.ericsson.oss.mediation.fm.o1.transform.mapper;

import java.util.List;
import java.util.Map;

import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;

import com.ericsson.oss.mediation.fm.o1.transform.annotations.AdditionalAttribute;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Responsible for mapping data that is common to all O1 notification types to an {@code EventNotification}.
 * <p>
 * This should be extended by mappers for specific O1 Notification types.
 **/

@MapperConfig
public interface CommonAlarmMapper {

    List<EventNotification> convertAlarmListToEventNotificationList(List<Map<String, Object>> alarms);

    @AdditionalAttribute(source = "vesEventListenerVersion", target = "vesEventListenerVersion")
    @AdditionalAttribute(source = "version", target = "vesVersion")
    @AdditionalAttribute(source = "sourceName", target = "vesSourceName")
    @AdditionalAttribute(source = "sequence", target = "vesSequence")
    @AdditionalAttribute(source = "reportingEntityName", target = "vesReportingEntityName")
    @AdditionalAttribute(source = "priority", target = "vesPriority")
    @AdditionalAttribute(source = "lastEpochMicrosec", target = "vesLastEpochMicrosec")
    @AdditionalAttribute(source = "startEpochMicrosec", target = "vesStartEpochMicrosec")
    @AdditionalAttribute(source = "eventType", target = "vesEventType")
    @AdditionalAttribute(source = "eventName", target = "vesEventName")
    @AdditionalAttribute(source = "eventId", target = "vesEventId")
    @AdditionalAttribute(source = "systemDN", target = "systemDN")
    @AdditionalAttribute(source = "alarmId", target = "generatedAlarmId", qualifiedByName = "generateAlarmId")
    @AdditionalAttribute(source = "notificationId", target = "notificationId")
    @AdditionalAttribute(source = "href", target = "fdn", qualifiedByName = "getNetworkElementFdnFromHref")
    @AdditionalAttribute(source="correlatedNotifications", target="", qualifiedByName="correlatedNotificationsProcessor")
    @Mapping(source = "additionalInformation", target = "additionalAttributes")
    @Mapping(constant = "ALARM", target = "recordType")
    @Mapping(source = "specificProblem", target = "specificProblem")
    @Mapping(source = "perceivedSeverity", target = "perceivedSeverity")
    @Mapping(source = "probableCause", target = "probableCause", qualifiedByName = "DefinedEnumModelConverter")
    @Mapping(source = "alarmType", target = "eventType")
    @Mapping(source = "alarmId", target = "eventAgentId", conditionQualifiedByName = "AlarmIdProcessor")
    @Mapping(source = "eventTime", target = "timeZone", qualifiedByName = "TimeZoneModelProcessor")
    @Mapping(source = "eventTime", target = "eventTime", qualifiedByName = "O1DateModelConverter")
    @Mapping(source = "href", target = "managedObjectInstance", qualifiedByName = "ManagedObjectInstanceConverter")
    EventNotification convertMapToEventNotification(Map<String, Object> alarm);

}
