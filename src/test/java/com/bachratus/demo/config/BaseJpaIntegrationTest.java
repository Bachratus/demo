package com.bachratus.demo.config;

import com.bachratus.demo.infra.db.config.JpaAuditingConfiguration;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

@SuppressWarnings("resource")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DataJpaTest
@Import({JpaAuditingConfiguration.class, ControlledClockTestConfiguration.class})
public abstract class BaseJpaIntegrationTest {

    @Autowired
    protected ControlledClockTestConfiguration.MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.setInstant(Instant.parse("2026-01-01T00:00:00Z"));
        SecurityContextHolder.clearContext();
    }

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("pass");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.jpa.properties.hibernate.cache.use_second_level_cache", () -> false);
        registry.add("spring.jpa.properties.hibernate.cache.use_query_cache", () -> false);
        registry.add(
                "spring.jpa.properties.hibernate.cache.region.factory_class",
                NoCachingRegionFactory.class::getName
        );
    }
}
