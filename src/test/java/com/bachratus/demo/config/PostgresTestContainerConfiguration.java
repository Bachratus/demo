package com.bachratus.demo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainerConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        //noinspection resource
        return new PostgreSQLContainer<>("postgres:17")
                .withDatabaseName("testdb")
                .withUsername("user")
                .withPassword("pass");
    }
}
