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

import java.util.Map;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ericsson.oss.mediation.fm.o1.transform.annotations.AdditionalAttribute;
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.DefaultConverters;
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.NamedConverters;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Responsible for mapping data for the O1 'notifyAlarmListRebuilt' notification type to an {@code EventNotification}.
 **/

@Mapper(componentModel = "cdi", config = CommonAlarmMapper.class, uses = { NamedConverters.class, DefaultConverters.class })
public abstract class NotifyAlarmListRebuiltMapper extends NotifyAlarmMapper {

    @Override
    @InheritConfiguration(name = "convertMapToEventNotification")
    @AdditionalAttribute(source = "reason", target = "reason")
    @Mapping(source = "alarmId", target = "eventAgentId", ignore = true)
    @Mapping(constant = "CLEARALL", target = "recordType")
    @Mapping(constant = "NE and OSS alarms are not in sync", target = "specificProblem")
    @Mapping(constant = "PROCESSING ERROR", target = "eventType")
    @Mapping(constant = "reinitialized", target = "probableCause")
    @Mapping(constant = "WARNING", target = "perceivedSeverity")
    public abstract EventNotification convertMapToEventNotification(Map<String, Object> alarm);
}
