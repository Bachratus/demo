package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.application.events.CustomerAccountCreatedEvent;
import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.infra.db.outbox.OutboxEventJpa;
import com.bachratus.demo.infra.db.outbox.OutboxEventJpaRepository;
import com.bachratus.demo.infra.db.outbox.OutboxStatus;
import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxKafkaPublisherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    OutboxEventJpaRepository repository;

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    OutboxKafkaPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxKafkaPublisher(
                repository,
                kafkaTemplate,
                kafkaProperties(3),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @DisplayName("Tests for publishBatch() method")
    @Nested
    class PublishBatch {

        @Test
        void shouldPublishPendingEventToMainTopicAndMarkAsPublished() {
            // given
            OutboxEventJpa event = event();

            when(repository.streamPublishableEvents(10, 3))
                    .thenReturn(Stream.of(event));

            when(kafkaTemplate.send(anyRecord()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // when
            int processed = publisher.publishBatch();

            // then
            assertThat(processed).isEqualTo(1);
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(event.getPublishedAt()).isEqualTo(NOW);
            assertThat(event.getLastError()).isNull();

            ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = recordCaptor();
            verify(kafkaTemplate).send(recordCaptor.capture());

            ProducerRecord<String, Object> record = recordCaptor.getValue();
            assertThat(record.topic()).isEqualTo("demo.customer-account-created.v1");
            assertThat(record.key()).isEqualTo(event.getAggregateId());
            assertThat(record.headers().lastHeader("event-id")).isNotNull();
            assertThat(record.headers().lastHeader("event-type")).isNotNull();
            assertThat(record.headers().lastHeader("content-type")).isNotNull();
        }

        @Test
        void shouldMarkEventAsFailedAndScheduleRetryWhenMainTopicPublishFailsBeforeMaxAttempts() {
            // given
            OutboxEventJpa event = event();

            when(repository.streamPublishableEvents(10, 3))
                    .thenReturn(Stream.of(event));

            when(kafkaTemplate.send(anyRecord()))
                    .thenReturn(failedFuture(new RuntimeException("Kafka unavailable")));

            // when
            int processed = publisher.publishBatch();

            // then
            assertThat(processed).isEqualTo(1);
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
            assertThat(event.getRetryCount()).isEqualTo(1);
            assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusMillis(1_000));
            assertThat(event.getLastError()).contains("Kafka unavailable");

            verify(kafkaTemplate).send(anyRecord());
        }

        @Test
        void shouldPublishToDltAndMarkAsDeadLetteredAfterMaxAttempts() {
            // given
            OutboxEventJpa event = event();
            event.markMainTopicPublishFailed("first", 3, 0, NOW.minusSeconds(2));
            event.markMainTopicPublishFailed("second", 3, 0, NOW.minusSeconds(1));

            when(repository.streamPublishableEvents(10, 3))
                    .thenReturn(Stream.of(event));

            when(kafkaTemplate.send(anyRecord()))
                    .thenReturn(
                            failedFuture(new RuntimeException("Kafka rejected record")),
                            CompletableFuture.completedFuture(null)
                    );

            // when
            int processed = publisher.publishBatch();

            // then
            assertThat(processed).isEqualTo(1);
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTERED);
            assertThat(event.getRetryCount()).isEqualTo(3);
            assertThat(event.getDeadLetteredAt()).isEqualTo(NOW);
            assertThat(event.getLastError()).contains("Kafka rejected record");

            ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = recordCaptor();
            verify(kafkaTemplate, times(2)).send(recordCaptor.capture());

            assertThat(recordCaptor.getAllValues().get(0).topic())
                    .isEqualTo("demo.customer-account-created.v1");
            assertThat(recordCaptor.getAllValues().get(1).topic())
                    .isEqualTo("demo.customer-account-created.v1.dlt");
            assertThat(recordCaptor.getAllValues().get(1).headers().lastHeader("dlt-original-topic"))
                    .isNotNull();
        }
    }

    private OutboxEventJpa event() {
        UUID customerId = UUID.randomUUID();

        OutboxEventDraft draft = new OutboxEventDraft(
                UUID.randomUUID(),
                CustomerAccountCreatedEvent.TOPIC_KEY,
                CustomerAccountCreatedEvent.AGGREGATE_TYPE,
                customerId.toString(),
                CustomerAccountCreatedEvent.EVENT_TYPE,
                OBJECT_MAPPER.createObjectNode()
                        .put("schemaVersion", 1)
                        .put("customerId", customerId.toString())
                        .put("userId", "user-123"),
                Map.of("trace-id", "trace-123"),
                Instant.parse("2026-01-01T10:00:00Z")
        );

        return OutboxEventJpa.from(
                draft,
                "demo.customer-account-created.v1",
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }

    private AppKafkaProperties kafkaProperties(int maxAttempts) {
        return new AppKafkaProperties(
                new AppKafkaProperties.Producer(45_000, 15_000, 5, 50_000),
                new AppKafkaProperties.Listener(1_000, 3),
                new AppKafkaProperties.Outbox(true, 10, 500, 1_000, maxAttempts),
                Map.of(
                        CustomerAccountCreatedEvent.TOPIC_KEY,
                        new AppKafkaProperties.Topic(
                                "demo.customer-account-created.v1",
                                3,
                                3,
                                1,
                                1,
                                "demo.customer-account-created.v1.dlt"
                        )
                )
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private CompletableFuture failedFuture(Exception exception) {
        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(exception);
        return future;
    }

    @SuppressWarnings("unchecked")
    private ProducerRecord<String, Object> anyRecord() {
        return any(ProducerRecord.class);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor() {
        return ArgumentCaptor.forClass(ProducerRecord.class);
    }
}
