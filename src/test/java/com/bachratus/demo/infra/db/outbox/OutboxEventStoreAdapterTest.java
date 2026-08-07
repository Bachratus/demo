package com.bachratus.demo.infra.db.outbox;

import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class OutboxEventStoreAdapterTest {

    private static final UUID EVENT_ID = UUID.fromString("7ab06985-44a9-4964-b616-dd0b264f31e4");
    private static final String TOPIC_KEY = "customer-account-created";
    private static final String TOPIC_NAME = "store.customer-account-created.v1";
    private static final String AGGREGATE_TYPE = "customer";
    private static final String AGGREGATE_ID = "c390a411-b177-4628-8058-64cd3cb7bf93";
    private static final String EVENT_TYPE = "customer.account-created.v1";
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void shouldPersistOutboxEventUsingResolvedTopicNameAndClock() {
        // given
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        AppKafkaProperties kafkaProperties = mock(AppKafkaProperties.class);
        AppKafkaProperties.Topic topic = mock(AppKafkaProperties.Topic.class);
        when(kafkaProperties.topic(TOPIC_KEY)).thenReturn(topic);
        when(topic.name()).thenReturn(TOPIC_NAME);

        OutboxEventStoreAdapter adapter = adapter(repository, kafkaProperties);
        OutboxEventDraft draft = draft(TOPIC_KEY);

        // when
        adapter.append(draft);

        // then
        ArgumentCaptor<OutboxEventJpa> savedEvent = ArgumentCaptor.forClass(OutboxEventJpa.class);
        verify(kafkaProperties).topic(TOPIC_KEY);
        verify(topic).name();
        verify(repository).save(savedEvent.capture());
        verifyNoMoreInteractions(kafkaProperties, topic, repository);

        OutboxEventJpa event = savedEvent.getValue();
        assertThat(event.getId()).isEqualTo(EVENT_ID);
        assertThat(event.getTopicKey()).isEqualTo(TOPIC_KEY);
        assertThat(event.getTopicName()).isEqualTo(TOPIC_NAME);
        assertThat(event.getAggregateType()).isEqualTo(AGGREGATE_TYPE);
        assertThat(event.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(event.getEventType()).isEqualTo(EVENT_TYPE);
        assertThat(event.getPayload()).isEqualTo(payload());
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW);
        assertThat(event.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(event.getCreatedAt()).isEqualTo(NOW);
        assertThat(event.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectNullEventBeforeResolvingTopicOrSaving() {
        // given
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        AppKafkaProperties kafkaProperties = mock(AppKafkaProperties.class);
        OutboxEventStoreAdapter adapter = adapter(repository, kafkaProperties);

        // when & then
        assertThatThrownBy(() -> adapter.append(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event cannot be null");
        verifyNoInteractions(kafkaProperties, repository);
    }

    @Test
    void shouldPropagateTopicResolutionFailureBeforeSaving() {
        // given
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        AppKafkaProperties kafkaProperties = mock(AppKafkaProperties.class);
        when(kafkaProperties.topic("external-event"))
                .thenThrow(new IllegalArgumentException("Missing Kafka topic configuration for key: external-event"));

        OutboxEventStoreAdapter adapter = adapter(repository, kafkaProperties);
        OutboxEventDraft draft = draft("external-event");

        // when & then
        assertThatThrownBy(() -> adapter.append(draft))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing Kafka topic configuration for key: external-event");
        verify(kafkaProperties).topic("external-event");
        verifyNoMoreInteractions(kafkaProperties);
        verifyNoInteractions(repository);
    }

    private OutboxEventStoreAdapter adapter(
            OutboxEventJpaRepository repository,
            AppKafkaProperties kafkaProperties
    ) {
        return new OutboxEventStoreAdapter(
                repository,
                kafkaProperties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private OutboxEventDraft draft(String topicKey) {
        return new OutboxEventDraft(
                EVENT_ID,
                topicKey,
                AGGREGATE_TYPE,
                AGGREGATE_ID,
                EVENT_TYPE,
                payload(),
                OCCURRED_AT
        );
    }

    private JsonNode payload() {
        return JsonNodeFactory.instance.objectNode()
                .put("schemaVersion", 1)
                .put("customerId", AGGREGATE_ID)
                .put("userId", "user-123");
    }
}
