package com.bachratus.demo.infra.kafka;

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

    private static final String CUSTOMER_ACCOUNT_CREATED_EVENT_KEY = "customer-account-created";

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Autowired
    AppKafkaProperties kafkaProperties;

    @Test
    void shouldCreateKafkaTopicsBeforeTestsStart() {
        String topicName = kafkaProperties.topic(CUSTOMER_ACCOUNT_CREATED_EVENT_KEY).name();
        String dltName = kafkaProperties.deadLetterTopicName(CUSTOMER_ACCOUNT_CREATED_EVENT_KEY, topicName);

        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(
                topicName,
                dltName
        );

        assertThat(topics).containsKeys(topicName, dltName);
        assertThat(topics.get(topicName).partitions()).hasSize(3);
        assertThat(topics.get(dltName).partitions()).hasSize(3);
    }
}
