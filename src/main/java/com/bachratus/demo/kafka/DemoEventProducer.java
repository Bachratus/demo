package com.bachratus.demo.kafka;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class DemoEventProducer {

    private final KafkaEventPublisher eventPublisher;
    private final String topic;

    public DemoEventProducer(
            KafkaEventPublisher eventPublisher,
            DemoKafkaProperties kafkaProperties
    ) {
        this.eventPublisher = eventPublisher;
        this.topic = kafkaProperties.topic("events");
    }

    public DemoEvent publish(UUID aggregateId, String message) {
        DemoEvent event = new DemoEvent(UUID.randomUUID(), aggregateId, message, Instant.now());

        eventPublisher.publish(topic, event);
        return event;
    }
}
