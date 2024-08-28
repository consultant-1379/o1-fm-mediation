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

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent;
import com.ericsson.oss.itpf.sdk.cluster.annotation.ServiceCluster;

import lombok.extern.slf4j.Slf4j;

/**
 * A class to observe the membership changes for the Node Suspender timer.
 * To ensure that in a multi-instace environment, only one instance is accountable for raising and clearing an alarm, O1NodeSuspender employs
 * master/slave capabilities.
 */
@ApplicationScoped
@Slf4j
public class ClusterMembershipObserver {

    private static final String NODE_SUSPENDER_TIMER_CLUSTER = "O1NodeSuspenderTimerCluster";
    private boolean master;
    private final Object lock = new Object();

    @PreDestroy
    public void shutdown() {
        synchronized (lock) {
            log.info("Shutting down, was running as {} of {}", master ? "master" : "slave", NODE_SUSPENDER_TIMER_CLUSTER);
            master = false;
        }
    }

    public boolean isMaster() {
        synchronized (lock) {
            log.debug(NODE_SUSPENDER_TIMER_CLUSTER + " Checking if instance is master {} ", master);
            return master;
        }
    }

    /**
     * Listens for cluster membership change events to determine if this instance is the master or slave.
     *
     * @param event
     *            membership change event
     */
    public void onMembershipChangeEvent(@Observes @ServiceCluster("O1NodeSuspenderTimerCluster") final MembershipChangeEvent event) {
        synchronized (lock) {
            if (event.isMaster() != master) {
                master = event.isMaster();
            }
            log.info("Now running as {} of {}", master ? "master" : "slave", NODE_SUSPENDER_TIMER_CLUSTER);
        }
    }

}
