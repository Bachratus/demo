package com.bachratus.demo.kafka;

import java.time.Instant;
import java.util.UUID;

public interface KafkaEvent {

    UUID id();

    UUID aggregateId();

    Instant occurredAt();
}
