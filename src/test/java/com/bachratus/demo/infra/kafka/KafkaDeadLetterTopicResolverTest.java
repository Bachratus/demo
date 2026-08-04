package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaDeadLetterTopicResolverTest {

    private final KafkaDeadLetterTopicResolver resolver = new KafkaDeadLetterTopicResolver(properties());

    @DisplayName("Tests for resolve(ConsumerRecord, Exception) method")
    @Nested
    class Resolve {

        @Test
        void shouldResolveConfiguredDltTopicAndKeepOriginalPartition() {
            // given
            ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                    "demo.customer-account-created.v1",
                    4,
                    10L,
                    "customer-123",
                    new Object()
            );

            // when
            TopicPartition result = resolver.resolve(record, new RuntimeException("boom"));

            // then
            assertThat(result.topic()).isEqualTo("demo.customer-account-created.v1.dlt");
            assertThat(result.partition()).isEqualTo(4);
        }

        @Test
        void shouldFallbackToConventionalDltTopicWhenTopicIsUnknown() {
            // given
            ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                    "external.topic",
                    1,
                    10L,
                    "key",
                    new Object()
            );

            // when
            TopicPartition result = resolver.resolve(record, new RuntimeException("boom"));

            // then
            assertThat(result.topic()).isEqualTo("external.topic.dlt");
            assertThat(result.partition()).isEqualTo(1);
        }
    }

    private AppKafkaProperties properties() {
        return new AppKafkaProperties(
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
    }
}
