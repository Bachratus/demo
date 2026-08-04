package com.bachratus.demo.infra.kafka.event;

import com.bachratus.demo.application.events.OutboxApplicationEvent;
import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.application.ports.out.OutboxEventDraftFactory;
import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Creates outbox event drafts from application events by applying Kafka topic configuration.
 */
@Component
@RequiredArgsConstructor
public class KafkaOutboxEventDraftFactory implements OutboxEventDraftFactory {

    private final AppKafkaProperties kafkaProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public OutboxEventDraft create(OutboxApplicationEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        AppKafkaProperties.Topic topic = kafkaProperties.topic(event.eventKey());

        return OutboxEventDraft.create(
                event.eventKey(),
                topic.aggregateType(),
                event.aggregateId(),
                topic.eventType(event.schemaVersion()),
                objectMapper.valueToTree(event),
                Instant.now(clock)
        );
    }
}
