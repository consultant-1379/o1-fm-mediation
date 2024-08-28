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

package com.ericsson.oss.mediation.fm.o1.nodesuspender;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Startup
@Slf4j
public class NodeSuspenderTimer {

    private static final boolean PERSISTENT_TIMER = true;
    private static final long INITIAL_DURATION_MSECS = 1000L;

    @Inject
    private TimerService timerService;

    @Inject
    private NodeSuspenderConfigurationListener listener;

    @Inject
    private O1NodeSuspenderCacheManager o1NodeSuspenderCacheManager;

    @PostConstruct
    private void init() {
        long intervalInMinutes = listener.getAlarmRateCheckInterval();
        long intervalInMillis = intervalInMinutes * 60 * 1000L; // Convert minutes to milliseconds

        log.debug("Node suspender timer starting in post construct with interval {}", intervalInMinutes);

        timerService.createIntervalTimer(INITIAL_DURATION_MSECS, intervalInMillis,
                new TimerConfig("Timer for node suspender cache", PERSISTENT_TIMER));
    }

    @Timeout
    public void timeout(final Timer timer) {
        log.debug("Node suspender timer interval has elapsed, resetting the cache");

        o1NodeSuspenderCacheManager.clearSuspendedNodes();
        o1NodeSuspenderCacheManager.resetCountForAllNodes();

        log.debug("Node suspender timer time-out operations done");
    }
}
