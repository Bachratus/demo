package com.bachratus.demo.infra.kafka.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka listener that consumes customer account creation events from the configured topic and logs them.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerAccountCreatedKafkaListener {

    @KafkaListener(
            id = "customer-account-created-console-logger",
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${app.kafka.topics.customer-account-created.name}",
            concurrency = "${app.kafka.topics.customer-account-created.concurrency}"
    )
    public void logCustomerAccountCreated(ConsumerRecord<String, JsonNode> record) {
        log.info(
                "Received customer account created event: topic={}, partition={}, offset={}, key={}, payload={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value()
        );
    }
}
