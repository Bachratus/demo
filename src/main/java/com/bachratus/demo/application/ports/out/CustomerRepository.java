package com.bachratus.demo.application.ports.out;

import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.UserId;

import java.util.Optional;

public interface CustomerRepository {

    Optional<Customer> findById(CustomerId customerId);

    Optional<Customer> findByUserId(UserId userId);

    Customer createNewCustomer(Customer customer);
}
