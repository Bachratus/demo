package com.bachratus.demo.infra.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Set;

/**
 * Removes selected Spring Boot auto-configurations when application-level infrastructure features are disabled.
 */
public class InfrastructureAutoConfigurationImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    private static final Map<String, Set<String>> AUTO_CONFIGURATIONS_BY_FEATURE_FLAG = Map.of(
            "app.kafka.enabled", Set.of(KafkaAutoConfiguration.class.getName())
    );

    private Environment environment;

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];

        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            matches[i] = shouldImport(autoConfigurationClasses[i]);
        }

        return matches;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private boolean shouldImport(String autoConfigurationClass) {
        if (autoConfigurationClass == null) {
            return true;
        }

        return AUTO_CONFIGURATIONS_BY_FEATURE_FLAG.entrySet()
                .stream()
                .noneMatch(entry -> isDisabled(entry.getKey()) && entry.getValue().contains(autoConfigurationClass));
    }

    private boolean isDisabled(String featureFlag) {
        return environment != null
                && !environment.getProperty(featureFlag, Boolean.class, Boolean.TRUE);
    }
}
