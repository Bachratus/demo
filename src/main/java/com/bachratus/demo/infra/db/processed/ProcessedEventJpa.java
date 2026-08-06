package com.bachratus.demo.infra.db.processed;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * Persisted marker for a Kafka event that has already been accepted for processing.
 */
@Getter
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "processed_event")
@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = @UniqueConstraint(
                name = "processed_events_event_unique",
                columnNames = {"event_type", "event_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class ProcessedEventJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ProcessedEventsIdSeqGen")
    @SequenceGenerator(name = "ProcessedEventsIdSeqGen", sequenceName = "processed_events_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    protected ProcessedEventJpa() {
    }

    public static ProcessedEventJpa of(String eventType, String eventId) {
        ProcessedEventJpa event = new ProcessedEventJpa();
        event.eventType = requireText(eventType, "eventType");
        event.eventId = requireText(eventId, "eventId");
        return event;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException(fieldName + " cannot be blank");
        return value.trim();
    }
}
