package com.bachratus.demo.infra.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

@Component
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
