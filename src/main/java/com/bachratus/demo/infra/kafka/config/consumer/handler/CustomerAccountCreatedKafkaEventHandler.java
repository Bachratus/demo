package com.bachratus.demo.infra.kafka.config.consumer.handler;

import com.bachratus.demo.infra.kafka.config.consumer.idempotency.IdempotentKafkaEventHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Handles customer account creation records after Kafka transport concerns are resolved by the listener.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerAccountCreatedKafkaEventHandler {

    @IdempotentKafkaEventHandler
    public void handle(ConsumerRecord<String, JsonNode> record) {
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
