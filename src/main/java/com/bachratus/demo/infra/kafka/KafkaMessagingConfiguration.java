package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration(proxyBeanMethods = false)
@EnableKafka
@EnableConfigurationProperties(AppKafkaProperties.class)
public class KafkaMessagingConfiguration {
}
