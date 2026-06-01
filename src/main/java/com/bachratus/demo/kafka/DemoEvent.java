package com.bachratus.demo.kafka;

import java.time.Instant;
import java.util.UUID;

public record DemoEvent(
        UUID id,
        String message,
        Instant occurredAt
) {
}
