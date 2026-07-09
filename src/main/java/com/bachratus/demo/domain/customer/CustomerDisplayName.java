package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;

import java.util.Optional;

public record CustomerDisplayName(String value) {

    public CustomerDisplayName {
        if (value == null || value.isBlank()) {
            throw new MissingRequiredFieldException("displayName");
        }

        value = value.trim();
    }

    public static CustomerDisplayName required(String value) {
        return new CustomerDisplayName(value);
    }

    public static Optional<CustomerDisplayName> optional(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new CustomerDisplayName(value));
    }
}