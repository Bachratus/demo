package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.utils.ValidationUtils;

import java.util.Optional;

public record DisplayName(String value) {

    public DisplayName {
        if (value == null || value.isBlank()) {
            ValidationUtils.requireNotNull(value, "displayName");
        }

        value = value.trim();
    }

    public static DisplayName required(String value) {
        return new DisplayName(value);
    }

    public static Optional<DisplayName> optional(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new DisplayName(value));
    }
}