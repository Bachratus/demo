package com.bachratus.demo.infra.kafka.config.consumer.listener;

import com.bachratus.demo.infra.kafka.config.consumer.handler.CustomerAccountCreatedKafkaEventHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka listener that consumes customer account creation records and delegates handling to a consumer handler.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerAccountCreatedKafkaListener {

    private final CustomerAccountCreatedKafkaEventHandler handler;

    @KafkaListener(
            id = "customer-account-created-console-logger",
            info = "customer-account-created-console-logger",
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${app.kafka.topics.customer-account-created.name}",
            concurrency = "${app.kafka.topics.customer-account-created.concurrency}"
    )
    public void logCustomerAccountCreated(ConsumerRecord<String, JsonNode> record) {
        handler.handle(record);
    }
}
