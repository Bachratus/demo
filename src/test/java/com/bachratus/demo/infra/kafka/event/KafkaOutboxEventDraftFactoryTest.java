package com.bachratus.demo.infra.kafka.event;

import com.bachratus.demo.application.events.CustomerAccountCreatedEvent;
import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaOutboxEventDraftFactoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void shouldCreateOutboxEventDraftFromApplicationEventAndKafkaConfiguration() {
        // given
        KafkaOutboxEventDraftFactory factory = new KafkaOutboxEventDraftFactory(
                kafkaProperties(),
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        UUID customerId = UUID.randomUUID();
        CustomerAccountCreatedEvent event = new CustomerAccountCreatedEvent(
                1,
                customerId,
                "user-123",
                "Me"
        );

        // when
        OutboxEventDraft draft = factory.create(event);

        // then
        assertThat(draft.id()).isNotNull();
        assertThat(draft.topicKey()).isEqualTo("customer-account-created");
        assertThat(draft.aggregateType()).isEqualTo("customer");
        assertThat(draft.aggregateId()).isEqualTo(customerId.toString());
        assertThat(draft.eventType()).isEqualTo("customer.account-created.v1");
        assertThat(draft.occurredAt()).isEqualTo(NOW);
        assertThat(draft.payload().get("schemaVersion").asInt()).isEqualTo(1);
        assertThat(draft.payload().get("customerId").asText()).isEqualTo(customerId.toString());
        assertThat(draft.payload().get("userId").asText()).isEqualTo("user-123");
        assertThat(draft.payload().get("displayName").asText()).isEqualTo("Me");
        assertThat(draft.payload().has("eventKey")).isFalse();
        assertThat(draft.payload().has("aggregateId")).isFalse();
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
}
