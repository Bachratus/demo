package com.bachratus.demo.infra.db.processed;

import com.bachratus.demo.config.BaseJpaIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessedEventJpaRepositoryIT extends BaseJpaIntegrationTest {

    @Autowired
    private ProcessedEventJpaRepository repository;

    @DisplayName("Tests for processed event uniqueness")
    @Nested
    class Uniqueness {

        @Test
        void shouldRejectDuplicateEventTypeAndEventIdPair() {
            // given
            String eventType = "customer.account-created.v1";
            String eventId = UUID.randomUUID().toString();

            repository.saveAndFlush(ProcessedEventJpa.of(eventType, eventId));

            // when & then
            assertThatThrownBy(() -> repository.saveAndFlush(ProcessedEventJpa.of(eventType, eventId)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void shouldAllowSameEventIdForDifferentEventTypesAndDifferentEventIdsForSameType() {
            // given
            String sharedEventId = UUID.randomUUID().toString();
            String eventType = "customer.account-created.v1";

            // when
            ProcessedEventJpa first = repository.saveAndFlush(ProcessedEventJpa.of(eventType, sharedEventId));
            ProcessedEventJpa second = repository.saveAndFlush(ProcessedEventJpa.of(
                    "customer.account-updated.v1",
                    sharedEventId
            ));
            ProcessedEventJpa third = repository.saveAndFlush(ProcessedEventJpa.of(
                    eventType,
                    UUID.randomUUID().toString()
            ));

            // then
            assertThat(first.getId()).isNotNull();
            assertThat(second.getId()).isNotNull();
            assertThat(third.getId()).isNotNull();
        }

        @Test
        void shouldTrimEventIdentityBeforePersisting() {
            // when
            ProcessedEventJpa persisted = repository.saveAndFlush(ProcessedEventJpa.of(
                    "  customer.account-created.v1  ",
                    "  " + UUID.randomUUID() + "  "
            ));

            // then
            assertThat(persisted.getEventType()).isEqualTo("customer.account-created.v1");
            assertThat(persisted.getEventId()).doesNotStartWith(" ").doesNotEndWith(" ");
        }
    }
}
