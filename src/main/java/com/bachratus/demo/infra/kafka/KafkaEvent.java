package com.bachratus.demo.infra.kafka;

import java.time.Instant;
import java.util.UUID;

public interface KafkaEvent {

    UUID id();

    UUID aggregateId();

    Instant occurredAt();
}
