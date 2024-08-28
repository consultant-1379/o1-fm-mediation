/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.o1.fm.heartbeat.service;

import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.fm.util.EventNotificationUtil;
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgent;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.services.fm.service.util.EventNotificationConverter;

import lombok.extern.slf4j.Slf4j;

/**
 * During PostConstruct, this class creates an interval timer that is set to run after a predetermined amount of time. It then reads all the data from
 * O1HeartbeatCache and determines whether any heartbeat has expired for a given networkElementFdn and raises a heartbeat alarm if it expired.
 */
@Singleton
@Startup
@Slf4j
public class O1FmHeartbeatIntervalTimer {

    private static final String O1_NODE = "O1Node";
    private static final int INTERVAL_DURATION_SECS = 20;
    private static final int INTERVAL_DURATION_MSECS = INTERVAL_DURATION_SECS * 1000;

    @Inject
    SystemRecorder systemRecorder;

    @Inject
    @Modeled
    EventSender<EventNotificationBatch> eventSender;

    @EServiceRef
    O1HeartbeatAgent o1HeartbeatAgent;

    @Inject
    TimerService timerService;

    @PostConstruct
    public void init() {
        log.info("Start timer for O1HeartBeat with interval {} ", INTERVAL_DURATION_SECS);
        timerService.createIntervalTimer(INTERVAL_DURATION_MSECS, INTERVAL_DURATION_MSECS, new TimerConfig("O1HeartBeat", false));
    }

    /**
     * timeout method will be called whenever interval timer expires
     */
    @Timeout
    public void timeout(final Timer timer) {
        log.debug("O1HeartBeat failure check started");
        actOnHeartbeatFailures();
        actOnHeartbeatRecoveries();
        log.debug("O1HeartBeat failure check is finished and will restart in {} seconds.", INTERVAL_DURATION_SECS);
    }

    private void actOnHeartbeatFailures() {
        for (String networkElementFdn : o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM)) {
            log.debug("Raising heartbeat alarm for node: {}", networkElementFdn);
            final EventNotification eventNotification =
                    EventNotificationUtil.createHeartbeatAlarm(networkElementFdn, "", O1_NODE, networkElementFdn);
            eventSender.send(EventNotificationConverter.serializeObject(Arrays.asList(eventNotification)));
            systemRecorder.recordEvent("FM_O1_ALARM_SERVICE", EventLevel.DETAILED, "", networkElementFdn,
                    "Heartbeat Failed : Alarm raised for " + networkElementFdn);
        }
    }

    private void actOnHeartbeatRecoveries() {
        for (String networkElementFdn : o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM)) {
            log.debug("Clearing heartbeat alarm for node: {}", networkElementFdn);
            final EventNotification eventNotification =
                    EventNotificationUtil.createHeartbeatClearAlarm(networkElementFdn, "", O1_NODE, networkElementFdn);
            eventSender.send(EventNotificationConverter.serializeObject(Arrays.asList(eventNotification)));
            systemRecorder.recordEvent("FM_O1_ALARM_SERVICE", EventLevel.DETAILED, "", networkElementFdn,
                    "Clearing heartbeat alarm for " + networkElementFdn);
        }
    }
}
