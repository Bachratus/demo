package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.application.events.CustomerAccountCreatedEvent;
import com.bachratus.demo.config.BaseFullIntegrationTest;
import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "app.kafka.outbox.enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
class KafkaTopicsIntegrationTest extends BaseFullIntegrationTest {

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Autowired
    AppKafkaProperties kafkaProperties;

    @Test
    void shouldCreateKafkaTopicsBeforeTestsStart() {
        AppKafkaProperties.Topic topic = kafkaProperties.topic(CustomerAccountCreatedEvent.TOPIC_KEY);

        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(
                topic.name(),
                topic.dltName()
        );

        assertThat(topics).containsKeys(topic.name(), topic.dltName());
        assertThat(topics.get(topic.name()).partitions()).hasSize(3);
        assertThat(topics.get(topic.dltName()).partitions()).hasSize(3);
    }
}
