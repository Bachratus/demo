package com.bachratus.demo.infra.kafka.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableKafka
public class KafkaMessagingConfiguration {
}
