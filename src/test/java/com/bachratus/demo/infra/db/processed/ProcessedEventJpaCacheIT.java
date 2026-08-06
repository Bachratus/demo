package com.bachratus.demo.infra.db.processed;

import com.bachratus.demo.config.BaseJpaCacheIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProcessedEventJpaCacheIT extends BaseJpaCacheIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ProcessedEventJpaRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private SessionFactory sessionFactory;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();

        statistics.setStatisticsEnabled(true);
        sessionFactory.getCache().evictAllRegions();
        statistics.clear();

        clock.setInstant(Instant.parse("2026-01-01T10:00:00Z"));

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("cache-test-user", null, "ROLE_USER")
        );
    }

    @Test
    void shouldUseSecondLevelCacheWhenProcessedEventIsLoadedByJpaIdTwice() {
        // given
        Long databaseId = persistProcessedEvent();

        sessionFactory.getCache().evictAllRegions();
        statistics.clear();

        // when
        Optional<ProcessedEventJpa> firstResult = transactionTemplate.execute(
                status -> repository.findById(databaseId)
        );

        long preparedStatementsAfterFirstRead = statistics.getPrepareStatementCount();
        long secondLevelCacheHitsAfterFirstRead = statistics.getSecondLevelCacheHitCount();

        Optional<ProcessedEventJpa> secondResult = transactionTemplate.execute(
                status -> repository.findById(databaseId)
        );

        // then
        assertThat(firstResult).isPresent();
        assertThat(secondResult).isPresent();

        assertThat(statistics.getSecondLevelCacheMissCount())
                .as("First read should miss second-level cache")
                .isGreaterThanOrEqualTo(1);

        assertThat(statistics.getSecondLevelCachePutCount())
                .as("First read should put processed event into second-level cache")
                .isGreaterThanOrEqualTo(1);

        assertThat(statistics.getPrepareStatementCount())
                .as("Second read by JPA id should not execute additional SQL")
                .isEqualTo(preparedStatementsAfterFirstRead);

        assertThat(statistics.getSecondLevelCacheHitCount())
                .as("Second read by JPA id should hit second-level cache")
                .isGreaterThan(secondLevelCacheHitsAfterFirstRead);
    }

    private Long persistProcessedEvent() {
        return transactionTemplate.execute(status -> {
            ProcessedEventJpa processedEvent = ProcessedEventJpa.of(
                    "customer.account-created.v1",
                    UUID.randomUUID().toString()
            );

            ProcessedEventJpa persisted = entityManager.persistAndFlush(processedEvent);
            return persisted.getId();
        });
    }
}
