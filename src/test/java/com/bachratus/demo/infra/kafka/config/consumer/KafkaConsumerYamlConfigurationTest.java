package com.bachratus.demo.infra.kafka.config.consumer;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerYamlConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldWrapKeyAndValueDeserializersWithErrorHandlingDeserializer() {
        contextRunner.run(context -> {
            KafkaProperties.Consumer consumer = context.getBean(KafkaProperties.class).getConsumer();

            assertThat(consumer.getKeyDeserializer()).isEqualTo(ErrorHandlingDeserializer.class);
            assertThat(consumer.getValueDeserializer()).isEqualTo(ErrorHandlingDeserializer.class);
            assertThat(consumer.getProperties())
                    .containsEntry(
                            "spring.deserializer.key.delegate.class",
                            StringDeserializer.class.getName()
                    )
                    .containsEntry(
                            "spring.deserializer.value.delegate.class",
                            JsonDeserializer.class.getName()
                    );
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(KafkaProperties.class)
    static class TestConfiguration {
    }
}
