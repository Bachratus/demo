package com.bachratus.demo.infra.kafka.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link AppKafkaProperties} so application Kafka settings can be injected as a typed bean.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppKafkaProperties.class)
public class AppKafkaPropertiesConfiguration {
}
