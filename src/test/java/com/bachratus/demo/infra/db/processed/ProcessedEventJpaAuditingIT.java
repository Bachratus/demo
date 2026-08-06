package com.bachratus.demo.infra.db.processed;

import com.bachratus.demo.config.BaseJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventJpaAuditingIT extends BaseJpaIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSetAuditingFieldsWhenProcessedEventIsPersisted() {
        // given
        Instant creationTime = Instant.parse("2026-01-01T10:00:00Z");
        clock.setInstant(creationTime);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("creator-user", null, "ROLE_USER")
        );

        ProcessedEventJpa processedEvent = ProcessedEventJpa.of(
                "customer.account-created.v1",
                UUID.randomUUID().toString()
        );

        // when
        ProcessedEventJpa persisted = entityManager.persistAndFlush(processedEvent);

        // then
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getVersion()).isNotNull();

        assertThat(persisted.getCreatedAt()).isEqualTo(creationTime);
        assertThat(persisted.getUpdatedAt()).isEqualTo(creationTime);

        assertThat(persisted.getCreatedBy()).isEqualTo("creator-user");
        assertThat(persisted.getUpdatedBy()).isEqualTo("creator-user");
    }
}
