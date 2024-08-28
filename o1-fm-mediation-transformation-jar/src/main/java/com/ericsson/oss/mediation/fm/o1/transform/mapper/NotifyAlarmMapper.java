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
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.ericsson.oss.mediation.fm.o1.transform.TransformerException;
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.AdditionalAttributesConverter;
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.DefaultConverters;
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.NamedConverters;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Responsible for mapping data for the following O1 notification types:
 * <ul>
 * <li>notifyNewAlarm</li>
 * <li>notifyChangedAlarm</li>
 * <li>notifyClearedAlarm</li>
 * <li>notifyChangedAlarmGeneral</li>
 * </ul>
 * to an {@code EventNotification}.
 **/

@Mapper(componentModel = "cdi", uses = { NamedConverters.class, DefaultConverters.class })
public abstract class NotifyAlarmMapper implements CommonAlarmMapper {

    private static final String HREF = "href";

    @Inject
    private AdditionalAttributesConverter additionalAttributesConverter;

    @BeforeMapping
    void beforeMapping(final Map<String, Object> alarm) {
        if (null == alarm.get(HREF)) {
            throw new TransformerException("Alarm is missing mandatory 'href' field.");
        }
    }

    @AfterMapping
    void afterMapping(@MappingTarget final EventNotification event, final Map<String, Object> alarm) {
        additionalAttributesConverter.addAdditionalAttributes(event, alarm, this.getClass());
    }

}
