
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.mockito.ArgumentMatcher;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;

public class Matchers {
    public static class EventNotificationTypesMatcher implements ArgumentMatcher<EventNotificationBatch> {

        private String[] expectedTypes;

        public EventNotificationTypesMatcher(String... types) {
            this.expectedTypes = types;
        }

        @Override
        public boolean matches(EventNotificationBatch eventNotificationBatch) {
            try {
                List<EventNotification> notifications = (List<EventNotification>) new ObjectInputStream(
                        new ByteArrayInputStream(eventNotificationBatch.getSerializedData())).readObject();
                List<String> actualTypes = notifications.stream().map(n -> n.getRecordType()).collect(Collectors.toList());
                return actualTypes.equals(Arrays.asList(expectedTypes));
            } catch (ClassNotFoundException | IOException e) {
                fail(e.getMessage());
            }
            return false;
        }
    }
}
