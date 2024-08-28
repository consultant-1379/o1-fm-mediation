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

package com.ericsson.oss.mediation.fm.o1.handlers.util;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

/**
 * Utility class to access the system recorder methods needed by the handlers.
 * Note, the recordEvent method cannot be used to record DDP data, to do this the recordEventData method must be used.
 */
public class Recorder {

    @Inject
    private SystemRecorder systemRecorder;

    public void recordEvent(final String handlerName, final String source, final String resource, final String additionalInformation) {
        systemRecorder.recordEvent(handlerName, EventLevel.DETAILED, source, resource, additionalInformation);
    }

    public void recordError(final String handlerName, final String source, final String resource, final String additionalInformation) {
        systemRecorder.recordError(handlerName, ErrorSeverity.ERROR, source, resource, additionalInformation);
    }

    public void recordWarning(final String handlerName, final String source, final String resource, final String additionalInformation) {
        systemRecorder.recordError(handlerName, ErrorSeverity.WARNING, source, resource, additionalInformation);
    }
}
