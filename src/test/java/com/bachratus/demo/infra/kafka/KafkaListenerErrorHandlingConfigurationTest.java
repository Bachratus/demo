package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaListenerErrorHandlingConfigurationTest {

    @Test
    void shouldCreateDefaultErrorHandlerWithDeadLetterRecoverer() {
        // given
        AppKafkaProperties properties = new AppKafkaProperties(
                new AppKafkaProperties.Producer(45_000, 15_000, 5, 50_000),
                new AppKafkaProperties.Listener(1_000, 3),
                new AppKafkaProperties.Outbox(true, 100, 500, 10_000, 5),
                Map.of(
                        "customer-account-created",
                        new AppKafkaProperties.Topic(
                                "demo.customer-account-created.v1",
                                3,
                                "demo.customer-account-created.v1.dlt"
                        )
                )
        );

        KafkaListenerErrorHandlingConfiguration configuration =
                new KafkaListenerErrorHandlingConfiguration(properties);

        // when
        DefaultErrorHandler errorHandler = configuration.kafkaDefaultErrorHandler(
                mock(KafkaTemplate.class),
                new KafkaDeadLetterTopicResolver(properties)
        );

        // then
        assertThat(errorHandler).isNotNull();
    }
}
