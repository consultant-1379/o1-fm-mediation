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

package com.ericsson.oss.mediation.fm.o1.transform.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.ericsson.oss.mediation.fm.o1.transform.TransformerException;

import lombok.experimental.UtilityClass;

/**
 * Class responsible for reading the following mediation configuration data from file and storing it in memory:
 * <ul>
 * <li>Supported O1 alarm types</li>
 * <li>Mappings for supported probableCause fields</li>
 * </ul>"
 **/
@UtilityClass
public class MediationConfigData {

    private static final String MEDIATION_CONFIG_YAML = "Mediation_Config.yaml";

    private static Map<Integer, String> probableCause = new HashMap<>();

    public static void readMediationConfigurationYaml() {
        final Yaml yaml = new Yaml(new Constructor(MediationConfig.class, new LoaderOptions()));
        try (InputStream inputStream = MediationConfigData.class.getClassLoader().getResourceAsStream(MEDIATION_CONFIG_YAML)) {
            final MediationConfig config = yaml.load(inputStream);
            probableCause = config.getProbableCause();
        } catch (final Exception e) {
            throw new TransformerException("Error reading mediation configuration yaml file", e);
        }
    }

    public static Map<Integer, String> getProbableCause() {
        return new HashMap<>(probableCause);
    }

}
