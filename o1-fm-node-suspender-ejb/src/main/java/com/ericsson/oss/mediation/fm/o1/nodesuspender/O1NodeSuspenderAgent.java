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

package com.ericsson.oss.mediation.fm.o1.nodesuspender;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class O1NodeSuspenderAgent {

    @Inject
    private O1NodeSuspenderCacheManager o1NodeSuspenderCacheManager;

    @Inject
    private NodeSuspenderConfigurationListener nodeSuspenderConfigurationListener;

    public void notifyNotificationReceived(final String networkElementId) {
        log.debug("Increment count for {}", networkElementId);

        final NodeSuspensionEntry entry = o1NodeSuspenderCacheManager.getCache().get(networkElementId);
        entry.setCount(entry.getCount() + 1);
        if (entry.getCount() >= nodeSuspenderConfigurationListener.getAlarmRateThreshold()) {
            entry.setSuspended(true);
        }
        o1NodeSuspenderCacheManager.getCache().put(networkElementId, entry);
        log.trace("Current count for the network element {} is {} ", networkElementId, o1NodeSuspenderCacheManager.getCache().get(networkElementId));

    }

    public void register(final String networkElementId) {
        log.debug("Add node to NodeSuspenderCache: {}", networkElementId);
        final NodeSuspensionEntry entry = new NodeSuspensionEntry(0, false);
        o1NodeSuspenderCacheManager.getCache().put(networkElementId, entry);
        log.debug("Added node {} to cache", networkElementId);
    }

    public void unregister(final String networkElementId) {
        log.debug("Remove node from NodeSuspenderCache: {}", networkElementId);
        if (o1NodeSuspenderCacheManager.getCache().containsKey(networkElementId)) {
            o1NodeSuspenderCacheManager.getCache().remove(networkElementId);
            log.debug("Removed node {} from cache", networkElementId);
        }
    }

    public boolean isNodeSuspended(final String networkElementId) {
        log.trace("Node suspended status: {} for network element : {}", o1NodeSuspenderCacheManager.getCache().get(networkElementId).isSuspended(),
                networkElementId);
        return o1NodeSuspenderCacheManager.getCache().get(networkElementId).isSuspended();
    }

    public boolean isRegistered(String networkElementId) {
        return o1NodeSuspenderCacheManager.getCache().containsKey(networkElementId);
    }

}
