package com.bachratus.demo.infra.db.outbox;

import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.application.ports.out.OutboxEventStore;
import com.bachratus.demo.infra.kafka.AppKafkaProperties;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Transactional
@Repository
@RequiredArgsConstructor
public class OutboxEventStoreAdapter implements OutboxEventStore {

    private final OutboxEventJpaRepository repository;
    private final AppKafkaProperties kafkaProperties;
    private final Clock clock;

    @Override
    public void append(OutboxEventDraft event) {
        Objects.requireNonNull(event, "event cannot be null");

        String topicName = kafkaProperties.topicName(event.topicKey());
        Instant now = Instant.now(clock);

        repository.save(OutboxEventJpa.from(event, topicName, now));
    }
}
