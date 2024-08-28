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

import com.ericsson.oss.mediation.fm.o1.transform.TransformerException;
import com.ericsson.oss.mediation.fm.o1.transform.annotations.AdditionalAttribute;
import com.ericsson.oss.mediation.fm.o1.transform.annotations.AdditionalAttributeMappings;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.o1.util.HrefUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * AdditionalAttributesConverter is used to read annotation AdditionalAttributeMappings and AdditionalAttribute,
 * and process its value and set it into Eventnotification additionalAttribute.
 */
@Slf4j
public class AdditionalAttributesConverter {

    public void addAdditionalAttributes(final EventNotification event, final Map<String, Object> alarmMap, final Class<?> implClass) {
        event.setAdditionalAttributes(new LinkedHashMap<>(event.getAdditionalAttributes()));
        final List<Class<?>> interfaces = ClassUtils.getAllInterfaces(implClass);
        interfaces.addAll(ClassUtils.getAllSuperclasses(implClass));
        for (final Class<?> interfaze : interfaces) {
            for (final Method method : interfaze.getDeclaredMethods()) {
                if (method.isAnnotationPresent(AdditionalAttributeMappings.class)) {

                    final AdditionalAttributeMappings additionalAttributeMappings = method.getAnnotation(AdditionalAttributeMappings.class);
                    final AdditionalAttribute[] additionalAttributeList = additionalAttributeMappings.value();
                    addAdditionalAttributesToEventNotification(event, additionalAttributeList, alarmMap);
                }
            }
        }
    }

    private void addAdditionalAttributesToEventNotification(final EventNotification event, final AdditionalAttribute[] additionalAttributeList, final Map<String, Object> alarmMap) {
        for (final AdditionalAttribute additionalAttribute : additionalAttributeList) {
            final Object additionalAttributeValue = getAdditionalAttributeValue(additionalAttribute, alarmMap);

            if (null != additionalAttributeValue) {
                if (additionalAttributeValue instanceof Map) {
                    for (Entry entry : ((Map<?, ?>) additionalAttributeValue).entrySet()) {
                        event.addAdditionalAttribute(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                    }
                } else {
                    event.addAdditionalAttribute(additionalAttribute.target(), String.valueOf(additionalAttributeValue));
                }
            }
        }
    }

    private Object getAdditionalAttributeValue(final AdditionalAttribute additionalAttribute, final Map<String, Object> alarmMap) {
        Object additionalAttributeValue = null;
        if (!StringUtils.isEmpty(additionalAttribute.qualifiedByName())) {
            try {
                final Method converterMethod = getClass().getDeclaredMethod(additionalAttribute.qualifiedByName(), Object.class);
                additionalAttributeValue = converterMethod.invoke(this, alarmMap.get(additionalAttribute.source()));

            } catch (final Exception e) {
                throw new TransformerException("Exception occured while converting Additional attributes " + e.getMessage(), e);
            }
        } else {
            additionalAttributeValue = alarmMap.get(additionalAttribute.source());
        }
        return additionalAttributeValue;

    }

    protected Integer generateAlarmId(final Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        log.debug("Generating alarm ID for input: {}", value);
        final int generatedAlarmId = value.hashCode() & (Integer.MAX_VALUE >> 1);
        log.debug("Generated alarm ID is: {} for input {}", generatedAlarmId, value);
        return generatedAlarmId;
    }

    protected String getNetworkElementFdnFromHref(final Object hrefValue) {
        if (null == hrefValue) {
            return null;
        }
        final String dnPrefix = HrefUtil.extractDnPrefix((String) hrefValue);
        return FdnUtil.getNetworkElementFdn(dnPrefix);
    }

    @SuppressWarnings("squid:S1166")
    public Map<String, String> correlatedNotificationsProcessor(Object correlatedNotifications) {
        if (null == correlatedNotifications) {
            return Collections.emptyMap();
        }

        Map<String, String> correlatedNotificationsMap = new LinkedHashMap<>();

        int correlatedNotificationPosition = 1;
        int notificationIdPosition = 1;

        for (Map correlatedNotification : (List<Map>) correlatedNotifications) {

            String sourceObjectInstanceValue = (String) correlatedNotification.get("sourceObjectInstance");
            List notificationIds = (List) correlatedNotification.get("notificationId");

            if (isNotBlank(sourceObjectInstanceValue) && isNotEmpty(notificationIds)) {
                correlatedNotificationsMap.put("correlatedNotifications_" + correlatedNotificationPosition + "_sourceObjectInstance",
                        sourceObjectInstanceValue);

                for (Object id : notificationIds) {
                    correlatedNotificationsMap.put("correlatedNotifications_" + correlatedNotificationPosition + "_Ids_" + notificationIdPosition,
                            id.toString());
                    ++notificationIdPosition;
                }
                notificationIdPosition = 1;
                ++correlatedNotificationPosition;
            }
        }

        return correlatedNotificationsMap;
    }
}
