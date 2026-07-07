package com.bachratus.demo.application.useCases;

import com.bachratus.demo.application.ports.out.CustomerRepository;
import com.bachratus.demo.application.request.CreateCustomerAccountRequest;
import com.bachratus.demo.application.ports.in.CreateCustomerAccountUseCase;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.DisplayName;
import com.bachratus.demo.domain.customer.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CreateCustomerAccountUseCaseImpl implements CreateCustomerAccountUseCase {

    private final CustomerRepository customerRepository;

    @Override
    public Customer create(
            CreateCustomerAccountRequest request,
            String subject
    ) {
        Customer customer = Customer.builder()
                .id(CustomerId.create())
                .displayName(DisplayName.optional(request.displayName()).orElse(null))
                .userId(UserId.of(subject))
                .build();

        return customerRepository.createNewCustomer(customer);
    }
}
