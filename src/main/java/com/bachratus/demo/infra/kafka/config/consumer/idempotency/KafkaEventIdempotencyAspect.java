package com.bachratus.demo.infra.kafka.config.consumer.idempotency;

import com.bachratus.demo.infra.db.processed.ProcessedEventJpa;
import com.bachratus.demo.infra.db.processed.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Stores processed Kafka event identifiers before invoking annotated handlers and skips duplicates.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventIdempotencyAspect {

    private static final String EVENT_ID_HEADER = "event-id";
    private static final String EVENT_TYPE_HEADER = "event-type";

    private final ProcessedEventJpaRepository repository;
    private final TransactionTemplate transactionTemplate;

    @Around("@annotation(com.bachratus.demo.infra.kafka.config.consumer.idempotency.IdempotentKafkaEventHandler)")
    public Object processOnce(ProceedingJoinPoint joinPoint) throws Throwable {
        KafkaEventIdentity identity = KafkaEventIdentity.from(joinPoint.getArgs());

        try {
            return transactionTemplate.execute(status -> {
                repository.saveAndFlush(ProcessedEventJpa.of(identity.eventType(), identity.eventId()));

                try {
                    return joinPoint.proceed();
                } catch (Throwable throwable) {
                    throw new HandlerInvocationException(throwable);
                }
            });
        } catch (DataIntegrityViolationException exception) {
            log.info(
                    "Skipping already processed Kafka event: eventType={}, eventId={}",
                    identity.eventType(),
                    identity.eventId()
            );
            return null;
        } catch (HandlerInvocationException exception) {
            throw exception.getCause();
        }
    }

    private record KafkaEventIdentity(String eventType, String eventId) {

        private static KafkaEventIdentity from(Object[] arguments) {
            ConsumerRecord<?, ?> record = Arrays.stream(arguments)
                    .filter(ConsumerRecord.class::isInstance)
                    .map(ConsumerRecord.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "@IdempotentKafkaEventHandler method must receive a ConsumerRecord argument"
                    ));

            return new KafkaEventIdentity(
                    requiredHeader(record, EVENT_TYPE_HEADER),
                    requiredHeader(record, EVENT_ID_HEADER)
            );
        }

        private static String requiredHeader(ConsumerRecord<?, ?> record, String name) {
            Header header = record.headers().lastHeader(name);
            if (header == null) {
                throw new IllegalArgumentException("Missing required Kafka header: " + name);
            }

            String value = new String(header.value(), StandardCharsets.UTF_8).trim();
            if (value.isBlank()) {
                throw new IllegalArgumentException("Kafka header cannot be blank: " + name);
            }
            return value;
        }
    }

    private static class HandlerInvocationException extends RuntimeException {

        private HandlerInvocationException(Throwable cause) {
            super(cause);
        }
    }
}
