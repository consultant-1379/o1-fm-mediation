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

import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED;
import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.NETWORK_ELEMENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.fm.o1.common.event.ModeledEventSender;
import com.ericsson.oss.mediation.fm.o1.nodesuspender.cluster.Master;
import com.ericsson.oss.mediation.fm.util.EventNotificationUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Class responsible to initialize the node suspender cache.
 * Handles CRUD operations on cache, node suspender timer expiry operations
 */
@ApplicationScoped
@Slf4j
public class O1NodeSuspenderCacheManager {

    public static final String NODESUSPENDER_CACHE_NAME = "O1NodeSuspenderCache";

    private Cache<String, NodeSuspensionEntry> nodeSuspenderCache;

    @Inject
    private CacheProviderBean cacheProviderBean;

    @Inject
    private NodeSuspenderConfigurationListener nodeSuspenderConfigurationListener;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ModeledEventSender modeledEventSender;

    @PostConstruct
    public synchronized void initializeCache() {
        try {
            nodeSuspenderCache = cacheProviderBean.createOrGetModeledCache(NODESUSPENDER_CACHE_NAME);
        } catch (final Exception e) {
            log.error("Failed to initialize {} due to {}", NODESUSPENDER_CACHE_NAME, e);
        }
        log.info("{} created", NODESUSPENDER_CACHE_NAME);
    }

    /**
     * Resets the count for all NEs.
     * This method is invoked when the interval timer expires.
     * This is annotated with @Master to make sure that only the master instance in a cluster resets the count.
     */
    @Master
    public void resetCountForAllNodes() {
        for (final String key : getAllKeys()) {
            final NodeSuspensionEntry value = nodeSuspenderCache.get(key);
            value.setCount(0);
            nodeSuspenderCache.put(key, value);
        }
        log.trace("Counter reset for all nodes in cache");
    }

    /**
     * Processes the NE Ids whose notification count is less than the ALARMRATE_NORMAL_THRESHOLD
     * This method is invoked when the interval timer expires.
     * This is annotated with @Master to make sure that only the master instance in a cluster executes this.
     * Otherwise, it may result in premature clearing of alarms, multiple alarms being sent.
     */
    @Master
    public void clearSuspendedNodes() {
        log.debug("Processing suspended nodes");
        List<String> suspendedNodeIds = getSuspendedNodeIds();
        resetNodeSuspensionStatus(suspendedNodeIds);
        clearNodeSuspendedAlarm(suspendedNodeIds);
    }

    public int getCacheSize() {
        return (int) StreamSupport.stream(this.getCache().spliterator(), false).count();
    }

    private void resetNodeSuspensionStatus(final List<String> networkElementIds) {
        log.debug("Resetting suspension status");
        for (final String networkElementId : networkElementIds) {
            final NodeSuspensionEntry value = nodeSuspenderCache.get(networkElementId);
            value.setSuspended(false);
            nodeSuspenderCache.put(networkElementId, value);
        }
    }

    private List<String> getSuspendedNodeIds() {
        int alarmRateThreshold = nodeSuspenderConfigurationListener.getAlarmRateNormalThreshold();
        log.debug("Alarm rate threshold is {} ", alarmRateThreshold);

        Map<String, Integer> suspendedNetworkElementIds = getSuspendedIdDetails();
        log.debug("NetworkElementIds that are suspended {}", suspendedNetworkElementIds);

        final List<String> networkElementIdsBelowThreshold = suspendedNetworkElementIds.entrySet().stream()
                .filter(entry -> entry.getValue() < alarmRateThreshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.debug("NetworkElementIds that are below the suspension threshold {}", networkElementIdsBelowThreshold);
        return networkElementIdsBelowThreshold;
    }

    private Map<String, Integer> getSuspendedIdDetails() {
        final Map<String, Integer> neAndValue = new HashMap<>();
        for (final Cache.Entry<String, NodeSuspensionEntry> entry : nodeSuspenderCache) {
            final NodeSuspensionEntry value = entry.getValue();
            if (value.isSuspended()) {
                neAndValue.put(entry.getKey(), value.getCount());
            }
        }
        return neAndValue;
    }

    private void clearNodeSuspendedAlarm(List<String> networkElementIds) {
        log.debug("Clearing node suspended alarm for network element Ids: {}", networkElementIds);

        for (String networkElementId : networkElementIds) {
            final String networkElementFdn = NETWORK_ELEMENT + "=" + networkElementId;
            log.debug("Clearing node suspended alarm for node: {}", networkElementFdn);

            final EventNotification eventNotification =
                    EventNotificationUtil.createNodeSuspensedClearAlarm(networkElementFdn, "", "");
            modeledEventSender.sendEventNotification(eventNotification);
            systemRecorder.recordEvent("FM_O1_ALARM_SERVICE", DETAILED, "", networkElementFdn,
                    "Clearing node suspended alarm for " + networkElementFdn);

        }
    }

    protected Cache<String, NodeSuspensionEntry> getCache() {
        if (nodeSuspenderCache == null) {
            log.warn(NODESUSPENDER_CACHE_NAME + " should have initialized post construct!");
            initializeCache();
        }
        return nodeSuspenderCache;
    }

    private List<String> getAllKeys() {
        final List<String> keys = new ArrayList<>();
        nodeSuspenderCache.forEach(entry -> keys.add(entry.getKey()));
        return keys;
    }
}
