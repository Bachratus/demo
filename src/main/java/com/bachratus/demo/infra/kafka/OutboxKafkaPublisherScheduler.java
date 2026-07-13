package com.bachratus.demo.infra.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxKafkaPublisherScheduler {

    private final OutboxKafkaPublisher publisher;

    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-delay-ms:500}")
    public void publishScheduledBatch() {
        try {
            int published = publisher.publishBatch();
            if (published > 0) {
                log.info("Processed {} outbox events", published);
            }
        } catch (Exception exception) {
            log.error("Unexpected outbox publisher failure", exception);
        }
    }
}
