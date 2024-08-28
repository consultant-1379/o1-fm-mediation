package com.ericsson.oss.mediation.fm.o1.transform.converter

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.transform.TransformerException
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.NamedConverters

/**
 * Tests here are mainly for negative and alternate scenarios that are difficult or not worth testing e2e with the CDI test framework.
 * <p>
 * The main flow is tested from these test classes:
 * <pre>
 * {@code com.ericsson.oss.mediation.fm.o1.transform.TransformManagerSpec}
 */
class O1DateModelConverterSpec extends CdiSpecification {


    def "Test O1DateModelConverter with null value"() {

        given: "O1DateModelConverter is created"
            final NamedConverters o1DateModelConverter = new NamedConverters()

        when: "date value is null"
            String result = o1DateModelConverter.o1DateModelConverter(null)

        then: "no conversion happen and return null"
            assert result == null
    }

    def "Test O1DateModelConverter with invalid value"() {

        given: "O1DateModelConverter is created"
            final NamedConverters o1DateModelConverter = new NamedConverters()

        when: "the date to convert is missing the seconds"
            String result = o1DateModelConverter.o1DateModelConverter("2023-09-06T11:32")

        then: "TransformerException will be thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Invalid input value for date: [2023-09-06T11:32] Date should in OpenAPI (rfc3339) format")
    }

    // Note that test for more than three milliseconds is in e2e test case in TransformManagerSpec
    def "Test O1DateModelConverter with valid value with less than three millisecond digits"() {

        given: "O1DateModelConverter is created"
            final NamedConverters o1DateModelConverter = new NamedConverters()

        when: "the date to convert has one millisecond digit"
            String result = o1DateModelConverter.o1DateModelConverter("2023-09-06T11:32:01.7Z")

        then: "resulting date contains default milisecond value with zeros padded"
            assert result == "20230906113201.007"
    }

    def "Test O1DateModelConverter with valid value with no milliseconds"() {

        given: "O1DateModelConverter is created"
            final NamedConverters o1DateModelConverter = new NamedConverters()

        when: "the date to convert has no milliseconds"
            String result = o1DateModelConverter.o1DateModelConverter("2023-09-06T11:32:01Z")

        then: "resulting date contains default 000 for miliseconds"
            assert result == "20230906113201.000"
    }
}

