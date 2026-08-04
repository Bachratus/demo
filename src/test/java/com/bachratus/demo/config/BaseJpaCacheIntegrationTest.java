package com.bachratus.demo.config;

import com.bachratus.demo.infra.db.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaAuditingConfiguration.class,
        ControlledClockTestConfiguration.class,
        PostgresTestContainerConfiguration.class,
        RedisTestContainerConfiguration.class
})
public abstract class BaseJpaCacheIntegrationTest {

    @Autowired
    protected ControlledClockTestConfiguration.MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.setInstant(Instant.parse("2026-01-01T00:00:00Z"));
        SecurityContextHolder.clearContext();
    }
}
