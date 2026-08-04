package com.bachratus.demo.application.events;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OutboxEventDraft(
        UUID id,
        String topicKey,
        String aggregateType,
        String aggregateId,
        String eventType,
        JsonNode payload,
        Instant occurredAt
) {

    public OutboxEventDraft {
        Objects.requireNonNull(id, "id cannot be null");
        topicKey = requireText(topicKey, "topicKey");
        aggregateType = requireText(aggregateType, "aggregateType");
        aggregateId = requireText(aggregateId, "aggregateId");
        eventType = requireText(eventType, "eventType");
        Objects.requireNonNull(payload, "payload cannot be null");
        if (!payload.isObject()) {
            throw new IllegalArgumentException("payload must be a parsed JSON object");
        }
        payload = payload.deepCopy();
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    public static OutboxEventDraft create(
            String topicKey,
            String aggregateType,
            String aggregateId,
            String eventType,
            JsonNode payload,
            Instant occurredAt
    ) {
        return new OutboxEventDraft(
                UUID.randomUUID(),
                topicKey,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                occurredAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException(fieldName + " cannot be blank");
        return value.trim();
    }
}
