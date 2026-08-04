package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.infra.db.outbox.OutboxEventJpa;
import com.bachratus.demo.infra.db.outbox.OutboxEventJpaRepository;
import com.bachratus.demo.infra.db.outbox.OutboxStatus;
import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxKafkaPublisher {

    private static final int MAX_ERROR_LENGTH = 2_000;

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppKafkaProperties kafkaProperties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int publishBatch() {
        AppKafkaProperties.Outbox outbox = kafkaProperties.outbox();
        AtomicInteger processedEvents = new AtomicInteger();

        try (Stream<OutboxEventJpa> events = repository.streamPublishableEvents(
                outbox.batchSize(),
                outbox.maxAttempts()
        )) {
            events.forEach(event -> {
                publish(event);
                processedEvents.incrementAndGet();
            });
        }

        return processedEvents.get();
    }

    private void publish(OutboxEventJpa event) {
        if (event.getStatus() == OutboxStatus.DLT_PENDING) {
            publishToDeadLetterTopic(event, event.getLastError());
            return;
        }

        publishToMainTopic(event);
    }

    private void publishToMainTopic(OutboxEventJpa event) {
        try {
            send(event.getTopicName(), event, Map.of());
            event.markPublished(Instant.now(clock));
        } catch (Exception exception) {
            String error = errorMessage(exception);
            Instant now = Instant.now(clock);

            event.markMainTopicPublishFailed(
                    error,
                    kafkaProperties.outbox().maxAttempts(),
                    kafkaProperties.outbox().retryBackoffMs(),
                    now
            );

            log.warn(
                    "Failed to publish outbox event {} to Kafka topic {}. Attempt {}/{}. Reason: {}",
                    event.getId(),
                    event.getTopicName(),
                    event.getRetryCount(),
                    kafkaProperties.outbox().maxAttempts(),
                    error
            );

            if (event.getStatus() == OutboxStatus.DLT_PENDING) {
                publishToDeadLetterTopic(event, error);
            }
        }
    }

    private void publishToDeadLetterTopic(OutboxEventJpa event, String sourceError) {
        String dltTopicName = kafkaProperties.deadLetterTopicName(event.getTopicKey(), event.getTopicName());

        try {
            send(dltTopicName, event, Map.of(
                    "dlt-original-topic", event.getTopicName(),
                    "dlt-error", sourceError == null ? "unknown" : sourceError
            ));
            event.markDeadLettered(sourceError, Instant.now(clock));
            log.warn("Published outbox event {} to DLT topic {}", event.getId(), dltTopicName);
        } catch (Exception exception) {
            String dltError = errorMessage(exception);
            String combinedError = "DLT publish failed after main topic error [%s]: %s"
                    .formatted(sourceError, dltError);

            event.markDeadLetterPublishFailed(
                    trim(combinedError),
                    kafkaProperties.outbox().retryBackoffMs(),
                    Instant.now(clock)
            );

            log.error("Failed to publish outbox event {} to DLT topic {}. Reason: {}",
                    event.getId(), dltTopicName, dltError);
        }
    }

    private void send(String topicName, OutboxEventJpa event, Map<String, String> additionalHeaders)
            throws ExecutionException, InterruptedException, TimeoutException {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                topicName,
                event.getAggregateId(),
                event.getPayload()
        );

        event.getHeaders().forEach((key, value) -> addHeader(record, key, value));
        addHeader(record, "event-id", event.getId().toString());
        addHeader(record, "event-type", event.getEventType());
        addHeader(record, "aggregate-type", event.getAggregateType());
        addHeader(record, "aggregate-id", event.getAggregateId());
        addHeader(record, "occurred-at", event.getOccurredAt().toString());
        addHeader(record, "content-type", "application/json");
        additionalHeaders.forEach((key, value) -> addHeader(record, key, value));

        try {
            kafkaTemplate.send(record).get(
                    kafkaProperties.producer().sendResultTimeoutMs(),
                    TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    private void addHeader(ProducerRecord<String, Object> record, String key, String value) {
        if (value == null) return;
        record.headers().add(key, value.getBytes(StandardCharsets.UTF_8));
    }

    private String errorMessage(Exception exception) {
        Throwable root = exception instanceof ExecutionException && exception.getCause() != null
                ? exception.getCause()
                : exception;

        String message = root.getMessage();
        String error = message == null || message.isBlank()
                ? root.getClass().getSimpleName()
                : root.getClass().getSimpleName() + ": " + message;

        return trim(error);
    }

    private String trim(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) return value;
        return value.substring(0, MAX_ERROR_LENGTH);
    }
}
