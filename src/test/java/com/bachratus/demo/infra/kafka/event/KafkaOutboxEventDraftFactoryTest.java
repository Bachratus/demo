package com.bachratus.demo.infra.kafka.event;

import com.bachratus.demo.application.events.CustomerAccountCreatedEvent;
import com.bachratus.demo.application.events.OutboxApplicationEvent;
import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class KafkaOutboxEventDraftFactoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private final ObjectMapper realObjectMapper = new ObjectMapper();

    @Test
    void shouldCreateDraftFromEventIdentityKafkaConfigurationSerializedPayloadAndClock() {
        // given
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        UUID customerId = UUID.randomUUID();
        CustomerAccountCreatedEvent event = new CustomerAccountCreatedEvent(
                2,
                customerId,
                "user-123",
                "Me"
        );
        ObjectNode payload = realObjectMapper.createObjectNode()
                .put("schemaVersion", 2)
                .put("customerId", customerId.toString())
                .put("userId", "user-123")
                .put("displayName", "Me");
        when(objectMapper.valueToTree(event)).thenReturn(payload);

        KafkaOutboxEventDraftFactory factory = factory(objectMapper, kafkaProperties());

        // when
        OutboxEventDraft draft = factory.create(event);

        // then
        assertThat(draft.id()).isNotNull();
        assertThat(draft.topicKey()).isEqualTo("customer-account-created");
        assertThat(draft.aggregateType()).isEqualTo("customer");
        assertThat(draft.aggregateId()).isEqualTo(customerId.toString());
        assertThat(draft.eventType()).isEqualTo("customer.account-created.v2");
        assertThat(draft.payload()).isEqualTo(payload);
        assertThat(draft.occurredAt()).isEqualTo(NOW);
        verify(objectMapper).valueToTree(event);
        verifyNoMoreInteractions(objectMapper);
    }

    @Test
    void shouldRejectNullEventBeforeReadingKafkaConfigurationOrSerializingPayload() {
        // given
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        KafkaOutboxEventDraftFactory factory = factory(objectMapper, kafkaProperties());

        // when & then
        assertThatThrownBy(() -> factory.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event cannot be null");
        verifyNoMoreInteractions(objectMapper);
    }

    @Test
    void shouldFailWhenEventKeyHasNoKafkaTopicConfiguration() {
        // given
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        OutboxApplicationEvent event = event();
        KafkaOutboxEventDraftFactory factory = factory(objectMapper, kafkaProperties());

        // when & then
        assertThatThrownBy(() -> factory.create(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing Kafka topic configuration for key: external-event");
        verifyNoMoreInteractions(objectMapper);
    }

    private KafkaOutboxEventDraftFactory factory(
            ObjectMapper objectMapper,
            AppKafkaProperties kafkaProperties
    ) {
        return new KafkaOutboxEventDraftFactory(
                kafkaProperties,
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private AppKafkaProperties kafkaProperties() {
        return new AppKafkaProperties(
                true,
                new AppKafkaProperties.Producer(45_000, 15_000, 5, 50_000),
                new AppKafkaProperties.Listener(1_000, 3),
                new AppKafkaProperties.Outbox(true, 100, 500, 10_000, 5),
                Map.of(
                        "customer-account-created",
                        new AppKafkaProperties.Topic(
                                "store.customer-account-created.v1",
                                3,
                                "store.customer-account-created.v1.dlt",
                                "customer.account-created",
                                "customer"
                        )
                )
        );
    }

    private OutboxApplicationEvent event(
    ) {
        return new OutboxApplicationEvent() {
            @Override
            public String eventKey() {
                return "external-event";
            }

            @Override
            public String aggregateId() {
                return "customer-1";
            }

            @Override
            public int schemaVersion() {
                return 1;
            }
        };
    }
}
