package com.bachratus.demo.infra.db.customer;

import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.DisplayName;
import com.bachratus.demo.domain.customer.UserId;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CustomerMapper {

    public Customer toDomain(CustomerJpa customerJpa) {
        Objects.requireNonNull(customerJpa, "customerJpa cannot be null");

        return Customer.restore(
                CustomerId.of(customerJpa.getPublicId()),
                UserId.of(customerJpa.getUserId()),
                DisplayName.optional(customerJpa.getDisplayName()).orElse(null)
        );
    }

    public CustomerJpa toEntity(Customer customer) {
        Objects.requireNonNull(customer, "customer cannot be null");

        CustomerJpa customerJpa = new CustomerJpa();

        customerJpa.setPublicId(customer.getId().value());
        customerJpa.setUserId(customer.getUserId().value());
        customerJpa.setDisplayName(toDisplayNameValue(customer.getDisplayName()));
        return customerJpa;
    }

    public void updateDisplayName(Customer customer, CustomerJpa customerJpa) {
        Objects.requireNonNull(customer, "customer cannot be null");
        Objects.requireNonNull(customerJpa, "customerJpa cannot be null");

        customerJpa.setDisplayName(toDisplayNameValue(customer.getDisplayName()));
    }

    private String toDisplayNameValue(DisplayName displayName) {
        return displayName == null ? null : displayName.value();
    }
}