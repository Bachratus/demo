package com.bachratus.demo.infra.db.outbox;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpa, UUID> {

    @QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "50"))
    @Query(value = """
            SELECT *
            FROM outbox_event
            WHERE next_attempt_at <= CURRENT_TIMESTAMP
              AND (
                    status = 'PENDING'
                    OR (status = 'FAILED' AND retry_count < :maxAttempts)
                    OR status = 'DLT_PENDING'
                  )
            ORDER BY occurred_at ASC, id ASC
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    Stream<OutboxEventJpa> streamPublishableEvents(
            @Param("batchSize") int batchSize,
            @Param("maxAttempts") int maxAttempts
    );
}
