package com.ericsson.oss.mediation.fm.o1.nodesuspender.cluster

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent
import com.ericsson.oss.mediation.fm.o1.nodesuspender.ClusterMembershipObserver

class ClusterMembershipObserverSpec extends CdiSpecification {

    @ObjectUnderTest
    private ClusterMembershipObserver cluster

    def "Test that the cluster is not master after membership change event"() {
        setup: "Event indicating member is not master"
            def event = Stub(MembershipChangeEvent) {
                isMaster() >> false
            }

        when: "The event is received by the observer"
            cluster.onMembershipChangeEvent(event)

        then: "The member is not master"
            !cluster.isMaster()

    }

    def "Test cluster membership event listener with different events"() {
        given: "Membership change events for member"
            def eventMaster1 = Stub(MembershipChangeEvent) {
                isMaster() >> true
            }
            def eventStand_by = Stub(MembershipChangeEvent) {
                isMaster() >> false
            }

        when: "The event indicating member as a master is received"
            cluster.onMembershipChangeEvent(eventMaster1)

        then: "The member is master"
            cluster.isMaster()

        when: "The member is a master, receives event to become stand-by"
            cluster.onMembershipChangeEvent(eventStand_by)

        then: "The member is a stand-by"
            !cluster.isMaster()

        when: "The member is stand-by, receives event to become master"
            cluster.onMembershipChangeEvent(eventMaster1)

        then: "The member is a master"
            cluster.isMaster()

    }

    def "Test cluster is not master when shutting down"() {
        given: "The cluster member is master"
            cluster.master = true

        when: "The member is shut-down"
            cluster.shutdown()

        then: "The member is no longer a master"
            !cluster.isMaster()

    }

}
