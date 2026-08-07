package com.bachratus.demo.infra.kafka.config;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AppKafkaPropertiesTest {

    @DisplayName("Tests for defaults and topic map defensive behavior")
    @Nested
    class DefaultsAndTopicMap {

        @Test
        void shouldUseSafeDefaultsWhenOptionalConfigurationSectionsAreMissing() {
            // when
            AppKafkaProperties properties = new AppKafkaProperties(
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // then
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.producer()).isEqualTo(new AppKafkaProperties.Producer(45_000, 15_000, 5, 50_000));
            assertThat(properties.listener()).isEqualTo(new AppKafkaProperties.Listener(1_000, 3));
            assertThat(properties.outbox()).isEqualTo(new AppKafkaProperties.Outbox(true, 100, 500, 10_000, 5));
            assertThat(properties.topics()).isEmpty();
        }

        @Test
        void shouldAllowDisablingKafkaAndOutboxIntegration() {
            // given
            AppKafkaProperties properties = new AppKafkaProperties(
                    false,
                    producer(),
                    listener(),
                    new AppKafkaProperties.Outbox(false, 100, 500, 10_000, 5),
                    Map.of("customer-account-created", topic())
            );

            // when & then
            assertThat(properties.enabled()).isFalse();
            assertThat(properties.outbox().enabled()).isFalse();
        }

        @Test
        void shouldDefensivelyCopyAndExposeUnmodifiableTopics() {
            // given
            Map<String, AppKafkaProperties.Topic> source = new LinkedHashMap<>();
            source.put("customer-account-created", topic());

            // when
            AppKafkaProperties properties = new AppKafkaProperties(
                    true,
                    producer(),
                    listener(),
                    outbox(),
                    source
            );
            source.put("other-event", otherTopic());

            // then
            assertThat(properties.topics()).containsOnlyKeys("customer-account-created");
            assertThatThrownBy(() -> properties.topics().put("new-event", otherTopic()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldNormalizeTopicKeysAndTopicMetadata() {
            // given
            Map<String, AppKafkaProperties.Topic> topics = new LinkedHashMap<>();
            topics.put(" customer-account-created ", new AppKafkaProperties.Topic(
                    " store.customer-account-created.v1 ",
                    3,
                    " store.customer-account-created.v1.dlt ",
                    " customer.account-created ",
                    " customer "
            ));
            AppKafkaProperties properties = new AppKafkaProperties(
                    true,
                    producer(),
                    listener(),
                    outbox(),
                    topics
            );

            // when
            AppKafkaProperties.Topic topic = properties.topic(" customer-account-created ");

            // then
            assertThat(topic.name()).isEqualTo("store.customer-account-created.v1");
            assertThat(topic.dltName()).isEqualTo("store.customer-account-created.v1.dlt");
            assertThat(topic.eventType()).isEqualTo("customer.account-created");
            assertThat(topic.eventType(2)).isEqualTo("customer.account-created.v2");
            assertThat(topic.aggregateType()).isEqualTo("customer");
            assertThat(properties.deadLetterTopicName(" customer-account-created "))
                    .isEqualTo("store.customer-account-created.v1.dlt");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.infra.kafka.config.AppKafkaPropertiesTest#invalidTopicMaps")
        void shouldRejectInvalidTopicMapEntries(
                String caseName,
                ThrowingCallable action,
                Class<? extends Throwable> expectedType,
                String expectedMessage
        ) {
            assertThat(caseName).isNotBlank();

            assertThatThrownBy(action)
                    .isInstanceOf(expectedType)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @DisplayName("Tests for Kafka topic resolution")
    @Nested
    class TopicResolution {

        @Test
        void shouldReturnConfiguredTopicAndDltNames() {
            // given
            AppKafkaProperties properties = properties();

            // when & then
            assertThat(properties.topic("customer-account-created").name())
                    .isEqualTo("store.customer-account-created.v1");
            assertThat(properties.deadLetterTopicName("customer-account-created"))
                    .isEqualTo("store.customer-account-created.v1.dlt");
            assertThat(properties.deadLetterTopicNameForTopic("store.customer-account-created.v1"))
                    .isEqualTo("store.customer-account-created.v1.dlt");
            assertThat(properties.topic("customer-account-created").eventType(1))
                    .isEqualTo("customer.account-created.v1");
            assertThat(properties.topic("customer-account-created").aggregateType())
                    .isEqualTo("customer");
        }

        @Test
        void shouldRejectUnknownLogicalTopicKey() {
            // given
            AppKafkaProperties properties = properties();

            // when & then
            assertThatThrownBy(() -> properties.topic("external-event"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Missing Kafka topic configuration for key: external-event");
            assertThatThrownBy(() -> properties.deadLetterTopicName("external-event"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Missing Kafka topic configuration for key: external-event");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.infra.kafka.config.AppKafkaPropertiesTest#invalidLogicalTopicKeys")
        void shouldRejectInvalidLogicalTopicKey(
                String caseName,
                ThrowingCallable action,
                Class<? extends Throwable> expectedType,
                String expectedMessage
        ) {
            assertThat(caseName).isNotBlank();

            assertThatThrownBy(action)
                    .isInstanceOf(expectedType)
                    .hasMessageContaining(expectedMessage);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.infra.kafka.config.AppKafkaPropertiesTest#invalidPhysicalTopicNames")
        void shouldRejectInvalidPhysicalTopicNameResolution(
                String caseName,
                ThrowingCallable action,
                Class<? extends Throwable> expectedType,
                String expectedMessage
        ) {
            assertThat(caseName).isNotBlank();

            assertThatThrownBy(action)
                    .isInstanceOf(expectedType)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @DisplayName("Tests for Kafka producer validation")
    @Nested
    class ProducerValidation {

        @Test
        void shouldAcceptProducerBoundaryValues() {
            // when
            AppKafkaProperties.Producer producer = new AppKafkaProperties.Producer(2, 1, 0, 3);

            // then
            assertThat(producer.deliveryTimeoutMs()).isEqualTo(2);
            assertThat(producer.requestTimeoutMs()).isEqualTo(1);
            assertThat(producer.lingerMs()).isZero();
            assertThat(producer.sendResultTimeoutMs()).isEqualTo(3);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.infra.kafka.config.AppKafkaPropertiesTest#invalidProducerSettings")
        void shouldRejectInvalidProducerSettings(
                String caseName,
                ThrowingCallable action,
                String expectedMessage
        ) {
            assertThat(caseName).isNotBlank();

            assertThatThrownBy(action)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @DisplayName("Tests for Kafka listener validation")
    @Nested
    class ListenerValidation {

        @Test
        void shouldAcceptListenerBoundaryValues() {
            // when
            AppKafkaProperties.Listener listener = new AppKafkaProperties.Listener(0, 1);

            // then
            assertThat(listener.retryIntervalMs()).isZero();
            assertThat(listener.maxAttempts()).isEqualTo(1);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.infra.kafka.config.AppKafkaPropertiesTest#invalidListenerSettings")
        void shouldRejectInvalidListenerSettings(
                String caseName,
                ThrowingCallable action,
                String expectedMessage
        ) {
            assertThat(caseName).isNotBlank();

            assertThatThrownBy(action)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @DisplayName("Tests for Kafka outbox validation")
    @Nested
    class OutboxValidation {

        @Test
        void shouldAcceptOutboxBoundaryValues() {
            // when
            AppKafkaProperties.Outbox outbox = new AppKafkaProperties.Outbox(false, 1, 1, 0, 1);

            // then
            assertThat(outbox.enabled()).isFalse();
            assertThat(outbox.batchSize()).isEqualTo(1);
            assertThat(outbox.pollDelayMs()).isEqualTo(1);
            assertThat(outbox.retryBackoffMs()).isZero();
            assertThat(outbox.maxAttempts()).isEqualTo(1);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.infra.kafka.config.AppKafkaPropertiesTest#invalidOutboxSettings")
        void shouldRejectInvalidOutboxSettings(
                String caseName,
                ThrowingCallable action,
                String expectedMessage
        ) {
            assertThat(caseName).isNotBlank();

            assertThatThrownBy(action)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @DisplayName("Tests for Kafka topic validation")
    @Nested
    class TopicValidation {

        @Test
        void shouldAcceptTopicBoundaryValues() {
            // when
            AppKafkaProperties.Topic topic = new AppKafkaProperties.Topic(
                    "store.customer-account-created.v1",
                    1,
                    "store.customer-account-created.v1.dlt",
                    "customer.account-created",
                    "customer"
            );

            // then
            assertThat(topic.concurrency()).isEqualTo(1);
            assertThat(topic.eventType(1)).isEqualTo("customer.account-created.v1");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.infra.kafka.config.AppKafkaPropertiesTest#invalidTopicDefinitions")
        void shouldRejectInvalidTopicDefinitions(
                String caseName,
                ThrowingCallable action,
                Class<? extends Throwable> expectedType,
                String expectedMessage
        ) {
            assertThat(caseName).isNotBlank();

            assertThatThrownBy(action)
                    .isInstanceOf(expectedType)
                    .hasMessageContaining(expectedMessage);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.infra.kafka.config.AppKafkaPropertiesTest#invalidSchemaVersions")
        void shouldRejectInvalidSchemaVersion(
                String caseName,
                ThrowingCallable action,
                String expectedMessage
        ) {
            assertThat(caseName).isNotBlank();

            assertThatThrownBy(action)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    private static Stream<Arguments> invalidTopicMaps() {
        return Stream.of(
                arguments(
                        "null topic map key",
                        (ThrowingCallable) () -> new AppKafkaProperties(
                                true,
                                producer(),
                                listener(),
                                outbox(),
                                topicMap(null, topic())
                        ),
                        NullPointerException.class,
                        "Kafka topic key cannot be null"
                ),
                arguments(
                        "blank topic map key",
                        (ThrowingCallable) () -> new AppKafkaProperties(
                                true,
                                producer(),
                                listener(),
                                outbox(),
                                topicMap("   ", topic())
                        ),
                        IllegalArgumentException.class,
                        "Kafka topic key cannot be blank"
                ),
                arguments(
                        "null topic map value",
                        (ThrowingCallable) () -> new AppKafkaProperties(
                                true,
                                producer(),
                                listener(),
                                outbox(),
                                topicMap("customer-account-created", null)
                        ),
                        NullPointerException.class,
                        "Kafka topic configuration for key customer-account-created cannot be null"
                ),
                arguments(
                        "duplicate topic map key after trimming",
                        (ThrowingCallable) () -> new AppKafkaProperties(
                                true,
                                producer(),
                                listener(),
                                outbox(),
                                duplicateTopicKeyMap()
                        ),
                        IllegalArgumentException.class,
                        "Duplicate Kafka topic configuration key: customer-account-created"
                )
        );
    }

    private static Stream<Arguments> invalidLogicalTopicKeys() {
        AppKafkaProperties properties = properties();

        return Stream.of(
                arguments(
                        "null logical topic key",
                        (ThrowingCallable) () -> properties.topic(null),
                        NullPointerException.class,
                        "Kafka topic key cannot be null"
                ),
                arguments(
                        "blank logical topic key",
                        (ThrowingCallable) () -> properties.topic("   "),
                        IllegalArgumentException.class,
                        "Kafka topic key cannot be blank"
                ),
                arguments(
                        "null logical DLT topic key",
                        (ThrowingCallable) () -> properties.deadLetterTopicName(null),
                        NullPointerException.class,
                        "Kafka topic key cannot be null"
                ),
                arguments(
                        "blank logical DLT topic key",
                        (ThrowingCallable) () -> properties.deadLetterTopicName("   "),
                        IllegalArgumentException.class,
                        "Kafka topic key cannot be blank"
                )
        );
    }

    private static Stream<Arguments> invalidPhysicalTopicNames() {
        AppKafkaProperties properties = properties();

        return Stream.of(
                arguments(
                        "null physical topic name",
                        (ThrowingCallable) () -> properties.deadLetterTopicNameForTopic(null),
                        NullPointerException.class,
                        "topicName cannot be null"
                ),
                arguments(
                        "blank physical topic name",
                        (ThrowingCallable) () -> properties.deadLetterTopicNameForTopic("   "),
                        IllegalArgumentException.class,
                        "topicName cannot be blank"
                ),
                arguments(
                        "unknown physical topic name",
                        (ThrowingCallable) () -> properties.deadLetterTopicNameForTopic("external.topic"),
                        IllegalArgumentException.class,
                        "Missing Kafka topic configuration for topic: external.topic"
                )
        );
    }

    private static Stream<Arguments> invalidProducerSettings() {
        return Stream.of(
                arguments(
                        "non-positive delivery timeout",
                        (ThrowingCallable) () -> new AppKafkaProperties.Producer(0, 1, 0, 2),
                        "deliveryTimeoutMs must be positive"
                ),
                arguments(
                        "non-positive request timeout",
                        (ThrowingCallable) () -> new AppKafkaProperties.Producer(2, 0, 0, 3),
                        "requestTimeoutMs must be positive"
                ),
                arguments(
                        "negative linger",
                        (ThrowingCallable) () -> new AppKafkaProperties.Producer(2, 1, -1, 3),
                        "lingerMs cannot be negative"
                ),
                arguments(
                        "non-positive send result timeout",
                        (ThrowingCallable) () -> new AppKafkaProperties.Producer(2, 1, 0, 0),
                        "sendResultTimeoutMs must be positive"
                ),
                arguments(
                        "request timeout equal to delivery timeout",
                        (ThrowingCallable) () -> new AppKafkaProperties.Producer(2, 2, 0, 3),
                        "requestTimeoutMs must be lower than deliveryTimeoutMs"
                ),
                arguments(
                        "request timeout greater than delivery timeout",
                        (ThrowingCallable) () -> new AppKafkaProperties.Producer(2, 3, 0, 4),
                        "requestTimeoutMs must be lower than deliveryTimeoutMs"
                ),
                arguments(
                        "send result timeout equal to delivery timeout",
                        (ThrowingCallable) () -> new AppKafkaProperties.Producer(2, 1, 0, 2),
                        "sendResultTimeoutMs must be greater than deliveryTimeoutMs"
                ),
                arguments(
                        "send result timeout lower than delivery timeout",
                        (ThrowingCallable) () -> new AppKafkaProperties.Producer(3, 1, 0, 2),
                        "sendResultTimeoutMs must be greater than deliveryTimeoutMs"
                )
        );
    }

    private static Stream<Arguments> invalidListenerSettings() {
        return Stream.of(
                arguments(
                        "negative retry interval",
                        (ThrowingCallable) () -> new AppKafkaProperties.Listener(-1, 1),
                        "retryIntervalMs cannot be negative"
                ),
                arguments(
                        "non-positive max attempts",
                        (ThrowingCallable) () -> new AppKafkaProperties.Listener(0, 0),
                        "maxAttempts must be positive"
                )
        );
    }

    private static Stream<Arguments> invalidOutboxSettings() {
        return Stream.of(
                arguments(
                        "non-positive batch size",
                        (ThrowingCallable) () -> new AppKafkaProperties.Outbox(true, 0, 1, 0, 1),
                        "batchSize must be positive"
                ),
                arguments(
                        "non-positive poll delay",
                        (ThrowingCallable) () -> new AppKafkaProperties.Outbox(true, 1, 0, 0, 1),
                        "pollDelayMs must be positive"
                ),
                arguments(
                        "negative retry backoff",
                        (ThrowingCallable) () -> new AppKafkaProperties.Outbox(true, 1, 1, -1, 1),
                        "retryBackoffMs cannot be negative"
                ),
                arguments(
                        "non-positive max attempts",
                        (ThrowingCallable) () -> new AppKafkaProperties.Outbox(true, 1, 1, 0, 0),
                        "maxAttempts must be positive"
                )
        );
    }

    private static Stream<Arguments> invalidTopicDefinitions() {
        return Stream.of(
                arguments(
                        "null topic name",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                null,
                                1,
                                "store.customer-account-created.v1.dlt",
                                "customer.account-created",
                                "customer"
                        ),
                        NullPointerException.class,
                        "Kafka topic name cannot be null"
                ),
                arguments(
                        "blank topic name",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                "   ",
                                1,
                                "store.customer-account-created.v1.dlt",
                                "customer.account-created",
                                "customer"
                        ),
                        IllegalArgumentException.class,
                        "Kafka topic name cannot be blank"
                ),
                arguments(
                        "non-positive concurrency",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                "store.customer-account-created.v1",
                                0,
                                "store.customer-account-created.v1.dlt",
                                "customer.account-created",
                                "customer"
                        ),
                        IllegalArgumentException.class,
                        "Kafka topic concurrency must be positive"
                ),
                arguments(
                        "null dead-letter topic name",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                "store.customer-account-created.v1",
                                1,
                                null,
                                "customer.account-created",
                                "customer"
                        ),
                        NullPointerException.class,
                        "Kafka dead-letter topic name cannot be null"
                ),
                arguments(
                        "blank dead-letter topic name",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                "store.customer-account-created.v1",
                                1,
                                "   ",
                                "customer.account-created",
                                "customer"
                        ),
                        IllegalArgumentException.class,
                        "Kafka dead-letter topic name cannot be blank"
                ),
                arguments(
                        "dead-letter topic name equal to main topic name after trimming",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                "store.customer-account-created.v1",
                                1,
                                " store.customer-account-created.v1 ",
                                "customer.account-created",
                                "customer"
                        ),
                        IllegalArgumentException.class,
                        "Kafka dead-letter topic name must be different from topic name"
                ),
                arguments(
                        "null event type",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                "store.customer-account-created.v1",
                                1,
                                "store.customer-account-created.v1.dlt",
                                null,
                                "customer"
                        ),
                        NullPointerException.class,
                        "Kafka event type cannot be null"
                ),
                arguments(
                        "blank event type",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                "store.customer-account-created.v1",
                                1,
                                "store.customer-account-created.v1.dlt",
                                "   ",
                                "customer"
                        ),
                        IllegalArgumentException.class,
                        "Kafka event type cannot be blank"
                ),
                arguments(
                        "null aggregate type",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                "store.customer-account-created.v1",
                                1,
                                "store.customer-account-created.v1.dlt",
                                "customer.account-created",
                                null
                        ),
                        NullPointerException.class,
                        "Kafka aggregate type cannot be null"
                ),
                arguments(
                        "blank aggregate type",
                        (ThrowingCallable) () -> new AppKafkaProperties.Topic(
                                "store.customer-account-created.v1",
                                1,
                                "store.customer-account-created.v1.dlt",
                                "customer.account-created",
                                "   "
                        ),
                        IllegalArgumentException.class,
                        "Kafka aggregate type cannot be blank"
                )
        );
    }

    private static Stream<Arguments> invalidSchemaVersions() {
        AppKafkaProperties.Topic topic = topic();

        return Stream.of(
                arguments(
                        "zero schema version",
                        (ThrowingCallable) () -> topic.eventType(0),
                        "schemaVersion must be positive"
                ),
                arguments(
                        "negative schema version",
                        (ThrowingCallable) () -> topic.eventType(-1),
                        "schemaVersion must be positive"
                )
        );
    }

    private static AppKafkaProperties properties() {
        return new AppKafkaProperties(
                true,
                producer(),
                listener(),
                outbox(),
                Map.of("customer-account-created", topic())
        );
    }

    private static AppKafkaProperties.Producer producer() {
        return new AppKafkaProperties.Producer(45_000, 15_000, 5, 50_000);
    }

    private static AppKafkaProperties.Listener listener() {
        return new AppKafkaProperties.Listener(1_000, 3);
    }

    private static AppKafkaProperties.Outbox outbox() {
        return new AppKafkaProperties.Outbox(true, 100, 500, 10_000, 5);
    }

    private static AppKafkaProperties.Topic topic() {
        return new AppKafkaProperties.Topic(
                "store.customer-account-created.v1",
                3,
                "store.customer-account-created.v1.dlt",
                "customer.account-created",
                "customer"
        );
    }

    private static AppKafkaProperties.Topic otherTopic() {
        return new AppKafkaProperties.Topic(
                "store.other-event.v1",
                1,
                "store.other-event.v1.dlt",
                "other.event",
                "other"
        );
    }

    private static Map<String, AppKafkaProperties.Topic> topicMap(
            String key,
            AppKafkaProperties.Topic topic
    ) {
        Map<String, AppKafkaProperties.Topic> topics = new LinkedHashMap<>();
        topics.put(key, topic);
        return topics;
    }

    private static Map<String, AppKafkaProperties.Topic> duplicateTopicKeyMap() {
        Map<String, AppKafkaProperties.Topic> topics = new LinkedHashMap<>();
        topics.put("customer-account-created", topic());
        topics.put(" customer-account-created ", otherTopic());
        return topics;
    }
}
