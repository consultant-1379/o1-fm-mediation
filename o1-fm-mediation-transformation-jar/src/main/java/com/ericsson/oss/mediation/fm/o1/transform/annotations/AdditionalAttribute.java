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

package com.ericsson.oss.mediation.fm.o1.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to map alarm data to an entry in the 'additionalAttributes' field of {@code EventNotification}.
 **/
@Repeatable(AdditionalAttributeMappings.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AdditionalAttribute {

    /**
     * The name of the parameter to be mapped from the alarm data.
     * <p>
     * Its value will be added as the value in the 'additionalAttributes' entry.
     **/
    String source();

    /**
     * The value to use as the key when adding the 'additionalAttributes' entry.
     * <p>
     * This
     **/
    String target();

    /**
     * String-based form of qualifier, used when looking for a suitable mapping method for a given property.
     * Default value is an empty String.
     **/

    String qualifiedByName() default "";

}
