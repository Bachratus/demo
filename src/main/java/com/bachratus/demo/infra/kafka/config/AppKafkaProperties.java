package com.bachratus.demo.infra.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.kafka")
public record AppKafkaProperties(
        Boolean enabled,
        Producer producer,
        Listener listener,
        Outbox outbox,
        Map<String, Topic> topics
) {

    public AppKafkaProperties {
        enabled = enabled == null ? Boolean.TRUE : enabled;
        producer = producer == null ? Producer.defaults() : producer;
        listener = listener == null ? Listener.defaults() : listener;
        outbox = outbox == null ? Outbox.defaults() : outbox;
        topics = topics == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(topics));

        if (producer.sendResultTimeoutMs() <= producer.deliveryTimeoutMs()) {
            throw new IllegalArgumentException(
                    "Kafka producer sendResultTimeoutMs must be greater than deliveryTimeoutMs"
            );
        }
    }

    public Topic topic(String key) {
        Topic topic = topics.get(key);
        if (topic == null)
            throw new IllegalArgumentException("Missing Kafka topic configuration for key: " + key);
        return topic;
    }

    public String topicName(String key) {
        return topic(key).name();
    }

    public String deadLetterTopicName(String key, String topicName) {
        Topic topic = topics.get(key);
        if (topic == null) {
            return topicName + ".dlt";
        }
        return topic.dltName();
    }

    public String deadLetterTopicNameForTopic(String topicName) {
        String normalizedTopicName = requireText(topicName, "topicName");

        return topics.values()
                .stream()
                .filter(topic -> topic.name().equals(normalizedTopicName))
                .map(Topic::dltName)
                .findFirst()
                .orElse(normalizedTopicName + ".dlt");
    }

    public record Producer(
            long deliveryTimeoutMs,
            long requestTimeoutMs,
            long lingerMs,
            long sendResultTimeoutMs
    ) {

        public Producer {
            if (deliveryTimeoutMs < 1) throw new IllegalArgumentException("Kafka producer deliveryTimeoutMs must be positive");
            if (requestTimeoutMs < 1) throw new IllegalArgumentException("Kafka producer requestTimeoutMs must be positive");
            if (lingerMs < 0) throw new IllegalArgumentException("Kafka producer lingerMs cannot be negative");
            if (sendResultTimeoutMs < 1) throw new IllegalArgumentException("Kafka producer sendResultTimeoutMs must be positive");
            if (requestTimeoutMs >= deliveryTimeoutMs) {
                throw new IllegalArgumentException("Kafka producer requestTimeoutMs must be lower than deliveryTimeoutMs");
            }
        }

        private static Producer defaults() {
            return new Producer(45_000, 15_000, 5, 50_000);
        }
    }

    public record Listener(
            long retryIntervalMs,
            int maxAttempts
    ) {

        public Listener {
            if (retryIntervalMs < 0) throw new IllegalArgumentException("Kafka listener retryIntervalMs cannot be negative");
            if (maxAttempts < 1) throw new IllegalArgumentException("Kafka listener maxAttempts must be positive");
        }

        private static Listener defaults() {
            return new Listener(1_000, 3);
        }
    }

    public record Outbox(
            boolean enabled,
            int batchSize,
            long pollDelayMs,
            long retryBackoffMs,
            int maxAttempts
    ) {

        public Outbox {
            if (batchSize < 1) throw new IllegalArgumentException("Kafka outbox batchSize must be positive");
            if (pollDelayMs < 1) throw new IllegalArgumentException("Kafka outbox pollDelayMs must be positive");
            if (retryBackoffMs < 0) throw new IllegalArgumentException("Kafka outbox retryBackoffMs cannot be negative");
            if (maxAttempts < 1) throw new IllegalArgumentException("Kafka outbox maxAttempts must be positive");
        }

        private static Outbox defaults() {
            return new Outbox(true, 100, 500, 10_000, 5);
        }
    }

    public record Topic(
            String name,
            int concurrency,
            String dltName,
            String eventType,
            String aggregateType
    ) {

        public Topic {
            name = requireText(name, "Kafka topic name");
            if (concurrency < 1) throw new IllegalArgumentException("Kafka topic concurrency must be positive");
            dltName = dltName == null || dltName.isBlank() ? name + ".dlt" : dltName.trim();
            eventType = requireText(eventType, "Kafka event type");
            aggregateType = requireText(aggregateType, "Kafka aggregate type");
        }

        public String eventType(int schemaVersion) {
            if (schemaVersion < 1) throw new IllegalArgumentException("Kafka event schemaVersion must be positive");
            return eventType + ".v" + schemaVersion;
        }

        private static String requireText(String value, String fieldName) {
            return AppKafkaProperties.requireText(value, fieldName);
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException(fieldName + " cannot be blank");
        return value.trim();
    }
}
