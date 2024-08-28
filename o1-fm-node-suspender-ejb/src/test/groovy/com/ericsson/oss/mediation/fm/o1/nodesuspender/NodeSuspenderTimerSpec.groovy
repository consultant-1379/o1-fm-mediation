package com.ericsson.oss.mediation.fm.o1.nodesuspender

import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED

import javax.ejb.Timer
import javax.ejb.TimerService
import javax.inject.Inject

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.fm.o1.common.event.ModeledEventSender
import com.ericsson.oss.mediation.translator.model.EventNotification

class NodeSuspenderTimerSpec extends CdiSpecification {

    private static final String NETWORK_ELEMENT_FDN_NODEA = "NetworkElement=NodeA"

    @ObjectUnderTest
    private NodeSuspenderTimer nodeSuspenderTimer

    @SpyImplementation
    private NodeSuspenderConfigurationListenerStub nodeSuspenderConfigurationListener

    @MockedImplementation
    private TimerService mockTimerService

    @MockedImplementation
    private ModeledEventSender modeledEventSender

    @Inject
    private O1NodeSuspenderCacheManager o1NodeSuspenderCache

    @Inject
    private SystemRecorder systemRecorder

    @MockedImplementation
    private CacheProviderBean cacheProviderBean

    def nodeSuspensionCache = new InMemoryCache<String, NodeSuspensionEntry>('O1NodeSuspenderCache')

    def setup() {
        setUpCache()
    }


    def "Test timer is created when init method is called"() {

        when: "init method was called"
            nodeSuspenderTimer.init()

        then: "a timer was created"
            1 * mockTimerService.createIntervalTimer(_, _, _)
    }


    def "Test when timeout method is invoked, NE with count less than threshold has suspension revoked"() {

        given: "The cache contains a suspended node with count less than the threshold"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache
            nodeSuspenderTimer.listener = nodeSuspenderConfigurationListener

        and: "NodeA is in wanted condition"
            nodeSuspensionCache.get("NodeA").isSuspended()
            nodeSuspensionCache.get("NodeA").getCount() == 49

        when: "timeout method was called"
            o1NodeSuspenderCache.initializeCache()
            nodeSuspenderTimer.timeout(_ as Timer)

        then: "Cache is reset"
            nodeSuspensionCache.get("NodeA").getCount() == 0

        and: "node suspension status is revoked"
            !nodeSuspensionCache.get("NodeA").isSuspended()

        and: "Node suspended alarm is cleared"
            1 * modeledEventSender.sendEventNotification({ it.toString().contains("perceivedSeverity=CLEARED") })
            1 * systemRecorder.recordEvent('FM_O1_ALARM_SERVICE', DETAILED,
                    '', 'NetworkElement=NodeA', 'Clearing node suspended alarm for NetworkElement=NodeA');

    }


    def "Test when timeout method is invoked, NE with count greater than threshold stays suspended"() {

        given: "The cache contains a suspended node with count greater than threshold"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache
            nodeSuspenderTimer.listener = nodeSuspenderConfigurationListener

        and: "NodeB is in wanted condition"
            nodeSuspensionCache.get("NodeB").isSuspended()
            nodeSuspensionCache.get("NodeB").getCount() == 51

        when: "timeout method was called"
            o1NodeSuspenderCache.initializeCache()
            nodeSuspenderTimer.timeout(_ as Timer)

        then: "Cache is reset"
            nodeSuspensionCache.get("NodeB").getCount() == 0

        and: "node stays suspended"
            nodeSuspensionCache.get("NodeB").isSuspended()

        and: "Alarm is not cleared"
            0 * modeledEventSender.sendEventNotification({ it.toString().contains("managedObjectInstance=NetworkElement=NodeB") })
            0 * systemRecorder.recordEvent('FM_O1_ALARM_SERVICE', DETAILED,
                    '', 'NetworkElement=NodeB', 'Node suspended alarm for NetworkElement=NodeB is not cleared');

    }


    def "Test when timeout method is invoked, suspended NE's are processed with automatic sync set to false"() {

        given: "The cache contains a suspended nodes"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache
            nodeSuspenderTimer.listener = nodeSuspenderConfigurationListener

        and: "NodeA is in wanted condition"
            nodeSuspensionCache.get("NodeA").isSuspended()
            nodeSuspensionCache.get("NodeA").getCount() == 49

        when: "timeout method was called"
            o1NodeSuspenderCache.initializeCache()
            nodeSuspenderTimer.timeout(_ as Timer)

        then: "Cache is reset"
            nodeSuspensionCache.get("NodeA").getCount() == 0

        and: "node suspension status is revoked"
            !nodeSuspensionCache.get("NodeA").isSuspended()

        and: "Alarm is cleared"
            1 * modeledEventSender.sendEventNotification(_ as EventNotification)

    }

    def "Test timeout invocation when NE has suspension revoked and alarm sync request fails"() {

        given: "The cache contains a suspended node with count less than the threshold"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache
            nodeSuspenderTimer.listener = nodeSuspenderConfigurationListener

        when: "timeout method was called"
            o1NodeSuspenderCache.initializeCache()
            nodeSuspenderTimer.timeout(_ as Timer)

        then: "Node suspended alarm is cleared"
            1 * modeledEventSender.sendEventNotification(_ as EventNotification)

    }


    private setUpCache() {
        nodeSuspensionCache.put("NodeA", new NodeSuspensionEntry(49, true))
        nodeSuspensionCache.put("NodeB", new NodeSuspensionEntry(51, true))
    }
}

class NodeSuspenderConfigurationListenerStub extends NodeSuspenderConfigurationListener {

    private int alarmRateNormalThreshold = 50;

    int getAlarmRateNormalThreshold() {
        return alarmRateNormalThreshold
    }

    private Boolean alarmRateFlowControl = true;

    Boolean getAlarmRateFlowControl() {
        return alarmRateFlowControl
    }
}
