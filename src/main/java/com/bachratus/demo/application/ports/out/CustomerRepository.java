package com.bachratus.demo.application.ports.out;

import com.bachratus.demo.domain.customer.Customer;

public interface CustomerRepository {

    Customer createNewCustomer(Customer customer);
}
