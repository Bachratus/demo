package com.bachratus.demo.domain.shared.value;

/**
 * A domain policy definition that determines the visibility and constraint
 * requirements for a specific field within a given context (e.g., registration or profile update).
 * <p>
 * This record is typically used to drive dynamic UI rendering or to perform
 * conditional validation in application services.
 * </p>
 *
 * @param available Indicates if the field should be presented to the user and processed by the system.
 * If {@code false}, the field is considered disabled or hidden.
 * @param required  Indicates if the field must contain a valid value.
 * This is only applicable if {@code available} is {@code true}.
 */
public record FieldPolicy(
        boolean available,
        boolean required
) {
    /**
     * Factory method for a field that is both visible and mandatory.
     * * @return A mandatory {@link FieldPolicy}.
     */
    public static FieldPolicy mandatory() {
        return new FieldPolicy(true, true);
    }

    /**
     * Factory method for a field that is visible but not mandatory.
     * * @return An optional {@link FieldPolicy}.
     */
    public static FieldPolicy optional() {
        return new FieldPolicy(true, false);
    }

    /**
     * Factory method for a field that is completely disabled or hidden.
     * * @return A disabled {@link FieldPolicy}.
     */
    public static FieldPolicy disabled() {
        return new FieldPolicy(false, false);
    }
}