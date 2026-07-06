package com.bachratus.demo.domain.shared.exception.validation;

import com.bachratus.demo.domain.shared.exception.DomainException;
import lombok.Getter;

/**
 * Base exception for all data validation failures within the domain.
 */
@Getter
public abstract class ValidationException extends DomainException {

    /**
     * The name of the specific field or property that failed validation.
     */
    private final String field;

    /**
     * Constructs a new validation exception with the faulty field and a descriptive message.
     *
     * @param field   The name of the attribute (e.g., "username").
     * @param message The reason why the validation failed.
     */
    public ValidationException(String field, String message) {
        super(String.format("Field '%s': %s", field, message));
        this.field = field;
    }
}