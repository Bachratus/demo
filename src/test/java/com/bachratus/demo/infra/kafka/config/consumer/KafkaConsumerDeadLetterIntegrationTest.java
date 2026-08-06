package com.bachratus.demo.infra.kafka.config.consumer;

import com.bachratus.demo.config.BaseFullIntegrationTest;
import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestPropertySource(properties = {
        "app.kafka.outbox.enabled=false",
        "app.kafka.listener.retry-interval-ms=0",
        "app.kafka.listener.max-attempts=1",
        "spring.kafka.consumer.group-id=demo-app-dlt-it"
})
class KafkaConsumerDeadLetterIntegrationTest extends BaseFullIntegrationTest {

    private static final String CUSTOMER_ACCOUNT_CREATED_EVENT_KEY = "customer-account-created";

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private AppKafkaProperties kafkaProperties;

    @Test
    void shouldPublishMalformedValueRecordToConfiguredDltWithDiagnosticHeaders() throws Exception {
        // given
        String topicName = kafkaProperties.topic(CUSTOMER_ACCOUNT_CREATED_EVENT_KEY).name();
        String dltTopicName = kafkaProperties.deadLetterTopicName(CUSTOMER_ACCOUNT_CREATED_EVENT_KEY);
        String eventId = UUID.randomUUID().toString();
        String aggregateId = UUID.randomUUID().toString();

        try (KafkaConsumer<String, byte[]> dltConsumer = new KafkaConsumer<>(dltConsumerProperties())) {
            dltConsumer.subscribe(List.of(dltTopicName));

            // when
            publishMalformedJsonRecord(topicName, eventId, aggregateId);

            // then
            ConsumerRecord<String, byte[]> dltRecord = pollForDltRecord(dltConsumer, eventId);

            assertThat(dltRecord.topic()).isEqualTo(dltTopicName);
            assertThat(dltRecord.key()).isEqualTo(aggregateId);
            assertThat(dltRecord.partition()).isGreaterThanOrEqualTo(0);

            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_SOURCE)).isEqualTo("consumer");
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_APPLICATION)).isEqualTo("demo");
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_CONSUMER_GROUP)).isEqualTo("demo-app-dlt-it");
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_LISTENER_ID))
                    .isEqualTo("customer-account-created-console-logger");
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_ORIGINAL_TOPIC)).isEqualTo(topicName);
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_ORIGINAL_PARTITION))
                    .isEqualTo(Integer.toString(dltRecord.partition()));
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_EVENT_ID)).isEqualTo(eventId);
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_EVENT_TYPE))
                    .isEqualTo("customer.account-created.v1");
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_AGGREGATE_TYPE)).isEqualTo("customer");
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_AGGREGATE_ID)).isEqualTo(aggregateId);
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_ERROR_CLASS)).isNotBlank();
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_ROOT_CAUSE_CLASS)).isNotBlank();
            assertThat(header(dltRecord, KafkaDeadLetterHeadersFactory.DLT_FAILED_AT)).isNotBlank();
        }
    }

    private void publishMalformedJsonRecord(String topicName, String eventId, String aggregateId) throws Exception {
        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProperties())) {
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                    topicName,
                    aggregateId,
                    "{\"schemaVersion\":".getBytes(StandardCharsets.UTF_8)
            );

            record.headers().add("event-id", bytes(eventId));
            record.headers().add("event-type", bytes("customer.account-created.v1"));
            record.headers().add("aggregate-type", bytes("customer"));
            record.headers().add("aggregate-id", bytes(aggregateId));
            record.headers().add("content-type", bytes("application/json"));

            producer.send(record).get();
        }
    }

    private ConsumerRecord<String, byte[]> pollForDltRecord(
            KafkaConsumer<String, byte[]> consumer,
            String eventId
    ) {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();

        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, byte[]> record : records) {
                if (eventId.equals(headerOrNull(record, KafkaDeadLetterHeadersFactory.DLT_EVENT_ID))) {
                    return record;
                }
            }
        }

        return fail("Expected DLT record with dlt-event-id <%s> was not consumed in time", eventId);
    }

    private Map<String, Object> producerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return properties;
    }

    private Map<String, Object> dltConsumerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-assertions-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return properties;
    }

    private String bootstrapServers() {
        Object value = kafkaAdmin.getConfigurationProperties().get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
        if (value instanceof List<?> list) {
            return String.join(",", list.stream().map(Object::toString).toList());
        }
        return value.toString();
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private String header(ConsumerRecord<?, ?> record, String name) {
        String value = headerOrNull(record, name);
        assertThat(value).as("Kafka header <%s>", name).isNotNull();
        return value;
    }

    private String headerOrNull(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
