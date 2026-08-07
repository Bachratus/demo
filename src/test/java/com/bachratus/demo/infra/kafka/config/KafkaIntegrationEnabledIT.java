package com.bachratus.demo.infra.kafka.config;

import com.bachratus.demo.config.BaseFullIntegrationTest;
import com.bachratus.demo.infra.kafka.config.consumer.KafkaDeadLetterHeadersFactory;
import com.bachratus.demo.infra.kafka.config.consumer.KafkaDeadLetterTopicResolver;
import com.bachratus.demo.infra.kafka.config.consumer.KafkaListenerErrorHandlingConfiguration;
import com.bachratus.demo.infra.kafka.config.consumer.KafkaMessagingConfiguration;
import com.bachratus.demo.infra.kafka.config.consumer.handler.CustomerAccountCreatedKafkaEventHandler;
import com.bachratus.demo.infra.kafka.config.consumer.idempotency.KafkaEventIdempotencyAspect;
import com.bachratus.demo.infra.kafka.config.consumer.listener.CustomerAccountCreatedKafkaListener;
import com.bachratus.demo.infra.kafka.config.producer.OutboxKafkaPublisher;
import com.bachratus.demo.infra.kafka.config.producer.OutboxKafkaPublisherScheduler;
import com.bachratus.demo.infra.kafka.event.KafkaOutboxEventDraftFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "spring.kafka.listener.auto-startup=false")
class KafkaIntegrationEnabledIT extends BaseFullIntegrationTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    AppKafkaProperties kafkaProperties;

    @Test
    void shouldRegisterKafkaInfrastructureWhenKafkaIntegrationIsEnabled() {
        assertThat(kafkaProperties.enabled()).isTrue();
        assertThat(applicationContext.getBeanNamesForType(KafkaOutboxEventDraftFactory.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaContainer.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaAdmin.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaAdmin.NewTopics.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaTemplate.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(ProducerFactory.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(ConsumerFactory.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(ConcurrentKafkaListenerContainerFactory.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaMessagingConfiguration.class)).isNotEmpty();
        assertThat(applicationContext.getBean(KafkaListenerEndpointRegistry.class).getListenerContainerIds())
                .contains("customer-account-created-console-logger");
        assertThat(applicationContext.getBeanNamesForType(KafkaListenerErrorHandlingConfiguration.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(CustomerAccountCreatedKafkaListener.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(CustomerAccountCreatedKafkaEventHandler.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaDeadLetterHeadersFactory.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaDeadLetterTopicResolver.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaEventIdempotencyAspect.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(DefaultErrorHandler.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(OutboxKafkaPublisher.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(OutboxKafkaPublisherScheduler.class)).isNotEmpty();
    }
}
