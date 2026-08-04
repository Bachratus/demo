package com.bachratus.demo.application.useCases;

import com.bachratus.demo.application.events.CustomerAccountCreatedEvent;
import com.bachratus.demo.application.events.OutboxApplicationEvent;
import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.application.ports.out.CustomerRepository;
import com.bachratus.demo.application.ports.out.OutboxEventDraftFactory;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCustomerAccountUseCaseImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    CustomerRepository customerRepository;

    CreateCustomerAccountUseCaseImpl createCustomerAccountUseCase;

    @Mock
    OutboxEventStore outboxEventStore;

    @Mock
    OutboxEventDraftFactory outboxEventDraftFactory;

    @Captor
    ArgumentCaptor<Customer> customerArgumentCaptor;

    @Captor
    ArgumentCaptor<OutboxApplicationEvent> outboxEventCaptor;

    @BeforeEach
    void setUp() {
        createCustomerAccountUseCase = new CreateCustomerAccountUseCaseImpl(
                customerRepository,
                outboxEventStore,
                outboxEventDraftFactory
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
            verifyNoInteractions(outboxEventDraftFactory, outboxEventStore);
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

            OutboxEventDraft draft = outboxDraft();
            when(outboxEventDraftFactory.create(any(OutboxApplicationEvent.class)))
                    .thenReturn(draft);

            // when
            Customer result = createCustomerAccountUseCase.create(request);

            // then
            verify(customerRepository).findByUserId(userId);
            verify(customerRepository).createNewCustomer(customerArgumentCaptor.capture());
            verify(outboxEventDraftFactory).create(outboxEventCaptor.capture());
            verify(outboxEventStore).append(draft);

            Customer captured = customerArgumentCaptor.getValue();
            OutboxApplicationEvent outboxEvent = outboxEventCaptor.getValue();

            assertThat(captured).isNotNull();
            assertThat(captured.getId()).isNotNull();
            assertThat(captured.getUserId()).isEqualTo(userId);
            assertThat(captured.getCustomerDisplayName())
                    .isEqualTo(CustomerDisplayName.optional(request.displayName()).orElse(null));

            assertThat(result).isSameAs(captured);

            assertThat(outboxEvent).isInstanceOf(CustomerAccountCreatedEvent.class);
            assertThat(outboxEvent.eventKey()).isEqualTo("customer-account-created");
            assertThat(outboxEvent.aggregateId()).isEqualTo(captured.getId().value().toString());
            assertThat(outboxEvent.schemaVersion()).isEqualTo(1);

            CustomerAccountCreatedEvent customerAccountCreatedEvent = (CustomerAccountCreatedEvent) outboxEvent;
            assertThat(customerAccountCreatedEvent.customerId()).isEqualTo(captured.getId().value());
            assertThat(customerAccountCreatedEvent.userId()).isEqualTo("123");
            assertThat(customerAccountCreatedEvent.displayName()).isEqualTo("Me");

            verifyNoMoreInteractions(customerRepository);
            verifyNoMoreInteractions(outboxEventDraftFactory);
            verifyNoMoreInteractions(outboxEventStore);
        }
    }

    private OutboxEventDraft outboxDraft() {
        return new OutboxEventDraft(
                UUID.randomUUID(),
                "customer-account-created",
                "customer",
                "aggregate-id",
                "customer.account-created.v1",
                OBJECT_MAPPER.createObjectNode(),
                Instant.parse("2026-01-01T12:00:00Z")
        );
    }
}
