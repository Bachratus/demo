package com.bachratus.demo.infra.kafka.config;

import com.bachratus.demo.config.BaseFullIntegrationTest;
import com.bachratus.demo.infra.db.outbox.OutboxEventStoreAdapter;
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

@TestPropertySource(properties = "app.kafka.enabled=false")
class KafkaIntegrationDisabledIT extends BaseFullIntegrationTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    AppKafkaProperties kafkaProperties;

    @Test
    void shouldKeepOutboxInfrastructureAndRemoveKafkaInfrastructureWhenKafkaIntegrationIsDisabled() {
        assertThat(kafkaProperties.enabled()).isFalse();
        assertThat(applicationContext.getBeanNamesForType(KafkaOutboxEventDraftFactory.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(OutboxEventStoreAdapter.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaContainer.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaAdmin.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaAdmin.NewTopics.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaTemplate.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(ProducerFactory.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(ConsumerFactory.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(ConcurrentKafkaListenerContainerFactory.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaMessagingConfiguration.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaListenerEndpointRegistry.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaListenerErrorHandlingConfiguration.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(CustomerAccountCreatedKafkaListener.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(CustomerAccountCreatedKafkaEventHandler.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaDeadLetterHeadersFactory.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaDeadLetterTopicResolver.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaEventIdempotencyAspect.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(DefaultErrorHandler.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(OutboxKafkaPublisher.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(OutboxKafkaPublisherScheduler.class)).isEmpty();
    }
}
