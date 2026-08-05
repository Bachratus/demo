package com.bachratus.demo.infra.kafka.config.consumer;

import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Resolves dead-letter topic partitions for failed Kafka records using configured topic mappings.
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaDeadLetterTopicResolver {

    private final AppKafkaProperties kafkaProperties;

    public TopicPartition resolve(ConsumerRecord<?, ?> record, Exception exception) {
        return new TopicPartition(
                kafkaProperties.deadLetterTopicNameForTopic(record.topic()),
                record.partition()
        );
    }
}
