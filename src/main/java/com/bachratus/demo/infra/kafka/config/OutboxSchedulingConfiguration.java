package com.bachratus.demo.infra.kafka.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled outbox publication when Kafka outbox publishing is configured as active.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.kafka",
        name = {"enabled", "outbox.enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@EnableScheduling
public class OutboxSchedulingConfiguration {
}
