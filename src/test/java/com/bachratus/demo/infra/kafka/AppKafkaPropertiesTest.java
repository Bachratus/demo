package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppKafkaPropertiesTest {

    @DisplayName("Tests for Kafka topic resolution")
    @Nested
    class TopicResolution {

        @Test
        void shouldEnableKafkaByDefault() {
            // given
            AppKafkaProperties properties = properties();

            // when & then
            assertThat(properties.enabled()).isTrue();
        }

        @Test
        void shouldAllowDisablingKafkaIntegration() {
            // given
            AppKafkaProperties properties = new AppKafkaProperties(
                    false,
                    producer(),
                    listener(),
                    outbox(),
                    Map.of("customer-account-created", topic())
            );

            // when & then
            assertThat(properties.enabled()).isFalse();
        }

        @Test
        void shouldReturnConfiguredTopicAndDltNames() {
            // given
            AppKafkaProperties properties = properties();

            // when & then
            assertThat(properties.topic("customer-account-created").name())
                    .isEqualTo("store.customer-account-created.v1");
            assertThat(properties.deadLetterTopicName("customer-account-created", "ignored"))
                    .isEqualTo("store.customer-account-created.v1.dlt");
            assertThat(properties.deadLetterTopicNameForTopic("store.customer-account-created.v1"))
                    .isEqualTo("store.customer-account-created.v1.dlt");
            assertThat(properties.topic("customer-account-created").eventType(1))
                    .isEqualTo("customer.account-created.v1");
            assertThat(properties.topic("customer-account-created").aggregateType())
                    .isEqualTo("customer");
        }

        @Test
        void shouldFallbackToConventionalDltNameWhenTopicIsUnknown() {
            // given
            AppKafkaProperties properties = properties();

            // when & then
            assertThat(properties.deadLetterTopicNameForTopic("external.topic"))
                    .isEqualTo("external.topic.dlt");
        }
    }

    @DisplayName("Tests for Kafka producer consistency")
    @Nested
    class ProducerConsistency {

        @Test
        void shouldRejectRequestTimeoutNotLowerThanDeliveryTimeout() {
            // when & then
            assertThatThrownBy(() -> new AppKafkaProperties.Producer(
                    15_000,
                    15_000,
                    5,
                    20_000
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requestTimeoutMs");
        }

        @Test
        void shouldRejectSendResultTimeoutNotGreaterThanDeliveryTimeout() {
            // when & then
            assertThatThrownBy(() -> new AppKafkaProperties(
                    true,
                    new AppKafkaProperties.Producer(45_000, 15_000, 5, 45_000),
                    listener(),
                    outbox(),
                    Map.of("customer-account-created", topic())
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sendResultTimeoutMs");
        }
    }

    private AppKafkaProperties properties() {
        return new AppKafkaProperties(
                true,
                producer(),
                listener(),
                outbox(),
                Map.of("customer-account-created", topic())
        );
    }

    private AppKafkaProperties.Producer producer() {
        return new AppKafkaProperties.Producer(45_000, 15_000, 5, 50_000);
    }

    private AppKafkaProperties.Listener listener() {
        return new AppKafkaProperties.Listener(1_000, 3);
    }

    private AppKafkaProperties.Outbox outbox() {
        return new AppKafkaProperties.Outbox(true, 100, 500, 10_000, 5);
    }

    private AppKafkaProperties.Topic topic() {
        return new AppKafkaProperties.Topic(
                "store.customer-account-created.v1",
                3,
                "store.customer-account-created.v1.dlt",
                "customer.account-created",
                "customer"
        );
    }
}
