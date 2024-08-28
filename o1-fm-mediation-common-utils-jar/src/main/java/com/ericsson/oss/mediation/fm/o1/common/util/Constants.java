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

package com.ericsson.oss.mediation.fm.o1.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    // Model related constants
    public static final String FDN = "fdn";
    public static final String FM_FUNCTION_RDN = "FmFunction=1";
    public static final String FM_ALARM_SUPERVISION_RDN = "FmAlarmSupervision=1";
    public static final String NETWORK_ELEMENT = "NetworkElement";
    public static final String AUTOMATIC_SYNCHRONIZATION = "automaticSynchronization";

    // JMS related constants
    public static final String JMS = "JMS";
    public static final String OBJECT_MESSAGE = "ObjectMessage";

    // Notification related constants
    public static final String HREF = "href";

    public static final String SYSTEM_DN = "systemDN";
    public static final String SYNCHRONIZATION_ALARM = "SYNCHRONIZATION_ALARM";

}
