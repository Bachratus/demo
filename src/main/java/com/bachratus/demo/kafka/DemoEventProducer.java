package com.bachratus.demo.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class DemoEventProducer {

    private final KafkaTemplate<String, DemoEvent> kafkaTemplate;
    private final String topic;

    public DemoEventProducer(
            KafkaTemplate<String, DemoEvent> kafkaTemplate,
            DemoKafkaProperties kafkaProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = kafkaProperties.topic("events");
    }

    public DemoEvent publish(String message) {
        DemoEvent event = new DemoEvent(UUID.randomUUID(), message, Instant.now());

        kafkaTemplate.send(topic, event.id().toString(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish demo event: {}", event.id(), exception);
                        return;
                    }

                    log.info(
                            "Published demo event: {} to topic: {}, partition: {}, offset: {}",
                            event.id(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });

        return event;
    }
}
