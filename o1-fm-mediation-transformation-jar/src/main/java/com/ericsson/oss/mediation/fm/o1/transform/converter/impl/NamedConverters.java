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

package com.ericsson.oss.mediation.fm.o1.transform.converter.impl;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Named;
import org.mapstruct.Condition;

import com.ericsson.oss.mediation.fm.o1.transform.TransformerException;
import com.ericsson.oss.mediation.fm.o1.transform.config.MediationConfigData;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.o1.util.HrefUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Contains custom converters that can be applied using the 'qualifiedByName' tag.
 * <p>
 * Example
 * <p>
 * 
 * @Mapping(source = "probableCause", target = "probableCause", qualifiedByName = "DefinedEnumModelConverter")
 */
@Slf4j
public class NamedConverters {

    /**
     * Converts the {@code href} alarm property value to a MO FDN.
     * <p>
     * Here is an example of how the converter should be used:
     * <p>
     * {@code
     * @Mapping(source = "href", target = "managedObjectInstance", qualifiedByName = "ManagedObjectInstanceConverter")
     * }
     * <p>
     * The converter would convert a href value such as the following:
     * <p>
     * "https://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1"
     * <p>
     * to:
     * <p>
     * "SubNetwork=skylight,MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
     */
    @Named("ManagedObjectInstanceConverter")
    public String managedObjectInstanceConverter(final String hrefValue) {
        if (StringUtils.isBlank(hrefValue)) {
            throw new IllegalArgumentException("Alarm is missing mandatory 'href' field.");
        }
        final String dnPrefix = HrefUtil.extractDnPrefix(hrefValue);
        final String ldn = HrefUtil.extractLdn(hrefValue);

        return FdnUtil.createFdn(dnPrefix, ldn);
    }

    /**
     * A converter implementation method that converts a date-time string in the OpenAPI (rfc3339) format to a string that matches the format argument
     * provided
     * to the converter.
     * <p>
     * Here is an example of how the converter should be used:
     *
     * <pre>
     * @Mapping(source = "eventTime", target = "eventTime", qualifiedByName = "O1DateModelConverter")
     * </pre>
     * 
     * For example, if used with format string above, '2023-09-06T11:32:01.743Z' would be converted to '20230906113201.743'.
     */
    @Named("O1DateModelConverter")
    public String o1DateModelConverter(final String date) {

        log.debug("O1DateModelConverter {} ", date);
        if (date == null) {
            return null;
        }
        final String formatValue = "yyyyMMddHHmmss.SSS";

        final String cleanedValue = removeNonDigitCharacters(date);

        // Extract date and time components
        final int year = Integer.parseInt(cleanedValue.substring(0, 4));
        final int month = Integer.parseInt(cleanedValue.substring(4, 6));
        final int day = Integer.parseInt(cleanedValue.substring(6, 8));
        final int hour = Integer.parseInt(cleanedValue.substring(8, 10));
        final int minute = Integer.parseInt(cleanedValue.substring(10, 12));
        final int second = Integer.parseInt(cleanedValue.substring(12, 14));

        final Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, minute, second);
        c.set(Calendar.MILLISECOND, extractMilliseconds(cleanedValue));

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatValue);
        final String convertedValue = simpleDateFormat.format(c.getTime());

        log.debug("Converted {} to {}", date, convertedValue);
        return convertedValue;
    }

    /**
     * Converter is invoked by an alarm that contains the 'eventTime' field.
     * <p>
     * If the eventTime contains the UTC offset it shall be used also to populate the EventNotification::timeZone
     * <p>
     *
     * <pre>
     * @Mapping(source = "eventTime", target = "timeZone", qualifiedByName = "TimeZoneModelProcessor")
     * </pre>
     */
    @Named("TimeZoneModelProcessor")
    public String timeZoneModelProcessor(final String eventTime) {
        log.debug("TimeZoneModelProcessor {}", eventTime);
        if (eventTime == null) {
            return eventTime;
        }
        try {
            if (isUTCOffsetPresent(eventTime)) {
                final Calendar calendar = DatatypeConverter.parseDateTime(eventTime);
                final TimeZone timeZone = calendar.getTimeZone();

                return timeZone.getDisplayName();
            }
        } catch (final Exception ex) {
            log.error("Exception occurred while parsing the event for timeZone data. Exception:{}, eventTime Value:{}", ex.getMessage(), eventTime);
            return null;
        }
        return eventTime;
    }

    /**
     * A converter implementation method that converts an enum defined in a yaml file into a String. This converter can be used in the following way:
     * <p>
     *
     * <pre>
     *  probableCause:
     *    '0': m3100Indeterminate
     *    '1': m3100AlarmIndicationSignal
     *    '2': m3100CallSetupFailure
     *    '3': m3100DegradedSignal
     * .....................
     * @Mapping(source = "probableCause", target = "probableCause", qualifiedByName = "DefinedEnumModelConverter")
     * </pre>
     * 
     * The probableCause value is determined by the integer value received from network equipment (ordinal) or can be received as string value
     * If the mapping is not present, the value received will be set as the probable cause.
     */
    @Named("DefinedEnumModelConverter")
    public String definedEnumModelConverter(final Object value) {
        if (value == null) {
            log.warn("Probable cause type is not valid");
            return null;
        }
        return getProbableCause(value.toString());
    }

    /**
     * Checks if the alarm contains an alarmId using the 'conditionQualifiedByName' tag
     * 
     * <pre>
     * alarmId is mandatory for the following notification types:
     *  'notifyNewAlarm'
     *  'notifyChangedAlarm'
     *  'notifyChangedAlarmGeneral'
     *  'notifyClearedAlarm'
     * </pre>
     *
     * @Mapping(source = "alarmId", target = "eventAgentId", conditionQualifiedByName = "AlarmIdProcessor")
     */
    @Condition
    @Named("AlarmIdProcessor")
    public boolean alarmIdProcessor(final Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            throw new TransformerException("Missing mandatory field alarmId");
        }
        return true;
    }

    private String removeNonDigitCharacters(final String value) {
        final String cleanedValue = value.replaceAll("[^0-9]", "");

        // Ensure that the cleaned string has at least 14 characters (YYYYMMDDHHMMSS)
        if (cleanedValue.length() < 14) {
            throw new TransformerException(
                    "Invalid input value for date: [" + value + "] Date should in OpenAPI (rfc3339) format");
        }
        return cleanedValue;
    }

    private int extractMilliseconds(final String value) {
        if (value.length() <= 14) {
            return 0; // Return 0 if there are no milliseconds.
        }
        // return integers from position 14 to a maximum of the first 3 characters beyond that position
        return Integer.parseInt(value.substring(14, Math.min(value.length(), 17)));
    }

    private boolean isUTCOffsetPresent(final String eventTime) {
        final OffsetDateTime offsetDateTime = OffsetDateTime.parse(eventTime);
        final ZoneOffset zoneOffset = offsetDateTime.getOffset();
        return !zoneOffset.toString().isEmpty();
    }

    /**
     * Checks the MediationConfigData to see if the value exists in the mapping. If present, the mapped value is returned.
     * If not present - the value is returned as is.
     */
    private String getProbableCause(final String value) {
        if (StringUtils.isNumeric(value) && MediationConfigData.getProbableCause().containsKey(Integer.parseInt(value))) {
            return MediationConfigData.getProbableCause().get(Integer.parseInt(value));
        }
        log.warn("The probable cause value for {} is not found in existing mapping", value);
        return value;
    }
}
