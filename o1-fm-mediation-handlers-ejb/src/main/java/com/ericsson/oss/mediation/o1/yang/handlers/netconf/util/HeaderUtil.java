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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util;

import java.util.Map;
import java.util.Optional;

import lombok.experimental.UtilityClass;

/**
 * Contains methods that can be used to read headers from the events received by handlers.
 */
@UtilityClass
public class HeaderUtil {

    public static <T> T readHeader(final Map<String, Object> moHeaders, final String name, final T defaultV) {
        return (T) readHeader(moHeaders, name).orElse(defaultV);
    }

    public static <T> Optional<T> readHeader(final Map<String, Object> headers, final String name) {
        return Optional.ofNullable((T) headers.get(name));
    }
}
