package com.bachratus.demo.infra.db.outbox;

import com.bachratus.demo.application.events.OutboxEventDraft;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "outbox_event")
public class OutboxEventJpa {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "topic_key", nullable = false, updatable = false)
    private String topicKey;

    @Column(name = "topic_name", nullable = false, updatable = false)
    private String topicName;

    @Column(name = "aggregate_type", nullable = false, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "dead_lettered_at")
    private Instant deadLetteredAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OutboxEventJpa() {
    }

    public static OutboxEventJpa from(OutboxEventDraft draft, String topicName, Instant now) {
        Objects.requireNonNull(draft, "draft cannot be null");
        Objects.requireNonNull(now, "now cannot be null");

        OutboxEventJpa event = new OutboxEventJpa();
        event.id = draft.id();
        event.topicKey = draft.topicKey();
        event.topicName = requireText(topicName, "topicName");
        event.aggregateType = draft.aggregateType();
        event.aggregateId = draft.aggregateId();
        event.eventType = draft.eventType();
        event.payload = draft.payload().deepCopy();
        event.status = OutboxStatus.PENDING;
        event.retryCount = 0;
        event.nextAttemptAt = now;
        event.occurredAt = draft.occurredAt();
        event.createdAt = now;
        event.updatedAt = now;
        return event;
    }

    public void markPublished(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        status = OutboxStatus.PUBLISHED;
        publishedAt = now;
        lastError = null;
        nextAttemptAt = now;
        updatedAt = now;
    }

    public void markMainTopicPublishFailed(String error, int maxAttempts, long retryBackoffMs, Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        retryCount++;
        status = retryCount >= maxAttempts ? OutboxStatus.DLT_PENDING : OutboxStatus.FAILED;
        lastError = error;
        nextAttemptAt = now.plusMillis(retryBackoffMs);
        updatedAt = now;
    }

    public void markDeadLettered(String error, Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        status = OutboxStatus.DEAD_LETTERED;
        deadLetteredAt = now;
        lastError = error;
        nextAttemptAt = now;
        updatedAt = now;
    }

    public void markDeadLetterPublishFailed(String error, long retryBackoffMs, Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        status = OutboxStatus.DLT_PENDING;
        lastError = error;
        nextAttemptAt = now.plusMillis(retryBackoffMs);
        updatedAt = now;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException(fieldName + " cannot be blank");
        return value.trim();
    }
}
