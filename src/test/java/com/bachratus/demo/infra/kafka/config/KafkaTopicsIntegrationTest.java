package com.bachratus.demo.infra.kafka.config;

import com.bachratus.demo.config.BaseFullIntegrationTest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
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
    void shouldCreateMainAndDeadLetterTopicsWithMatchingPartitionsAndDurabilityConfig() throws Exception {
        String topicName = kafkaProperties.topic(CUSTOMER_ACCOUNT_CREATED_EVENT_KEY).name();
        String dltName = kafkaProperties.deadLetterTopicName(CUSTOMER_ACCOUNT_CREATED_EVENT_KEY);

        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(
                topicName,
                dltName
        );

        assertThat(topics).containsKeys(topicName, dltName);
        assertThat(topics.get(topicName).partitions())
                .extracting(TopicPartitionInfo::partition)
                .containsExactly(0, 1, 2);
        assertThat(topics.get(dltName).partitions())
                .extracting(TopicPartitionInfo::partition)
                .containsExactly(0, 1, 2);

        assertThat(topicConfig(topicName, TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG)).isEqualTo("1");
        assertThat(topicConfig(dltName, TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG)).isEqualTo("1");
    }

    private String topicConfig(String topicName, String configName) throws Exception {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Map<ConfigResource, Config> configs = adminClient.describeConfigs(List.of(resource))
                    .all()
                    .get();

            ConfigEntry entry = configs.get(resource).get(configName);
            return entry.value();
        }
    }
}
