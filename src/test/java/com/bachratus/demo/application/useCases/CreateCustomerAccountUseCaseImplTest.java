package com.bachratus.demo.application.useCases;

import com.bachratus.demo.application.events.CustomerAccountCreatedEvent;
import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.application.ports.out.CustomerRepository;
import com.bachratus.demo.application.ports.out.OutboxEventStore;
import com.bachratus.demo.application.request.CreateCustomerAccountRequest;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerDisplayName;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.UserId;
import com.bachratus.demo.domain.shared.exception.AlreadyExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCustomerAccountUseCaseImplTest {

    @Mock
    CustomerRepository customerRepository;

    CreateCustomerAccountUseCaseImpl createCustomerAccountUseCase;

    @Mock
    OutboxEventStore outboxEventStore;

    @Captor
    ArgumentCaptor<Customer> customerArgumentCaptor;

    @Captor
    ArgumentCaptor<OutboxEventDraft> outboxEventCaptor;

    @BeforeEach
    void setUp() {
        createCustomerAccountUseCase = new CreateCustomerAccountUseCaseImpl(
                customerRepository,
                outboxEventStore,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @DisplayName("Tests for create(CreateCustomerAccountRequest) method")
    @Nested
    class Create {

        @Test
        void shouldThrowExceptionWhenCustomerAlreadyExists() {
            // given
            CreateCustomerAccountRequest request = new CreateCustomerAccountRequest("Me", "123");
            UserId userId = UserId.of(request.subject());

            Customer existingCustomer = Customer.builder()
                    .id(CustomerId.create())
                    .userId(userId)
                    .build();

            when(customerRepository.findByUserId(userId))
                    .thenReturn(Optional.of(existingCustomer));

            // when & then
            assertThatThrownBy(() -> createCustomerAccountUseCase.create(request))
                    .isInstanceOf(AlreadyExistsException.class)
                    .hasMessageContaining("userId")
                    .hasMessageContaining("123");

            verify(customerRepository).findByUserId(userId);
            verify(customerRepository, never()).createNewCustomer(any(Customer.class));
            verifyNoInteractions(outboxEventStore);
            verifyNoMoreInteractions(customerRepository);
        }

        @Test
        void shouldPassInitializedNewCustomerToRepositoryAndReturnSavedCustomer() {
            // given
            CreateCustomerAccountRequest request = new CreateCustomerAccountRequest("Me", "123");
            UserId userId = UserId.of(request.subject());

            when(customerRepository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            when(customerRepository.createNewCustomer(any(Customer.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Customer result = createCustomerAccountUseCase.create(request);

            // then
            verify(customerRepository).findByUserId(userId);
            verify(customerRepository).createNewCustomer(customerArgumentCaptor.capture());
            verify(outboxEventStore).append(outboxEventCaptor.capture());

            Customer captured = customerArgumentCaptor.getValue();
            OutboxEventDraft outboxEvent = outboxEventCaptor.getValue();

            assertThat(captured).isNotNull();
            assertThat(captured.getId()).isNotNull();
            assertThat(captured.getUserId()).isEqualTo(userId);
            assertThat(captured.getCustomerDisplayName())
                    .isEqualTo(CustomerDisplayName.optional(request.displayName()).orElse(null));

            assertThat(result).isSameAs(captured);

            assertThat(outboxEvent.topicKey()).isEqualTo(CustomerAccountCreatedEvent.TOPIC_KEY);
            assertThat(outboxEvent.aggregateType()).isEqualTo(CustomerAccountCreatedEvent.AGGREGATE_TYPE);
            assertThat(outboxEvent.aggregateId()).isEqualTo(captured.getId().value().toString());
            assertThat(outboxEvent.eventType()).isEqualTo(CustomerAccountCreatedEvent.EVENT_TYPE);
            assertThat(outboxEvent.occurredAt()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
            assertThat(outboxEvent.payload().get("schemaVersion").asInt()).isEqualTo(1);
            assertThat(outboxEvent.payload().get("customerId").asText()).isEqualTo(captured.getId().value().toString());
            assertThat(outboxEvent.payload().get("userId").asText()).isEqualTo("123");
            assertThat(outboxEvent.payload().get("displayName").asText()).isEqualTo("Me");

            verifyNoMoreInteractions(customerRepository);
            verifyNoMoreInteractions(outboxEventStore);
        }
    }
}
