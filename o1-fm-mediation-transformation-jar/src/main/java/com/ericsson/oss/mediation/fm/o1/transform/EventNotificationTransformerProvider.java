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

package com.ericsson.oss.mediation.fm.o1.transform;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.ericsson.oss.mediation.fm.o1.transform.mapper.CommonAlarmMapper;
import com.ericsson.oss.mediation.fm.o1.transform.mapper.NotifyAlarmListRebuiltMapper;
import com.ericsson.oss.mediation.fm.o1.transform.mapper.NotifyAlarmMapperImpl;

public class EventNotificationTransformerProvider {

    private static final String NOTIFY_ALARM_LIST_REBUILT = "notifyAlarmListRebuilt";
    private static final String NOTIFY_NEW_ALARM = "notifyNewAlarm";
    private static final String NOTIFY_CHANGED_ALARM = "notifyChangedAlarm";
    private static final String NOTIFY_CHANGED_ALARM_GENERAL = "notifyChangedAlarmGeneral";
    private static final String NOTIFY_CLEARED_ALARM = "notifyClearedAlarm";
    @Inject
    NotifyAlarmListRebuiltMapper notifyAlarmListRebuiltMapper;

    @Inject
    NotifyAlarmMapperImpl notifyAlarmMapper;

    private static final Set<String> SUPPORTED_NOTIFICATION_TYPES = new HashSet<>(Arrays.asList(
            NOTIFY_ALARM_LIST_REBUILT, NOTIFY_NEW_ALARM, NOTIFY_CHANGED_ALARM, NOTIFY_CHANGED_ALARM_GENERAL, NOTIFY_CLEARED_ALARM));

    public CommonAlarmMapper selectMapper(final Map<String, Object> alarm) {
        final String notification = getNotificationType(alarm);
        if (notification.equals(NOTIFY_ALARM_LIST_REBUILT)) {
            return notifyAlarmListRebuiltMapper;
        }
        return notifyAlarmMapper;

    }

    private String getNotificationType(final Map<String, Object> alarm) {
        final Object notificationType = alarm.get("notificationType");

        if (notificationType == null) {
            throw new TransformerException("notificationType from alarm map was null - cannot transform alarm");
        }

        if (!(notificationType instanceof String)) {
            throw new TransformerException("notificationType was not a string type - cannot transform alarm");
        }

        if (!SUPPORTED_NOTIFICATION_TYPES.contains(notificationType)) {
            throw new TransformerException("notificationType is not valid - cannot transform alarm");
        }

        return (String) notificationType;
    }

}
