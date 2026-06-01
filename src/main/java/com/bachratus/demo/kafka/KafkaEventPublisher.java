package com.bachratus.demo.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, KafkaEvent event) {
        kafkaTemplate.send(topic, event.aggregateId().toString(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish Kafka event: {}", event.id(), exception);
                        return;
                    }

                    log.info(
                            "Published Kafka event: {} for aggregate: {} to topic: {}, partition: {}, offset: {}",
                            event.id(),
                            event.aggregateId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
