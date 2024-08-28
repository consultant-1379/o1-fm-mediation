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

package com.ericsson.oss.mediation.fm.o1.common.event;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.fm.supervision.response.AlarmSupervisionResponse;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.services.fm.service.util.EventNotificationConverter;

/**
 * A modeled event sender class for sending different modeled events.
 * Note 1: This replaces using the EventBuffer from the fm-common-event-buffering-jar to send EventNotifications. That
 * library uses J2SE code and uses its own thread pooling mechanism which is not recommended in a JEE environment. Using
 * that library in this module and having dependencies to it in the different modules like the engine, heartbeat and handler
 * modules caused class loading problems during deployment.
 * Note 2: Sending a single modeled EventNotification is no longer supported as there is no longer a modeled
 * channel for FMMediationChannel deployed in the model repo. An EventNotificationBatch is the only
 * supported modeled event.
 */
public class ModeledEventSender {

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> mediationTaskRequestEventSender;

    @Inject
    @Modeled
    private EventSender<EventNotificationBatch> eventNotificationBatchEventSender;

    @Inject
    @Modeled
    private EventSender<AlarmSupervisionResponse> alarmSupervisionResponseEventSender;

    public void sendMediationTaskRequestEvent(final MediationTaskRequest mediationTaskRequest) {
        mediationTaskRequestEventSender.send(mediationTaskRequest);
    }

    public void sendEventNotification(final EventNotification eventNotification) {
        sendEventNotifications(Arrays.asList(eventNotification));
    }

    public void sendEventNotifications(final List<EventNotification> eventNotifications) {
        final EventNotificationBatch eventNotificationBatch = EventNotificationConverter.serializeObject(eventNotifications);
        eventNotificationBatchEventSender.send(eventNotificationBatch);
    }

    public void sendAlarmSupervisionResponseEvent(final AlarmSupervisionResponse alarmSupervisionResponse) {
        alarmSupervisionResponseEventSender.send(alarmSupervisionResponse);
    }
}
