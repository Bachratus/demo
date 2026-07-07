package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;

public record UserId(String value) {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new MissingRequiredFieldException("userId");
        }

        value = value.trim();
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}