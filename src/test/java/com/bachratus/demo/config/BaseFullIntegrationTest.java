package com.bachratus.demo.config;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@Import({
        PostgresTestContainerConfiguration.class,
        RedisTestContainerConfiguration.class,
        KafkaTestContainerConfiguration.class,
        KafkaTopicsTestConfiguration.class
})
public abstract class BaseFullIntegrationTest {
}
