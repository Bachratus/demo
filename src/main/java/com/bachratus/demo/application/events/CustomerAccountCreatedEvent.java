package com.bachratus.demo.application.events;

import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerDisplayName;

import java.util.UUID;

public record CustomerAccountCreatedEvent(
        int schemaVersion,
        UUID customerId,
        String userId,
        String displayName
) {

    public static final String TOPIC_KEY = "customer-account-created";
    public static final String EVENT_TYPE = "customer.account-created.v1";
    public static final String AGGREGATE_TYPE = "customer";

    public static CustomerAccountCreatedEvent from(Customer customer) {
        CustomerDisplayName displayName = customer.getCustomerDisplayName();

        return new CustomerAccountCreatedEvent(
                1,
                customer.getId().value(),
                customer.getUserId().value(),
                displayName == null ? null : displayName.value()
        );
    }
}
