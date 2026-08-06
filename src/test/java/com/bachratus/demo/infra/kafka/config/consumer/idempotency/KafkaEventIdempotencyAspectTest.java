package com.bachratus.demo.infra.kafka.config.consumer.idempotency;

import com.bachratus.demo.infra.db.processed.ProcessedEventJpa;
import com.bachratus.demo.infra.db.processed.ProcessedEventJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventIdempotencyAspectTest {

    private static final String EVENT_TYPE = "customer.account-created.v1";
    private static final String EVENT_ID = "event-123";

    @Mock
    ProcessedEventJpaRepository repository;

    @Mock
    TransactionTemplate transactionTemplate;

    @Mock
    ProceedingJoinPoint joinPoint;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaEventIdempotencyAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new KafkaEventIdempotencyAspect(repository, transactionTemplate);
    }

    @Test
    void shouldStoreProcessedEventAndProceedWithHandler() throws Throwable {
        // given
        executeTransactionCallbacks();
        ConsumerRecord<String, JsonNode> record = record();

        when(joinPoint.getArgs()).thenReturn(new Object[]{record});
        when(joinPoint.proceed()).thenReturn("handled");

        // when
        Object result = aspect.processOnce(joinPoint);

        // then
        assertThat(result).isEqualTo("handled");

        ArgumentCaptor<ProcessedEventJpa> eventCaptor = ArgumentCaptor.forClass(ProcessedEventJpa.class);
        verify(repository).saveAndFlush(eventCaptor.capture());
        verify(joinPoint).proceed();

        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(EVENT_TYPE);
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void shouldSkipHandlerWhenEventWasAlreadyProcessed() throws Throwable {
        // given
        executeTransactionCallbacks();
        ConsumerRecord<String, JsonNode> record = record();

        when(joinPoint.getArgs()).thenReturn(new Object[]{record});
        when(repository.saveAndFlush(any(ProcessedEventJpa.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        // when
        Object result = aspect.processOnce(joinPoint);

        // then
        assertThat(result).isNull();
        verify(joinPoint, never()).proceed();
    }

    @Test
    void shouldRequireEventIdHeader() {
        // given
        ConsumerRecord<String, JsonNode> record = recordWithoutHeader("event-id");
        when(joinPoint.getArgs()).thenReturn(new Object[]{record});

        // when & then
        assertThatThrownBy(() -> aspect.processOnce(joinPoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event-id");

        verifyNoInteractions(repository);
        verifyNoInteractions(transactionTemplate);
    }

    @SuppressWarnings("unchecked")
    private void executeTransactionCallbacks() {
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
    }

    private ConsumerRecord<String, JsonNode> record() {
        ConsumerRecord<String, JsonNode> record = new ConsumerRecord<>(
                "store.customer-account-created.v1",
                0,
                1L,
                "customer-123",
                objectMapper.createObjectNode()
        );
        record.headers().add(header("event-type", EVENT_TYPE));
        record.headers().add(header("event-id", EVENT_ID));
        return record;
    }

    private ConsumerRecord<String, JsonNode> recordWithoutHeader(String missingHeader) {
        ConsumerRecord<String, JsonNode> record = new ConsumerRecord<>(
                "store.customer-account-created.v1",
                0,
                1L,
                "customer-123",
                objectMapper.createObjectNode()
        );
        if (!"event-type".equals(missingHeader)) {
            record.headers().add(header("event-type", EVENT_TYPE));
        }
        if (!"event-id".equals(missingHeader)) {
            record.headers().add(header("event-id", EVENT_ID));
        }
        return record;
    }

    private RecordHeader header(String name, String value) {
        return new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
