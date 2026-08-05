package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.infra.kafka.config.consumer.CustomerAccountCreatedKafkaListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class CustomerAccountCreatedKafkaListenerTest {

    private final CustomerAccountCreatedKafkaListener listener = new CustomerAccountCreatedKafkaListener();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("Tests for logCustomerAccountCreated(ConsumerRecord) method")
    @Nested
    class LogCustomerAccountCreated {

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
            listener.logCustomerAccountCreated(record);

            // then
            assertThat(output.getOut())
                    .contains("Received customer account created event")
                    .contains("store.customer-account-created.v1")
                    .contains("partition=2")
                    .contains("offset=42")
                    .contains("customer-123");
        }
    }
}
