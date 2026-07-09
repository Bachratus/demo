package com.bachratus.demo.infra.db.customer;

import com.bachratus.demo.config.BaseJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerJpaTest extends BaseJpaIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSetAuditingFieldsWhenCustomerIsPersisted() {
        // given
        Instant creationTime = Instant.parse("2026-01-01T10:00:00Z");
        clock.setInstant(creationTime);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("creator-user", null, "ROLE_USER")
        );

        CustomerJpa customerJpa = new CustomerJpa();
        customerJpa.setPublicId(UUID.randomUUID());
        customerJpa.setUserId("123");
        customerJpa.setDisplayName("Me");

        // when
        CustomerJpa persisted = entityManager.persistAndFlush(customerJpa);

        // then
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getVersion()).isNotNull();

        assertThat(persisted.getCreatedAt()).isEqualTo(creationTime);
        assertThat(persisted.getUpdatedAt()).isEqualTo(creationTime);

        assertThat(persisted.getCreatedBy()).isEqualTo("creator-user");
        assertThat(persisted.getUpdatedBy()).isEqualTo("creator-user");
    }

    @Test
    void shouldUpdateModificationAuditFieldsWhenCustomerIsUpdated() {
        // given
        Instant creationTime = Instant.parse("2026-01-01T10:00:00Z");
        Instant modificationTime = Instant.parse("2026-01-01T11:00:00Z");

        clock.setInstant(creationTime);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("creator-user", null, "ROLE_USER")
        );

        CustomerJpa customerJpa = new CustomerJpa();
        customerJpa.setPublicId(UUID.randomUUID());
        customerJpa.setUserId("123");
        customerJpa.setDisplayName("Old name");

        CustomerJpa persisted = entityManager.persistAndFlush(customerJpa);
        entityManager.clear();

        CustomerJpa existingCustomer = entityManager.find(CustomerJpa.class, persisted.getId());

        Integer versionBeforeUpdate = existingCustomer.getVersion();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("modifier-user", null, "ROLE_USER")
        );

        clock.setInstant(modificationTime);

        // when
        existingCustomer.setDisplayName("New name");
        entityManager.flush();
        entityManager.clear();

        CustomerJpa updatedCustomer = entityManager.find(CustomerJpa.class, persisted.getId());

        // then
        assertThat(updatedCustomer.getCreatedAt()).isEqualTo(creationTime);
        assertThat(updatedCustomer.getCreatedBy()).isEqualTo("creator-user");

        assertThat(updatedCustomer.getUpdatedAt()).isEqualTo(modificationTime);
        assertThat(updatedCustomer.getUpdatedBy()).isEqualTo("modifier-user");

        assertThat(updatedCustomer.getVersion()).isGreaterThan(versionBeforeUpdate);
    }
}