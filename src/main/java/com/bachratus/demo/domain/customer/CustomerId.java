package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.utils.ValidationUtils;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        ValidationUtils.requireNotNull(value, "customerId");
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    public static CustomerId create() {
        return new CustomerId(UUID.randomUUID());
    }
}