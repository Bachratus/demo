package com.bachratus.demo.domain.shared.exception.validation;

import lombok.Getter;

/**
 * Exception thrown when a domain parameter matches its length constraints
 * but violates internal structural rules or contains illegal characters.
 * <p>
 * This is a generic validation exception used for fields like Name, LastName,
 * or Login, where specific patterns (e.g., no control characters,
 * no special symbols) are required.
 * </p>
 *
 * <p><b>Common Reason Codes:</b></p>
 * <ul>
 * <li>{@code contains_control_characters} - For inputs with tabs, new lines, etc.</li>
 * <li>{@code invalid_characters} - For inputs with forbidden symbols.</li>
 * <li>{@code internal_whitespace} - For fields that must be a single word.</li>
 * </ul>
 *
 * @see ValidationException
 */
@Getter
public class InvalidParameterFormatException extends ValidationException {

    /**
     * Constructs a new exception for a specific field and reason.
     *
     * @param field  The name of the validated field (e.g., "name", "login").
     * @param reason A machine-readable code representing the violation.
     */
    public InvalidParameterFormatException(String field, String reason) {
        super(field, reason);
    }

    /**
     * Factory method for reporting illegal control characters (ASCII < 32).
     *
     * @param field The field name.
     * @return A pre-configured exception instance.
     */
    public static InvalidParameterFormatException controlCharacters(String field) {
        return new InvalidParameterFormatException(field, "contains_control_characters");
    }

    /**
     * Factory method for reporting unexpected internal whitespace.
     *
     * @param field The field name.
     * @return A pre-configured exception instance.
     */
    public static InvalidParameterFormatException internalWhitespace(String field) {
        return new InvalidParameterFormatException(field, "contains_internal_whitespace");
    }

    /**
     * For cases where input contains characters forbidden by domain policy
     * (e.g., special symbols in names or letters in phone numbers).
     */
    public static InvalidParameterFormatException illegalCharacters(String field){
        return new InvalidParameterFormatException(field, "contains_illegal_characters");
    }
}