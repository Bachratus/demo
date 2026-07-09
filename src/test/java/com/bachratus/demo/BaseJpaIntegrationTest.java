package com.bachratus.demo;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SuppressWarnings("resource")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DataJpaTest
@EnableJpaAuditing
public abstract class BaseJpaIntegrationTest {

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
