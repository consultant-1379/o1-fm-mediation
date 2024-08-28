package com.ericsson.oss.mediation.fm.o1.handlers.dps


import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.DataBucket
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps

class DpsQuerySpec extends CdiSpecification {

    @ObjectUnderTest
    DpsQuery dpsquery

    def "Test getSupervisedListOfNodesFromDatabase"() {
        given:
            persistNodes()
        expect:
            assert dpsquery.getSupervisedListOfNodesFromDatabase()
     }

     private void persistNodes() {
        RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        DataBucket liveBucket = runtimeDps.build().getLiveBucket()
        liveBucket = runtimeDps.build().getLiveBucket()

        final Map<String, Object> moAttributes = new HashMap<String, Object>();
        moAttributes.put("networkElementId", "NetworkElement=O1Node");
        moAttributes.put("neType", "O1Node");
        moAttributes.put("platformType", "CPP");
        moAttributes.put("ossPrefix", "MeContext=O1Node");
        moAttributes.put("ossModelIdentity", "1294-439-662");

        final ManagedObject networkElement = liveBucket.getMibRootBuilder().namespace("OSS_NE_DEF").version("2.0.0").name("O1Node").type("NetworkElement").addAttributes(moAttributes).create()
        final Map<String, Object> targetAttributes = new HashMap<String, Object>()
        targetAttributes.put("category", "NODE")
        targetAttributes.put("type", "O1Node")
        targetAttributes.put("name", "NetworkElement=O1Node")
        targetAttributes.put("modelIdentity", "1294-439-662");
        final PersistenceObject targetPo = runtimeDps.addPersistenceObject().namespace("O1_MED").type("Target").addAttributes(targetAttributes).build();
        networkElement.setTarget(targetPo)
        final ManagedObject managedElement = liveBucket.getMibRootBuilder().namespace("OSS_NE_FM_DEF").type("FmFunction").parent(networkElement).addAttribute("currentServiceState", "IN_SERVICE").create()
        final Map<String, Object> supervisionAttributes = new HashMap<String, Object>()
        supervisionAttributes.put("active", true)
        supervisionAttributes.put("automaticSynchronization", true)
        final ManagedObject managedElement1 = liveBucket.getMibRootBuilder().namespace("OSS_NE_FM_DEF").name("1").type("FmAlarmSupervision").parent(networkElement).addAttributes(supervisionAttributes).create()

        final Map<String, Object> moAttributes1 = new HashMap<String, Object>();
        moAttributes1.put("networkElementId", "NetworkElement=O1Node1");
        moAttributes1.put("neType", "O1Node");
        moAttributes1.put("platformType", "CPP");
        moAttributes1.put("ossPrefix", "MeContext=O1Node");
        moAttributes1.put("ossModelIdentity", "1294-439-662");
        final ManagedObject networkElement1 = liveBucket.getMibRootBuilder().namespace("OSS_NE_DEF").version("2.0.0").name("O1Node1").type("NetworkElement").addAttributes(moAttributes1).create()
        final Map<String, Object> targetAttributes1 = new HashMap<String, Object>()
        targetAttributes1.put("category", "NODE")
        targetAttributes1.put("type", "O1Node")
        targetAttributes1.put("name", "NetworkElement=O1Node1")
        targetAttributes1.put("modelIdentity", "1294-439-662");
        final PersistenceObject targetPo1 = runtimeDps.addPersistenceObject().namespace("O1_MED").type("Target").addAttributes(targetAttributes1).build();
        networkElement1.setTarget(targetPo1)
        final ManagedObject managedElement2 = liveBucket.getMibRootBuilder().namespace("OSS_NE_FM_DEF").type("FmFunction").parent(networkElement1).addAttribute("currentServiceState", "IN_SERVICE").create()
        final Map<String, Object> supervisionAttributes1 = new HashMap<String, Object>()
        supervisionAttributes1.put("active", true)
        supervisionAttributes1.put("automaticSynchronization", true)
        final ManagedObject managedElement3 = liveBucket.getMibRootBuilder().namespace("OSS_NE_FM_DEF").name("1").type("FmAlarmSupervision").parent(networkElement1).addAttributes(supervisionAttributes1).create()

     }

   }