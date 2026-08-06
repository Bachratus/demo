package com.bachratus.demo.infra.kafka.config.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Builds application-level diagnostic headers for records published to consumer dead-letter topics.
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaDeadLetterHeadersFactory {

    public static final String DLT_SOURCE = "dlt-source";
    public static final String DLT_APPLICATION = "dlt-application";
    public static final String DLT_CONSUMER_GROUP = "dlt-consumer-group";
    public static final String DLT_LISTENER_ID = "dlt-listener-id";
    public static final String DLT_ORIGINAL_TOPIC = "dlt-original-topic";
    public static final String DLT_ORIGINAL_PARTITION = "dlt-original-partition";
    public static final String DLT_ORIGINAL_OFFSET = "dlt-original-offset";
    public static final String DLT_ORIGINAL_TIMESTAMP = "dlt-original-timestamp";
    public static final String DLT_FAILED_AT = "dlt-failed-at";
    public static final String DLT_ERROR_CLASS = "dlt-error-class";
    public static final String DLT_ERROR_MESSAGE = "dlt-error-message";
    public static final String DLT_ROOT_CAUSE_CLASS = "dlt-root-cause-class";
    public static final String DLT_ROOT_CAUSE_MESSAGE = "dlt-root-cause-message";
    public static final String DLT_EVENT_ID = "dlt-event-id";
    public static final String DLT_EVENT_TYPE = "dlt-event-type";
    public static final String DLT_AGGREGATE_TYPE = "dlt-aggregate-type";
    public static final String DLT_AGGREGATE_ID = "dlt-aggregate-id";

    private static final int MAX_HEADER_VALUE_LENGTH = 1_000;
    private static final String UNKNOWN = "unknown";

    private final String applicationName;
    private final String consumerGroup;
    private final Clock clock;

    public KafkaDeadLetterHeadersFactory(
            @Value("${spring.application.name:unknown}") String applicationName,
            @Value("${spring.kafka.consumer.group-id:unknown}") String consumerGroup,
            Clock clock
    ) {
        this.applicationName = normalize(applicationName);
        this.consumerGroup = normalize(consumerGroup);
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public Headers create(ConsumerRecord<?, ?> record, Exception exception) {
        Objects.requireNonNull(record, "record cannot be null");
        Objects.requireNonNull(exception, "exception cannot be null");

        Headers headers = new RecordHeaders();
        Throwable rootCause = rootCause(exception);

        add(headers, DLT_SOURCE, "consumer");
        add(headers, DLT_APPLICATION, applicationName);
        add(headers, DLT_CONSUMER_GROUP, consumerGroup);
        add(headers, DLT_LISTENER_ID, listenerId(record));
        add(headers, DLT_ORIGINAL_TOPIC, record.topic());
        add(headers, DLT_ORIGINAL_PARTITION, Integer.toString(record.partition()));
        add(headers, DLT_ORIGINAL_OFFSET, Long.toString(record.offset()));
        add(headers, DLT_ORIGINAL_TIMESTAMP, Long.toString(record.timestamp()));
        add(headers, DLT_FAILED_AT, Instant.now(clock).toString());
        add(headers, DLT_ERROR_CLASS, exception.getClass().getName());
        add(headers, DLT_ERROR_MESSAGE, exception.getMessage());
        add(headers, DLT_ROOT_CAUSE_CLASS, rootCause.getClass().getName());
        add(headers, DLT_ROOT_CAUSE_MESSAGE, rootCause.getMessage());

        copyOriginalHeader(record, headers, "event-id", DLT_EVENT_ID);
        copyOriginalHeader(record, headers, "event-type", DLT_EVENT_TYPE);
        copyOriginalHeader(record, headers, "aggregate-type", DLT_AGGREGATE_TYPE);
        copyOriginalHeader(record, headers, "aggregate-id", DLT_AGGREGATE_ID);

        return headers;
    }

    private void copyOriginalHeader(ConsumerRecord<?, ?> record, Headers headers, String originalName, String dltName) {
        Header originalHeader = record.headers().lastHeader(originalName);
        if (originalHeader == null) return;
        headers.add(dltName, originalHeader.value());
    }

    private String listenerId(ConsumerRecord<?, ?> record) {
        Header listenerInfo = record.headers().lastHeader(KafkaHeaders.LISTENER_INFO);
        if (listenerInfo == null) return UNKNOWN;
        return normalize(new String(listenerInfo.value(), StandardCharsets.UTF_8));
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private void add(Headers headers, String name, String value) {
        headers.add(name, truncate(normalize(value)).getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return UNKNOWN;
        return value.trim();
    }

    private String truncate(String value) {
        if (value.length() <= MAX_HEADER_VALUE_LENGTH) return value;
        return value.substring(0, MAX_HEADER_VALUE_LENGTH);
    }
}
