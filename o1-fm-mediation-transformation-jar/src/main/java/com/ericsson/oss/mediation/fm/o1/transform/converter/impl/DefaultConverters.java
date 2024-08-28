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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.PredicateUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Contains simple converters that will be applied to matching types by default.
 * <p>
 * Example
 * <p>
 * {@code String objectToString(final Object value) }
 * <p>
 * Would be applied to the following source if it were an Object type
 * <p>
 *
 * @Mapping(source = "alarmId", target = "eventAgentId")
 */
@Slf4j
public class DefaultConverters {
    /**
     * Responsible for converting and validating additionalInformation from JSON or MAP to the required format (String, String).
     * <p>
     * Example
     * <p>
     * JSON from alarmRecords:{"PLMN": "262-80", "UserPlaneDomain": "defaultUPD"}
     * MAP object from Notifications:{additionalKeyOne=additionalValue1, additionalKeyTwo=additionalValue2}
     * <p>
     */
    @SuppressWarnings("squid:S1166")
    public Map<String, String> additionalInfoProcessor(Object additionalInformation) {
        final Map<String, String> additionalInfoMap = new LinkedHashMap<>();

        final ObjectMapper mapper = new ObjectMapper();
        try {
            if (!(additionalInformation instanceof Map)) {
                additionalInformation = mapper.readValue(String.valueOf(additionalInformation), Map.class);
            }
            MapUtils.predicatedMap((Map) additionalInformation, PredicateUtils.instanceofPredicate(String.class),
                    PredicateUtils.instanceofPredicate(String.class));
            additionalInfoMap.putAll((Map<String, String>) additionalInformation);

        } catch (final Exception e) {
            log.warn("Discarding Additional Information as it does not conform to expected format of Map<String, String>", e);
        }
        return additionalInfoMap;
    }

    public String objectToString(final Object value) {
        return null != value ? String.valueOf(value) : null;
    }

    public List<String> objectToList(final Object value) {
        return null != value ? (List<String>) value : Collections.emptyList();
    }
}
