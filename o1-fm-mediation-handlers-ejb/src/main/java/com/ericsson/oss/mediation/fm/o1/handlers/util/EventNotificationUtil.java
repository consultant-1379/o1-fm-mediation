/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.fm.o1.handlers.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.translator.model.Constants;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import lombok.experimental.UtilityClass;

/**
 * This class provide some utilities to create EventNotification for some specific ENM Alarms
 *
 */
@UtilityClass
public class EventNotificationUtil {

    private static final String ON_BEHALF_OF_VALUE = "ManagementSystem=ENM";
    private static final String ON_BEHALF_OF_TAG = "behalf";

    /**
     *
     * This method is for setting the timestamp in an Event notification
     *
     * @param eventNotification
     *            the eventNotification on which set the timestamp
     * @param timeStamp
     *            the timestamp of the Alarm
     * @return the eventNotification with timestamp configured
     */
    private static EventNotification setEventTimeAndTimeZone(final EventNotification eventNotification, final long timeStamp) {
        final Calendar calendar = Calendar.getInstance();
        final Date date = new Date();
        date.setTime(timeStamp);
        calendar.setTime(date);
        eventNotification.setTimeZone(calendar.getTimeZone().getID());
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        eventNotification.setEventTime(simpleDateFormat.format(date));
        return eventNotification;
    }

    /**
     * This method is for creating the EventNotification and setting the required attributes for the synchronization Alarms
     *
     * @param fdn
     *            an FDN of a Node MO
     *
     * @return an EventNotification for the Synchronization Alarms
     */
    private static EventNotification createBaseAlarm(final String fdn) {
        EventNotification eventNotification = new EventNotification();
        eventNotification.addAdditionalAttribute("fdn", FdnUtil.getNetworkElementFdn(fdn));
        eventNotification.addAdditionalAttribute(ON_BEHALF_OF_TAG, ON_BEHALF_OF_VALUE);
        eventNotification.setPerceivedSeverity(Constants.SEV_INDETERMINATE);
        eventNotification.setManagedObjectInstance(fdn);
        setEventTimeAndTimeZone(eventNotification, new Date().getTime());
        return eventNotification;
    }

    /**
     * This method is for creating of EventNotification for the Synchronization Alarms
     *
     * @param fdn
     *            an FDN of a Node MO
     * @param recordType
     *            SYNCHRONIZATION_STARTED <br>
     *            SYNCHRONIZATION_ENDED <br>
     *            SYNCHRONIZATION_ABORTED <br>
     * @return an EventNotification for the Synchronization Alarms
     */
    public static EventNotification createSyncAlarm(final String fdn, final String recordType) {
        final EventNotification eventNotification = createBaseAlarm(fdn);
        eventNotification.setRecordType(recordType);
        return eventNotification;
    }
}
