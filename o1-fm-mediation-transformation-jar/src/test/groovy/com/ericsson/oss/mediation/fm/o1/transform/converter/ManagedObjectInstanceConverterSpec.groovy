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
package com.ericsson.oss.mediation.fm.o1.transform.converter

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.NamedConverters

/**
 * Tests here are mainly for negative and alternate scenarios that are difficult or not worth testing e2e with the CDI test framework.
 * <p>
 * The main flow is tested from these test classes:
 * <pre>
 *  {@code com.ericsson.oss.mediation.fm.o1.transform.TransformManagerSpec}
 */
class ManagedObjectInstanceConverterSpec extends CdiSpecification {


    def "Test ManagedObjectInstanceConverter with null value"() {

        given: "ManagedObjectInstanceConverter is created"
            NamedConverters managedObjectInstanceConverter = new NamedConverters()

        when: "href value is null"
            managedObjectInstanceConverter.managedObjectInstanceConverter(null)

        then: "Exception is thrown"
            def e = thrown(IllegalArgumentException.class)
            assert e.message.contains("Alarm is missing mandatory 'href' field.")
    }

    def "Test ManagedObjectInstanceConverter with http"() {

        given: "ManagedObjectInstanceConverter is created"
            NamedConverters managedObjectInstanceConverter = new NamedConverters()

        when: "href value is present with http"
            String result = managedObjectInstanceConverter.managedObjectInstanceConverter("http://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1")

        then: "ManagedObjectInstance has been created from href"
            assert result == "SubNetwork=skylight,MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
    }

    def "Test ManagedObjectInstanceConverter with https"() {

        given: "ManagedObjectInstanceConverter is created"
             NamedConverters managedObjectInstanceConverter = new NamedConverters()

        when: "href value is present with https"
            String result = managedObjectInstanceConverter.managedObjectInstanceConverter("https://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1")

        then: "ManagedObjectInstance has been created from href"
            assert result == "SubNetwork=skylight,MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
    }

    def "Test ManagedObjectInstanceConverter when dnPrefix is just an MeContext"() {

         given: "ManagedObjectInstanceConverter is created"
            NamedConverters managedObjectInstanceConverter = new NamedConverters()

         when: "href value contains only dnPrefix"
            String result = managedObjectInstanceConverter.managedObjectInstanceConverter("http://cucp.MeContext/ManagedElement=1/GNBCUCPFunction=1")

         then: "ManagedObjectInstance has been created from href"
            assert result == "MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
    }

    def "Test ManagedObjectInstanceConverter when dnPrefix has multiple SubNetworks"() {

         given: "ManagedObjectInstanceConverter is created"
            NamedConverters managedObjectInstanceConverter = new NamedConverters()

         when: "href value contains multiple SubNetworks"
            String result = managedObjectInstanceConverter.managedObjectInstanceConverter("http://cucp.MeContext.athlone.SubNetwork.ireland.SubNetwork/ManagedElement=1/GNBCUCPFunction=1")

         then: "ManagedObjectInstance has been created from href"
            assert result == "SubNetwork=ireland,SubNetwork=athlone,MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
    }

    def "Test ManagedObjectInstanceConverter when href does not contain ldn"() {
 
         given: "ManagedObjectInstanceConverter is created"
            NamedConverters managedObjectInstanceConverter = new NamedConverters()

         when: "href value does not contain ldn"
            String result = managedObjectInstanceConverter.managedObjectInstanceConverter("http://cucp.MeContext.athlone.SubNetwork.ireland.SubNetwork")

         then: "ManagedObjectInstance has been created from href"
            assert result == "SubNetwork=ireland,SubNetwork=athlone,MeContext=cucp"
    }
}
