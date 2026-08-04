package com.bachratus.demo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestContainerConfiguration {

    private static final int REDIS_PORT = 6379;

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        //noinspection resource
        return new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(REDIS_PORT);
    }

    @Bean
    HibernatePropertiesCustomizer redissonHibernatePropertiesCustomizer(
            @Qualifier("redisContainer") GenericContainer<?> redisContainer
    ) {
        return properties -> {
            properties.put("hibernate.cache.redisson.config", getRedissonConfigPath(redisContainer));
            properties.put("hibernate.cache.redisson.fallback", "false");
        };
    }

    private static String getRedissonConfigPath(GenericContainer<?> redisContainer) {
        try {
            Path config = Files.createTempFile("redisson-test-", ".yaml");

            Files.writeString(
                    config,
                    """
                    singleServerConfig:
                      address: "redis://%s:%d"
                    """.formatted(
                            redisContainer.getHost(),
                            redisContainer.getMappedPort(REDIS_PORT)
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
}
