package com.bachratus.demo.config;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@Import({
        PostgresTestContainerConfiguration.class,
        RedisTestContainerConfiguration.class,
        KafkaTestContainerConfiguration.class,
        KafkaTopicsTestConfiguration.class
})
public abstract class BaseFullIntegrationTest {

    @Autowired
    protected ControlledClockTestConfiguration.MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.setInstant(Instant.parse("2026-01-01T00:00:00Z"));
        SecurityContextHolder.clearContext();
    }
}
