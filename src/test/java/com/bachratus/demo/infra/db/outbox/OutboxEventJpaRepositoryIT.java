package com.bachratus.demo.infra.db.outbox;

import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.config.BaseJpaIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventJpaRepositoryIT extends BaseJpaIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TOPIC_NAME = "store.customer-account-created.v1";
    private static final Instant DUE_TIME = Instant.now().minusSeconds(60);
    private static final Instant FUTURE_TIME = Instant.now().plusSeconds(3_600);
    private static final Instant OCCURRED_BASE = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    private OutboxEventJpaRepository repository;

    @DisplayName("Tests for streamPublishableEvents(int, int)")
    @Nested
    class StreamPublishableEvents {

        @Test
        void shouldSelectOnlyDuePublishableEventsAndKeepStableOrder() {
            // given
            OutboxEventJpa pendingDue = pendingEvent("pending-due", DUE_TIME, OCCURRED_BASE.plusSeconds(1));
            OutboxEventJpa failedDue = pendingEvent("failed-due", DUE_TIME, OCCURRED_BASE.plusSeconds(2));
            failedDue.markMainTopicPublishFailed("temporary broker failure", 5, 0, DUE_TIME);

            OutboxEventJpa dltPendingDue = pendingEvent("dlt-pending-due", DUE_TIME, OCCURRED_BASE.plusSeconds(3));
            dltPendingDue.markMainTopicPublishFailed("main topic exhausted", 1, 0, DUE_TIME);

            OutboxEventJpa pendingFuture = pendingEvent("pending-future", FUTURE_TIME, OCCURRED_BASE.plusSeconds(4));

            OutboxEventJpa failedFuture = pendingEvent("failed-future", DUE_TIME, OCCURRED_BASE.plusSeconds(5));
            failedFuture.markMainTopicPublishFailed("future retry", 5, 3_600_000, Instant.now());

            OutboxEventJpa dltPendingFuture = pendingEvent("dlt-pending-future", DUE_TIME, OCCURRED_BASE.plusSeconds(6));
            dltPendingFuture.markMainTopicPublishFailed("future dlt retry", 1, 3_600_000, Instant.now());

            OutboxEventJpa published = pendingEvent("published", DUE_TIME, OCCURRED_BASE.plusSeconds(7));
            published.markPublished(DUE_TIME);

            OutboxEventJpa deadLettered = pendingEvent("dead-lettered", DUE_TIME, OCCURRED_BASE.plusSeconds(8));
            deadLettered.markMainTopicPublishFailed("main exhausted", 1, 0, DUE_TIME);
            deadLettered.markDeadLettered("main exhausted", DUE_TIME);

            repository.saveAll(List.of(
                    pendingFuture,
                    deadLettered,
                    dltPendingDue,
                    failedFuture,
                    pendingDue,
                    published,
                    dltPendingFuture,
                    failedDue
            ));
            repository.flush();

            // when
            List<UUID> resultIds = streamPublishableIds(20, 5);

            // then
            assertThat(resultIds).containsExactly(
                    pendingDue.getId(),
                    failedDue.getId(),
                    dltPendingDue.getId()
            );
        }

        @Test
        void shouldApplyBatchLimitAfterOrderingPublishableEvents() {
            // given
            OutboxEventJpa first = pendingEvent("first", DUE_TIME, OCCURRED_BASE.plusSeconds(1));
            OutboxEventJpa second = pendingEvent("second", DUE_TIME, OCCURRED_BASE.plusSeconds(2));
            OutboxEventJpa third = pendingEvent("third", DUE_TIME, OCCURRED_BASE.plusSeconds(3));

            repository.saveAll(List.of(third, first, second));
            repository.flush();

            // when
            List<UUID> resultIds = streamPublishableIds(2, 5);

            // then
            assertThat(resultIds).containsExactly(first.getId(), second.getId());
        }
    }

    private List<UUID> streamPublishableIds(int batchSize, int maxAttempts) {
        try (Stream<OutboxEventJpa> events = repository.streamPublishableEvents(batchSize, maxAttempts)) {
            return events.map(OutboxEventJpa::getId).toList();
        }
    }

    private OutboxEventJpa pendingEvent(String aggregateId, Instant nextAttemptAt, Instant occurredAt) {
        OutboxEventDraft draft = new OutboxEventDraft(
                UUID.randomUUID(),
                "customer-account-created",
                "customer",
                aggregateId,
                "customer.account-created.v1",
                OBJECT_MAPPER.createObjectNode()
                        .put("schemaVersion", 1)
                        .put("aggregateId", aggregateId),
                occurredAt
        );

        return OutboxEventJpa.from(draft, TOPIC_NAME, nextAttemptAt);
    }
}
