package com.bachratus.demo.application.useCases;

import com.bachratus.demo.application.ports.out.CustomerRepository;
import com.bachratus.demo.application.request.CreateCustomerAccountRequest;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerDisplayName;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.UserId;
import com.bachratus.demo.domain.shared.exception.AlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCustomerAccountUseCaseImplTest {

    @Mock
    CustomerRepository customerRepository;

    @InjectMocks
    CreateCustomerAccountUseCaseImpl createCustomerAccountUseCase;

    @Captor
    ArgumentCaptor<Customer> customerArgumentCaptor;

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

            Customer captured = customerArgumentCaptor.getValue();

            assertThat(captured).isNotNull();
            assertThat(captured.getId()).isNotNull();
            assertThat(captured.getUserId()).isEqualTo(userId);
            assertThat(captured.getCustomerDisplayName())
                    .isEqualTo(CustomerDisplayName.optional(request.displayName()).orElse(null));

            assertThat(result).isSameAs(captured);

            verifyNoMoreInteractions(customerRepository);
        }
    }
}