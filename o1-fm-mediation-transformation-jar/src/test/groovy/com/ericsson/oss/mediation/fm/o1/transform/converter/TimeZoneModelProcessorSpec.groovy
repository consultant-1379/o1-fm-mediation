package com.ericsson.oss.mediation.fm.o1.transform.converter

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.transform.converter.impl.NamedConverters

/**
 * Tests here are mainly for negative and edge case scenarios that are difficult or not worth testing e2e with the CDI test framework.
 * <p>
 * The main flow is tested from these test classes:
 * <pre>
 * {@code com.ericsson.oss.mediation.fm.o1.transform.TransformManagerSpec}
 */
class TimeZoneModelProcessorSpec extends CdiSpecification {

    def "Test TimeZoneModelProcessor with invalid value"() {

        given: "TimeZoneModelProcessor is created"
            final String eventTime = "2023-09-06T11:32"
            final NamedConverters timeZoneModelProcessor = new NamedConverters()

        when: "no UTC offset is present in eventTime"
             final String eventNotificationTimeZone = timeZoneModelProcessor.timeZoneModelProcessor(eventTime)

        then: "timeZone is not set"
            assert eventNotificationTimeZone == null
    }

    def "Test TimeZoneModelProcessor with eventTime null"() {

        given: "TimeZoneModelProcessor is created"
            final NamedConverters timeZoneModelProcessor = new NamedConverters()

        when: "eventTime is set as null"
            final String eventNotificationTimeZone = timeZoneModelProcessor.timeZoneModelProcessor(null)

        then: "timeZone is not set"
            assert eventNotificationTimeZone == null
    }

    def "Test TimeZoneModelProcessor with valid value"() {

        given: "TimeZoneModelProcessor is created"
            final NamedConverters timeZoneModelProcessor = new NamedConverters()

        when: "the UTC offset is extracted and timeZone is set"
              final String eventNotificationTimeZone =  timeZoneModelProcessor.timeZoneModelProcessor(eventTime)

        then: "assert that the timeZone is correct"
            assert eventNotificationTimeZone == timeZone

        where:
            eventTime                       | timeZone
            "2018-06-21T15:55:01.897Z"      | "GMT+00:00"
            "2018-06-21T15:55:01.897+05:30" | "GMT+05:30"
            "2018-06-21T15:55:01.897-01:00" | "GMT-01:00"
    }
}
