package com.bachratus.demo.application.useCases;

import com.bachratus.demo.application.events.CustomerAccountCreatedEvent;
import com.bachratus.demo.application.events.OutboxEventDraft;
import com.bachratus.demo.application.ports.out.CustomerRepository;
import com.bachratus.demo.application.ports.out.OutboxEventStore;
import com.bachratus.demo.application.request.CreateCustomerAccountRequest;
import com.bachratus.demo.application.ports.in.CreateCustomerAccountUseCase;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.CustomerDisplayName;
import com.bachratus.demo.domain.customer.UserId;
import com.bachratus.demo.domain.shared.exception.AlreadyExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
@Service
public class CreateCustomerAccountUseCaseImpl implements CreateCustomerAccountUseCase {

    private final CustomerRepository customerRepository;
    private final OutboxEventStore outboxEventStore;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public Customer create(
            CreateCustomerAccountRequest request
    ) {
        UserId userId = UserId.of(request.subject());

        Optional<Customer> existing = customerRepository.findByUserId(userId);

        if (existing.isPresent()) {
            throw new AlreadyExistsException(Customer.class, "userId", userId.value());
        }

        Customer customer = Customer.builder()
                .id(CustomerId.create())
                .displayName(CustomerDisplayName.optional(request.displayName()).orElse(null))
                .userId(userId)
                .build();

        Customer savedCustomer = customerRepository.createNewCustomer(customer);

        outboxEventStore.append(toCustomerAccountCreatedEvent(savedCustomer));

        return savedCustomer;
    }

    private OutboxEventDraft toCustomerAccountCreatedEvent(Customer customer) {
        CustomerAccountCreatedEvent payload = CustomerAccountCreatedEvent.from(customer);

        return OutboxEventDraft.create(
                CustomerAccountCreatedEvent.TOPIC_KEY,
                CustomerAccountCreatedEvent.AGGREGATE_TYPE,
                customer.getId().value().toString(),
                CustomerAccountCreatedEvent.EVENT_TYPE,
                objectMapper.valueToTree(payload),
                Instant.now(clock)
        );
    }
}
