package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableKafka
@EnableConfigurationProperties(AppKafkaProperties.class)
public class KafkaTopicConfiguration {

    @Bean
    public KafkaAdmin.NewTopics demoTopics(AppKafkaProperties properties) {
        NewTopic[] topics = properties.topics().values().stream()
                .flatMap(topic -> Map.of(
                        topic.name(), topic,
                        topic.dltName(), topic
                ).entrySet().stream())
                .collect(
                        LinkedHashMap<String, Map.Entry<String, AppKafkaProperties.Topic>>::new,
                        (topicsByName, topic) -> topicsByName.putIfAbsent(topic.getKey(), topic),
                        LinkedHashMap::putAll
                )
                .values()
                .stream()
                .map(topic -> newTopic(topic.getKey(), topic.getValue()))
                .toArray(NewTopic[]::new);

        return new KafkaAdmin.NewTopics(topics);
    }

    private NewTopic newTopic(String name, AppKafkaProperties.Topic topic) {
        return TopicBuilder.name(name)
                .partitions(topic.partitions())
                .replicas(topic.replicationFactor())
                .config("min.insync.replicas", String.valueOf(topic.minInSyncReplicas()))
                .build();
    }
}
