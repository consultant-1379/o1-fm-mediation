/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.fm.o1.transform;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.fm.o1.transform.config.MediationConfigData;
import com.ericsson.oss.mediation.fm.o1.transform.mapper.AlarmRecordsMapper;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@ApplicationScoped
public class TransformManager {

    @Inject
    private SystemRecorder recorder;

    @Inject
    private EventNotificationTransformerProvider eventNotificationTransformerProvider;

    @Inject
    private AlarmRecordsMapper alarmRecordsMapper;

    protected XmlMapper xmlMapper = new XmlMapper();

    private static final String FDN = "fdn";
    public static final String ALARM_LIST = "AlarmList";
    public static final String ATTRIBUTES = "attributes";
    public static final String ALARM_RECORDS = "alarmRecords";

    @PostConstruct
    private void initCache() {
        MediationConfigData.readMediationConfigurationYaml();
    }

    public EventNotification translateAlarm(final Map<String, Object> alarm) {
        if (MapUtils.isEmpty(alarm)) {
            throw new TransformerException("Received request to transform alarm with no properties.");
        }
        try {
            return eventNotificationTransformerProvider
                    .selectMapper(alarm)
                    .convertMapToEventNotification(alarm);
        } catch (final Exception exception) {
            recorder.recordError("Transformation failed due to: [" + exception + "]", ErrorSeverity.WARNING, "ModelTransformer", "",
                    "For alarm with the following ID: " + alarm.get("alarmId"));

            throw new TransformerException("Error while converting alarm map to eventnotification", exception);
        }
    }

    public List<EventNotification> transformAlarmRecords(final String responseXml, final String meContextFdn) {
        if (isBlank(responseXml)) {
            throw new TransformerException("Received request to transform null or empty alarm records.");
        }
        try {
            final List<Map<String, Object>> alarmRecords = normalizeXmlAlarmRecordList(responseXml);
            return transformAlarmRecords(alarmRecords, meContextFdn);
        } catch (final Exception ex) {
            throw new TransformerException("Error while converting list of alarm records to list of eventnotification", ex);
        }
    }

    private List<EventNotification> transformAlarmRecords(final List<Map<String, Object>> alarmRecords, final String meContextFdn) {

        final List<EventNotification> eventNotifications = new ArrayList<>(alarmRecords.size());
        for (final Map<String, Object> alarmRecord : alarmRecords) {
            try {
                final EventNotification event = alarmRecordsMapper.convertMapToEventNotification(alarmRecord);
                event.setManagedObjectInstance(FdnUtil.createFdn(meContextFdn, event.getManagedObjectInstance()));
                event.addAdditionalAttribute(FDN, FdnUtil.getNetworkElementFdn(event.getManagedObjectInstance()));
                eventNotifications.add(event);

            } catch (final Exception exception) {
                recorder.recordError("Transformation failed due to: [" + exception + "]", ErrorSeverity.WARNING, "ModelTransformer", "",
                        "For the alarm with the following ID: " + alarmRecord.get("alarmId"));
            }
        }
        return eventNotifications;
    }

    private List<Map<String, Object>> normalizeXmlAlarmRecordList(final String responseXml) throws JsonProcessingException {
        Map alarmList = (Map) xmlMapper.readValue(responseXml, Map.class).get(ALARM_LIST);
        Map<String, Object> attributes = (Map<String, Object>) alarmList.get(ATTRIBUTES);
        List<Map<String, Object>> alarmRecords = new ArrayList<>();
        if (attributes.get(ALARM_RECORDS) instanceof List) {
            alarmRecords = (List<Map<String, Object>>) attributes.get(ALARM_RECORDS);
        } else {
            alarmRecords.add((Map<String, Object>) attributes.get(ALARM_RECORDS));
        }
        return alarmRecords;
    }
}
