package com.bachratus.demo.infra.kafka;

import com.bachratus.demo.application.ports.in.CreateCustomerAccountUseCase;
import com.bachratus.demo.application.request.CreateCustomerAccountRequest;
import com.bachratus.demo.config.BaseFullIntegrationTest;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.infra.db.outbox.OutboxEventJpa;
import com.bachratus.demo.infra.db.outbox.OutboxEventJpaRepository;
import com.bachratus.demo.infra.db.outbox.OutboxStatus;
import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import com.bachratus.demo.infra.kafka.config.consumer.CustomerAccountCreatedKafkaListener;
import com.bachratus.demo.infra.kafka.config.consumer.KafkaDeadLetterTopicResolver;
import com.bachratus.demo.infra.kafka.config.producer.OutboxKafkaPublisher;
import com.bachratus.demo.infra.kafka.config.producer.OutboxKafkaPublisherScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "app.kafka.enabled=false")
class KafkaIntegrationDisabledIT extends BaseFullIntegrationTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    AppKafkaProperties kafkaProperties;

    @Autowired
    CreateCustomerAccountUseCase createCustomerAccountUseCase;

    @Autowired
    OutboxEventJpaRepository outboxEventRepository;

    @Test
    void shouldKeepOutboxWritesAndDisableKafkaIntegration() {
        assertThat(kafkaProperties.enabled()).isFalse();
        assertThat(applicationContext.getBeanNamesForType(KafkaContainer.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaAdmin.NewTopics.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(CustomerAccountCreatedKafkaListener.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KafkaDeadLetterTopicResolver.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(DefaultErrorHandler.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(OutboxKafkaPublisher.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(OutboxKafkaPublisherScheduler.class)).isEmpty();

        Customer customer = createCustomerAccountUseCase.create(new CreateCustomerAccountRequest(
                "Offline Kafka",
                "disabled-kafka-" + UUID.randomUUID()
        ));

        Optional<OutboxEventJpa> storedEvent = outboxEventRepository.findAll()
                .stream()
                .filter(event -> event.getAggregateId().equals(customer.getId().value().toString()))
                .findFirst();

        assertThat(storedEvent).hasValueSatisfying(event -> {
            assertThat(event.getTopicKey()).isEqualTo("customer-account-created");
            assertThat(event.getTopicName()).isEqualTo("store.customer-account-created.v1");
            assertThat(event.getAggregateType()).isEqualTo("customer");
            assertThat(event.getEventType()).isEqualTo("customer.account-created.v1");
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(event.getPayload().get("displayName").asText()).isEqualTo("Offline Kafka");
        });
    }
}
