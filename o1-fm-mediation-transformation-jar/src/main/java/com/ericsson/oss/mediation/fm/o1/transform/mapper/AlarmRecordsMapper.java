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

import javax.inject.Inject;

import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.AdditionalAttributesConverter;
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.DefaultConverters;
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.NamedConverters;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Responsible for mapping alarm records to {@code EventNotification}s.
 * <p>
 * Alarm records are read from the nodes 'AlarmList.alarmRecords' attribute.
 **/

@Mapper(config = CommonAlarmMapper.class, componentModel = "cdi", uses = { NamedConverters.class, DefaultConverters.class })
public abstract class AlarmRecordsMapper implements CommonAlarmMapper {

    @Inject
    private AdditionalAttributesConverter additionalAttributesConverter;

    @Override
    @InheritConfiguration(name = "convertMapToEventNotification")
    @Mapping(source = "objectInstance", target = "managedObjectInstance")
    @Mapping(constant = "SYNCHRONIZATION_ALARM", target = "recordType")
    @Mapping(source = "alarmChangedTime",
            defaultExpression = "java(namedConverters.o1DateModelConverter((String)alarmRecord.get(\"alarmRaisedTime\")))", target = "eventTime",
            qualifiedByName = "O1DateModelConverter")
    public abstract EventNotification convertMapToEventNotification(Map<String, Object> alarmRecord);

    @AfterMapping
    void afterMapping(@MappingTarget final EventNotification event, final Map<String, Object> alarmRecord) {
        additionalAttributesConverter.addAdditionalAttributes(event, alarmRecord, getClass());
    }

}
