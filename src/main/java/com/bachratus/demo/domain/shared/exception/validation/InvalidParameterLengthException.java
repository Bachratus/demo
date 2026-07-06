package com.bachratus.demo.domain.shared.exception.validation;

import lombok.Getter;

/**
 * Exception thrown when a string parameter does not meet length requirements.
 * Supports minimum, maximum, and range-based validation errors.
 */
@Getter
public class InvalidParameterLengthException extends ValidationException {

    private final String parameterName;
    private final Integer limit;

    private InvalidParameterLengthException(String parameterName, Integer limit, String reason) {
        super(parameterName, reason);
        this.parameterName = parameterName;
        this.limit = limit;
    }

    /**
     * Factory method for a required range of characters.
     * Example: "must_be_between_3_and_50_chars"
     */
    public static InvalidParameterLengthException range(String parameterName, int min, int max) {
        return new InvalidParameterLengthException(
                parameterName,
                null, // limit is null because we provide a range in the message
                "must_be_between_%d_and_%d_chars".formatted(min, max)
        );
    }

    /**
     * Factory method for exceeding maximum length.
     */
    public static InvalidParameterLengthException exceeded(String parameterName, int max) {
        return new InvalidParameterLengthException(
                parameterName,
                max,
                "must_have_at_most_%d_chars".formatted(max)
        );
    }

    /**
     * Factory method for not reaching minimum length.
     */
    public static InvalidParameterLengthException tooShort(String parameterName, int min) {
        return new InvalidParameterLengthException(
                parameterName,
                min,
                "must_have_at_least_%d_chars".formatted(min)
        );
    }
}