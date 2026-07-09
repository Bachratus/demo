package com.bachratus.demo.domain.shared.utils;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static <T> T requireNotNull(T value, String field) {
        if (value == null) {
            throw new MissingRequiredFieldException(field);
        }
        return value;
    }
}
