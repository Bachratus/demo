package com.bachratus.demo.application.ports.in;

import com.bachratus.demo.application.request.CreateCustomerAccountRequest;
import com.bachratus.demo.domain.customer.Customer;

public interface CreateCustomerAccountUseCase {

    Customer create(
            CreateCustomerAccountRequest request
    );
}
