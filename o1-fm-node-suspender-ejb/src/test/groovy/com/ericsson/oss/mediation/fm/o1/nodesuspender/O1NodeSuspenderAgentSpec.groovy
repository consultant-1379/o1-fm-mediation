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
package com.ericsson.oss.mediation.fm.o1.nodesuspender

import javax.inject.Inject

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean

class O1NodeSuspenderAgentSpec extends CdiSpecification {

    private static final String NETWORK_ELEMENT_FDN = "NetworkElement=NodeA"

    @ObjectUnderTest
    private O1NodeSuspenderAgent o1NodeSuspenderAgent

    @Inject
    private NodeSuspenderConfigurationListener nodeSuspenderConfigurationListener

    def nodeSuspensionCache = new InMemoryCache<String, NodeSuspensionEntry>('O1NodeSuspenderCache')

    @MockedImplementation
    private CacheProviderBean cacheProviderBean

    def setup() {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.addManagedObject()
                .type("MeContext")
                .name("5G141O1001")
                .build()
    }


    def "Test that a Network Element is added to node suspender cache"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        when: "the nodeSuspender agent register is called with an NE id"
            o1NodeSuspenderAgent.register("Node1")

        then: "the cache is successfully updated with the NE id"
            nodeSuspensionCache.size() == 1
            nodeSuspensionCache.get("Node1").getCount() == 0

    }

    def "Test that a Network Element is removed from node suspender cache"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains an entry"
            o1NodeSuspenderAgent.register("Node1")

        when: "the nodeSuspender agent unregister is called with an NE id"
            o1NodeSuspenderAgent.unregister("Node1")

        then: "the cache is successfully updated with entry removed"
            nodeSuspensionCache.size() == 0

    }

    def "Test that for a Network element node suspender counter cache is updated"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains an entry"
            o1NodeSuspenderAgent.register("Node1")


        when: "the nodeSuspender agent notifyNotificationReceived is called with an NE id"
            o1NodeSuspenderAgent.notifyNotificationReceived("Node1")

        then: "notification count for cache is increased for the node"
            nodeSuspensionCache.get("Node1").getCount() == 1
    }

    def "Test that for a Network element node suspender counter cache is incremented for every call"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains two entries"
            o1NodeSuspenderAgent.register("Node1")
            o1NodeSuspenderAgent.register("Node2")

        when: "the o1NodeSuspenderAgent notifyNotificationReceived is called with multiple NE ids"
            o1NodeSuspenderAgent.notifyNotificationReceived("Node1")
            o1NodeSuspenderAgent.notifyNotificationReceived("Node1")
            o1NodeSuspenderAgent.notifyNotificationReceived("Node1")
            o1NodeSuspenderAgent.notifyNotificationReceived("Node2")

        then: "the nodeSuspender agent is successfully invoked for the translated alarm"
            nodeSuspensionCache.get("Node1").getCount() == 3
            nodeSuspensionCache.get("Node2").getCount() == 1

    }

    def "Test resetting the node suspender cache"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "The alarm rate threshold is set to 2"
            nodeSuspenderConfigurationListener.alarmRateThreshold = 2

        and: "the cache is populated for two nodes"
            o1NodeSuspenderAgent.register("Node1")
            o1NodeSuspenderAgent.register("Node2")
            o1NodeSuspenderAgent.notifyNotificationReceived("Node1")
            o1NodeSuspenderAgent.notifyNotificationReceived("Node1")
            o1NodeSuspenderAgent.notifyNotificationReceived("Node1")
            o1NodeSuspenderAgent.notifyNotificationReceived("Node2")
            nodeSuspensionCache.get("Node1").getCount() == 3
            nodeSuspensionCache.get("Node2").getCount() == 1

        when: "the o1NodeSuspenderAgent is called"
            o1NodeSuspenderAgent.o1NodeSuspenderCacheManager.resetCountForAllNodes()

        then: "the cache entries are reset for all nodes"
            nodeSuspensionCache.get("Node1").getCount() == 0
            nodeSuspensionCache.get("Node2").getCount() == 0
            o1NodeSuspenderAgent.isNodeSuspended("Node1")
            !o1NodeSuspenderAgent.isNodeSuspended("Node2")
    }


    def "Test that the cache is updated correctly when alarm flow threshold exceeded"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains an entry"
            o1NodeSuspenderAgent.register("Node1")

        and: "alarmRateThreshold is set"
            nodeSuspenderConfigurationListener.alarmRateThreshold = 1

        and: "notification count is initially 0"
            nodeSuspensionCache.get("Node1").getCount() == 0

        when: "the nodeSuspender agent is called to update the cache"
            o1NodeSuspenderAgent.notifyNotificationReceived("Node1")

        then: "notification count in increased"
            nodeSuspensionCache.get("Node1").getCount() == 1

        and: "node suspended status is returned as true"
            o1NodeSuspenderAgent.isNodeSuspended("Node1")

    }

    def "Test that the cache is updated correctly when alarm flow threshold not exceeded"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains an entry"
            o1NodeSuspenderAgent.register("Node1")


        and: "alarmRateThreshold is set"
            nodeSuspenderConfigurationListener.alarmRateThreshold = 10

        and: "notification count is initially 0"
            nodeSuspensionCache.get("Node1").getCount() == 0

        when: "the nodeSuspender agent is called to update the cache"
            o1NodeSuspenderAgent.notifyNotificationReceived("Node1")

        then: "notification count in increased"
            nodeSuspensionCache.get("Node1").getCount() == 1

        and: "node suspended status is returned as false"
            !o1NodeSuspenderAgent.isNodeSuspended("Node1")

    }

    def "Test call to check alarm flow rate"() {
        when: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.alarmRateFlowControl = true

        then: "assert alarm flow rate check returns true"
            nodeSuspenderConfigurationListener.getAlarmRateFlowControl()
    }

}
