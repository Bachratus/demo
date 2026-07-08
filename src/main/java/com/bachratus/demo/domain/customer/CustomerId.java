package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new MissingRequiredFieldException("customerId");
        }
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    public static CustomerId create() {
        return new CustomerId(UUID.randomUUID());
    }
}