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

package com.ericsson.oss.mediation.fm.o1.nodesuspender;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * A class to store the count of notifications received, and to indicate if that NE is suspended or not
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class NodeSuspensionEntry implements Serializable {

    private static final long serialVersionUID = 989356321L;

    private int count;

    private boolean isSuspended;

    public NodeSuspensionEntry(final int count, final boolean isSuspended) {
        this.count = count;
        this.isSuspended = isSuspended;
    }
}
