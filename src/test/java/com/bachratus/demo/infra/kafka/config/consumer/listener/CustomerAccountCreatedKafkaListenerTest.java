package com.bachratus.demo.infra.kafka.config.consumer.listener;

import com.bachratus.demo.infra.kafka.config.consumer.handler.CustomerAccountCreatedKafkaEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerAccountCreatedKafkaListenerTest {

    @Mock
    CustomerAccountCreatedKafkaEventHandler handler;

    private CustomerAccountCreatedKafkaListener listener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        listener = new CustomerAccountCreatedKafkaListener(handler);
    }

    @DisplayName("Tests for logCustomerAccountCreated(ConsumerRecord) method")
    @Nested
    class LogCustomerAccountCreated {

        @Test
        void shouldDelegateConsumedCustomerAccountCreatedEventToHandler() {
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
            verify(handler).handle(record);
        }
    }
}
