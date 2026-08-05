package com.bachratus.demo.infra.kafka.config.consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Enables Spring Kafka listener infrastructure when the application Kafka integration is active.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableKafka
public class KafkaMessagingConfiguration {
}
