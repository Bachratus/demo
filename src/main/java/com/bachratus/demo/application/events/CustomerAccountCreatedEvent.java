package com.bachratus.demo.application.events;

import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerDisplayName;

import java.util.UUID;

public record CustomerAccountCreatedEvent(
        int schemaVersion,
        UUID customerId,
        String userId,
        String displayName
) implements OutboxApplicationEvent {

    private static final int SCHEMA_VERSION = 1;
    private static final String EVENT_KEY = "customer-account-created";

    public static CustomerAccountCreatedEvent from(Customer customer) {
        CustomerDisplayName displayName = customer.getCustomerDisplayName();

        return new CustomerAccountCreatedEvent(
                SCHEMA_VERSION,
                customer.getId().value(),
                customer.getUserId().value(),
                displayName == null ? null : displayName.value()
        );
    }

    @Override
    public String eventKey() {
        return EVENT_KEY;
    }

    @Override
    public String aggregateId() {
        return customerId.toString();
    }
}
