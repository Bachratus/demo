package com.bachratus.demo.config;

import com.bachratus.demo.infra.db.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaAuditingConfiguration.class,
        ControlledClockTestConfiguration.class
})
public abstract class BaseJpaCacheIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("testdb")
                    .withUsername("user")
                    .withPassword("pass");

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add(
                "spring.jpa.properties.hibernate.cache.redisson.config",
                BaseJpaCacheIntegrationTest::getRedissonConfigPath
        );

        registry.add(
                "spring.jpa.properties.hibernate.cache.redisson.fallback",
                () -> false
        );

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add(
                "spring.data.redis.port",
                () -> redis.getMappedPort(6379)
        );
    }

    private static String getRedissonConfigPath() {
        try {
            Path config = Files.createTempFile("redisson-test-", ".yaml");

            Files.writeString(
                    config,
                    """
                    singleServerConfig:
                      address: "redis://%s:%d"
                    """.formatted(
                            redis.getHost(),
                            redis.getMappedPort(6379)
                    )
            );

            config.toFile().deleteOnExit();
            return config.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot create Redisson test configuration",
                    exception
            );
        }
    }

    @Autowired
    protected ControlledClockTestConfiguration.MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.setInstant(Instant.parse("2026-01-01T00:00:00Z"));
        SecurityContextHolder.clearContext();
    }
}