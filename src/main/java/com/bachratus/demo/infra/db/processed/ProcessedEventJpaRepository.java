package com.bachratus.demo.infra.db.processed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Stores processed Kafka event identifiers used for idempotent consumer handling.
 */
@Repository
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpa, Long> {

    boolean existsByEventTypeAndEventId(String eventType, String eventId);
}
