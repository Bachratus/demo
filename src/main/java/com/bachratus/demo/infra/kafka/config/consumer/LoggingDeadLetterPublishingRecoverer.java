package com.bachratus.demo.infra.kafka.config.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.nio.charset.StandardCharsets;

/**
 * Dead-letter recoverer that logs consumer DLT publication attempts with business event metadata.
 */
@Slf4j
public class LoggingDeadLetterPublishingRecoverer extends DeadLetterPublishingRecoverer {

    private final KafkaDeadLetterTopicResolver deadLetterTopicResolver;

    public LoggingDeadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaDeadLetterTopicResolver deadLetterTopicResolver
    ) {
        super(kafkaTemplate, deadLetterTopicResolver::resolve);
        this.deadLetterTopicResolver = deadLetterTopicResolver;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, Exception exception) {
        TopicPartition destination = deadLetterTopicResolver.resolve(record, exception);

        log.warn(
                "Publishing Kafka record to DLT. sourceTopic={}, sourcePartition={}, sourceOffset={}, "
                        + "dltTopic={}, dltPartition={}, consumerGroup={}, eventId={}, eventType={}, "
                        + "exceptionClass={}, exceptionMessage={}",
                record.topic(),
                record.partition(),
                record.offset(),
                destination.topic(),
                destination.partition(),
                consumerGroup(consumer),
                header(record, "event-id"),
                header(record, "event-type"),
                exception.getClass().getName(),
                exception.getMessage()
        );

        try {
            super.accept(record, consumer, exception);
            log.warn(
                    "Published Kafka record to DLT. sourceTopic={}, sourcePartition={}, sourceOffset={}, "
                            + "dltTopic={}, dltPartition={}, consumerGroup={}, eventId={}, eventType={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    destination.topic(),
                    destination.partition(),
                    consumerGroup(consumer),
                    header(record, "event-id"),
                    header(record, "event-type")
            );
        } catch (RuntimeException recoveryException) {
            log.error(
                    "Failed to publish Kafka record to DLT. sourceTopic={}, sourcePartition={}, sourceOffset={}, "
                            + "dltTopic={}, dltPartition={}, consumerGroup={}, eventId={}, eventType={}, reason={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    destination.topic(),
                    destination.partition(),
                    consumerGroup(consumer),
                    header(record, "event-id"),
                    header(record, "event-type"),
                    recoveryException.getMessage(),
                    recoveryException
            );
            throw recoveryException;
        }
    }

    private String consumerGroup(Consumer<?, ?> consumer) {
        if (consumer == null || consumer.groupMetadata() == null || consumer.groupMetadata().groupId() == null) {
            return "unknown";
        }
        return consumer.groupMetadata().groupId();
    }

    private String header(ConsumerRecord<?, ?> record, String name) {
        org.apache.kafka.common.header.Header header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) return "unknown";
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
