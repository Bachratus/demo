package com.bachratus.demo.infra.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.kafka")
public record AppKafkaProperties(
        int partitions,
        int replicas,
        Map<String, String> topics
) {

    public AppKafkaProperties {
        topics = topics == null ? Map.of() : Map.copyOf(topics);
    }

    public String topic(String key) {
        String topic = topics.get(key);
        if (topic == null || topic.isBlank())
            throw new IllegalArgumentException("Missing Kafka topic mapping for key: " + key);
        return topic;
    }
}
