package com.bachratus.demo.config;

import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.stream.Stream;

@TestConfiguration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AppKafkaProperties.class)
public class KafkaTopicsTestConfiguration {

    private static final int PARTITIONS = 3;
    private static final int REPLICATION_FACTOR = 1;
    private static final int MIN_IN_SYNC_REPLICAS = 1;

    @Bean
    KafkaAdmin.NewTopics demoKafkaTopics(AppKafkaProperties properties) {
        NewTopic[] topics = properties.topics()
                .values()
                .stream()
                .flatMap(topic -> Stream.of(
                        topic(topic.name()),
                        topic(topic.dltName())
                ))
                .toArray(NewTopic[]::new);

        return new KafkaAdmin.NewTopics(topics);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(MIN_IN_SYNC_REPLICAS))
                .build();
    }
}
