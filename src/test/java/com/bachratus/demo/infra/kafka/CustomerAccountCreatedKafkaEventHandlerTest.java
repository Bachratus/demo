package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.infra.kafka.config.consumer.handler.CustomerAccountCreatedKafkaEventHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class CustomerAccountCreatedKafkaEventHandlerTest {

    private final CustomerAccountCreatedKafkaEventHandler handler = new CustomerAccountCreatedKafkaEventHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLogConsumedCustomerAccountCreatedEvent(CapturedOutput output) {
        // given
        ConsumerRecord<String, JsonNode> record = new ConsumerRecord<>(
                "store.customer-account-created.v1",
                2,
                42L,
                "customer-123",
                objectMapper.createObjectNode()
                        .put("schemaVersion", 1)
                        .put("customerId", "customer-123")
                        .put("userId", "user-123")
        );

        // when
        handler.handle(record);

        // then
        assertThat(output.getOut())
                .contains("Received customer account created event")
                .contains("store.customer-account-created.v1")
                .contains("partition=2")
                .contains("offset=42")
                .contains("customer-123");
    }
}
