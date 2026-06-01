package com.bachratus.demo.infra.kafka;

import java.time.Instant;
import java.util.UUID;

public record DemoEvent(
        UUID id,
        UUID aggregateId,
        String message,
        Instant occurredAt
) implements KafkaEvent {
}
