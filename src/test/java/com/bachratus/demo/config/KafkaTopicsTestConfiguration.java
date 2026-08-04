package com.bachratus.demo.config;

import com.bachratus.demo.application.events.CustomerAccountCreatedEvent;
import com.bachratus.demo.infra.kafka.AppKafkaProperties;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppKafkaProperties.class)
public class KafkaTopicsTestConfiguration {

    private static final int PARTITIONS = 3;
    private static final int REPLICATION_FACTOR = 1;
    private static final int MIN_IN_SYNC_REPLICAS = 1;

    @Bean
    KafkaAdmin.NewTopics demoKafkaTopics(AppKafkaProperties properties) {
        AppKafkaProperties.Topic topic = properties.topic(CustomerAccountCreatedEvent.TOPIC_KEY);

        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(topic.name())
                        .partitions(PARTITIONS)
                        .replicas(REPLICATION_FACTOR)
                        .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(MIN_IN_SYNC_REPLICAS))
                        .build(),
                TopicBuilder.name(topic.dltName())
                        .partitions(PARTITIONS)
                        .replicas(REPLICATION_FACTOR)
                        .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(MIN_IN_SYNC_REPLICAS))
                        .build()
        );
    }
}
