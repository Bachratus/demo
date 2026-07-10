package com.bachratus.demo.infra.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppKafkaProperties.class)
public class KafkaTopicConfiguration {

    @Bean
    public KafkaAdmin.NewTopics demoTopics(AppKafkaProperties properties) {
        NewTopic[] topics = properties.topics().values().stream()
                .map(topic -> TopicBuilder.name(topic)
                        .partitions(properties.partitions())
                        .replicas(properties.replicas())
                        .build())
                .toArray(NewTopic[]::new);

        return new KafkaAdmin.NewTopics(topics);
    }
}
