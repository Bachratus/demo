package com.bachratus.demo.infra.db.customer;

import com.bachratus.demo.config.BaseJpaCacheIntegrationTest;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.UserId;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
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

@Import({CustomerRepositoryAdapter.class, CustomerMapper.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CustomerJpaCacheTest extends BaseJpaCacheIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private CustomerJpaRepository customerJpaRepository;

    @Autowired
    private CustomerRepositoryAdapter adapter;

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
    void shouldUseSecondLevelCacheWhenCustomerIsLoadedByJpaIdTwice() {
        // given
        Long databaseId = persistCustomerJpa(
                UUID.randomUUID(),
                uniqueUserId("jpa-id")
        );

        sessionFactory.getCache().evictAllRegions();
        statistics.clear();

        // when
        Optional<CustomerJpa> firstResult = transactionTemplate.execute(
                status -> customerJpaRepository.findById(databaseId)
        );

        long preparedStatementsAfterFirstRead = statistics.getPrepareStatementCount();
        long secondLevelCacheHitsAfterFirstRead = statistics.getSecondLevelCacheHitCount();

        Optional<CustomerJpa> secondResult = transactionTemplate.execute(
                status -> customerJpaRepository.findById(databaseId)
        );

        // then
        assertThat(firstResult).isPresent();
        assertThat(secondResult).isPresent();

        assertThat(statistics.getSecondLevelCacheMissCount())
                .as("First read should miss second-level cache")
                .isGreaterThanOrEqualTo(1);

        assertThat(statistics.getSecondLevelCachePutCount())
                .as("First read should put customer into second-level cache")
                .isGreaterThanOrEqualTo(1);

        assertThat(statistics.getPrepareStatementCount())
                .as("Second read by JPA id should not execute additional SQL")
                .isEqualTo(preparedStatementsAfterFirstRead);

        assertThat(statistics.getSecondLevelCacheHitCount())
                .as("Second read by JPA id should hit second-level cache")
                .isGreaterThan(secondLevelCacheHitsAfterFirstRead);
    }

    @Test
    void shouldUseCacheWhenCustomerIsFoundByPublicIdThroughAdapter() {
        // given
        UUID publicId = UUID.randomUUID();

        persistCustomerJpa(
                publicId,
                uniqueUserId("public-id")
        );

        sessionFactory.getCache().evictAllRegions();
        statistics.clear();

        // when
        Optional<Customer> firstResult = transactionTemplate.execute(
                status -> adapter.findById(CustomerId.of(publicId))
        );

        long preparedStatementsAfterFirstRead = statistics.getPrepareStatementCount();
        long queryCacheHitsAfterFirstRead = statistics.getQueryCacheHitCount();

        Optional<Customer> secondResult = transactionTemplate.execute(
                status -> adapter.findById(CustomerId.of(publicId))
        );

        // then
        assertThat(firstResult).isPresent();
        assertThat(secondResult).isPresent();

        assertThat(statistics.getQueryCacheMissCount())
                .as("First read should miss query cache")
                .isGreaterThanOrEqualTo(1);

        assertThat(statistics.getQueryCachePutCount())
                .as("First read should put customer into query cache")
                .isGreaterThanOrEqualTo(1);

        assertThat(statistics.getPrepareStatementCount())
                .as("Second read by publicId should not execute additional SQL")
                .isEqualTo(preparedStatementsAfterFirstRead);

        assertThat(statistics.getQueryCacheHitCount())
                .as("Second read by publicId should hit query cache")
                .isGreaterThan(queryCacheHitsAfterFirstRead);
    }

    @Test
    void shouldUseCacheWhenCustomerIsFoundByUserIdThroughAdapter() {
        // given
        UUID publicId = UUID.randomUUID();
        String userId = uniqueUserId("user-id");

        persistCustomerJpa(
                publicId,
                userId
        );

        sessionFactory.getCache().evictAllRegions();
        statistics.clear();

        // when
        Optional<Customer> firstResult = transactionTemplate.execute(
                status -> adapter.findByUserId(UserId.of(userId))
        );

        long preparedStatementsAfterFirstRead = statistics.getPrepareStatementCount();
        long queryCacheHitsAfterFirstRead = statistics.getQueryCacheHitCount();

        Optional<Customer> secondResult = transactionTemplate.execute(
                status -> adapter.findByUserId(UserId.of(userId))
        );

        // then
        assertThat(firstResult).isPresent();
        assertThat(secondResult).isPresent();

        assertThat(statistics.getQueryCacheMissCount())
                .as("First read should miss query cache")
                .isGreaterThanOrEqualTo(1);

        assertThat(statistics.getQueryCachePutCount())
                .as("First read should put customer into query cache")
                .isGreaterThanOrEqualTo(1);

        assertThat(statistics.getPrepareStatementCount())
                .as("Second read by userId should not execute additional SQL")
                .isEqualTo(preparedStatementsAfterFirstRead);

        assertThat(statistics.getQueryCacheHitCount())
                .as("Second read by userId should hit query cache")
                .isGreaterThan(queryCacheHitsAfterFirstRead);
    }

    private Long persistCustomerJpa(UUID publicId, String userId) {
        return transactionTemplate.execute(status -> {
            CustomerJpa customerJpa = new CustomerJpa();
            customerJpa.setPublicId(publicId);
            customerJpa.setUserId(userId);
            customerJpa.setDisplayName("Me");

            CustomerJpa persisted = entityManager.persistAndFlush(customerJpa);
            return persisted.getId();
        });
    }

    private String uniqueUserId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}