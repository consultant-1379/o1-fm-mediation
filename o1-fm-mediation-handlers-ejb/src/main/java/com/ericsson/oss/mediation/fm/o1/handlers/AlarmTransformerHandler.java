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

package com.ericsson.oss.mediation.fm.o1.handlers;

import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.HREF;
import static com.ericsson.oss.mediation.fm.o1.common.util.Constants.SYSTEM_DN;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.ALARM_DISCARDED_WITH_TRANSFORMATION_EXCEPTION;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.FAILED_TO_TRANSFORM_ALARM;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics;
import com.ericsson.oss.mediation.fm.o1.transform.TransformManager;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler translates the alarm object received to an {@code EventNotification} and adds it to the payload of the {@code MediationComponentEvent} sent
 * to the next handler.
 */
@Slf4j
@EventHandler
public class AlarmTransformerHandler extends AbstractFmMediationHandler {

    @Inject
    private O1HandlerStatistics o1HandlerStatistics;

    @Inject
    private TransformManager transformManager;

    protected String getHandlerName() {
        return AlarmTransformerHandler.class.getName();
    }

    @Override
    public Object onEventWithResult(final Object inputEvent) {
        log.debug("Received event of type {} ", inputEvent.getClass().getName());
        super.onEventWithResult(inputEvent);
        try {
            if (payload instanceof Map<?, ?>) {
                log.debug("Translating alarm");
                final EventNotification eventNotification = transformManager.translateAlarm((Map<String, Object>) payload);
                o1HandlerStatistics.incrementTotalNoOfSuccessfulTransformations();
                log.debug("Translating alarm done {}", eventNotification);
                return new MediationComponentEvent(headers, eventNotification);
            }
        } catch (final Exception exception) {
            final String hrefValue = (String) ((Map<String, Object>) payload).get(HREF);
            final String systemDN = (String) ((Map<String, Object>) payload).get(SYSTEM_DN);
            getRecorder().recordError(getHandlerName(), systemDN, hrefValue,
                    String.format(ALARM_DISCARDED_WITH_TRANSFORMATION_EXCEPTION, exception.getMessage()));
            throw new EventHandlerException(FAILED_TO_TRANSFORM_ALARM, exception);
        }
        return new MediationComponentEvent(headers, StringUtils.EMPTY);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
