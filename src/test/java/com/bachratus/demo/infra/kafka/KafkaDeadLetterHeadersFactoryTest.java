package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.infra.kafka.config.consumer.KafkaDeadLetterHeadersFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaDeadLetterHeadersFactoryTest {

    private static final Instant NOW = Instant.parse("2026-08-06T09:30:00Z");

    private final KafkaDeadLetterHeadersFactory factory = new KafkaDeadLetterHeadersFactory(
            "demo",
            "demo-app",
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void shouldCreateDiagnosticHeadersForConsumerDeadLetterRecord() {
        // given
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "store.customer-account-created.v1",
                2,
                42L,
                1_722_941_200_000L,
                TimestampType.CREATE_TIME,
                12,
                20,
                "customer-123",
                "{\"customerId\":\"customer-123\"}",
                new RecordHeaders(),
                Optional.empty()
        );
        record.headers().add("event-id", bytes("event-123"));
        record.headers().add("event-type", bytes("customer.account-created.v1"));
        record.headers().add("aggregate-type", bytes("customer"));
        record.headers().add("aggregate-id", bytes("customer-123"));
        record.headers().add(KafkaHeaders.LISTENER_INFO, bytes("customer-account-created-console-logger"));

        Exception exception = new IllegalStateException(
                "handler failed",
                new IllegalArgumentException("invalid customer state")
        );

        // when
        Headers headers = factory.create(record, exception);

        // then
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_SOURCE)).isEqualTo("consumer");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_APPLICATION)).isEqualTo("demo");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_CONSUMER_GROUP)).isEqualTo("demo-app");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_LISTENER_ID))
                .isEqualTo("customer-account-created-console-logger");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_ORIGINAL_TOPIC))
                .isEqualTo("store.customer-account-created.v1");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_ORIGINAL_PARTITION)).isEqualTo("2");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_ORIGINAL_OFFSET)).isEqualTo("42");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_ORIGINAL_TIMESTAMP))
                .isEqualTo("1722941200000");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_FAILED_AT)).isEqualTo(NOW.toString());
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_ERROR_CLASS))
                .isEqualTo(IllegalStateException.class.getName());
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_ERROR_MESSAGE)).isEqualTo("handler failed");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_ROOT_CAUSE_CLASS))
                .isEqualTo(IllegalArgumentException.class.getName());
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_ROOT_CAUSE_MESSAGE))
                .isEqualTo("invalid customer state");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_EVENT_ID)).isEqualTo("event-123");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_EVENT_TYPE))
                .isEqualTo("customer.account-created.v1");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_AGGREGATE_TYPE)).isEqualTo("customer");
        assertThat(header(headers, KafkaDeadLetterHeadersFactory.DLT_AGGREGATE_ID)).isEqualTo("customer-123");
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private String header(Headers headers, String name) {
        return new String(headers.lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}
