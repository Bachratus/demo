package com.bachratus.demo.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "demo.kafka")
public record DemoKafkaProperties(
        int partitions,
        int replicas,
        Map<String, String> topics
) {

    public DemoKafkaProperties {
        topics = topics == null ? Map.of() : Map.copyOf(topics);
    }

    public String topic(String key) {
        String topic = topics.get(key);
        if (topic == null || topic.isBlank())
            throw new IllegalArgumentException("Missing Kafka topic mapping for key: " + key);
        return topic;
    }
}
