package com.bachratus.demo.domain.shared.value;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

/**
 * A generic Value Object representing a text description.
 * <p>
 * This class is intended to be used across different domain entities
 * to handle optional text fields consistently. It ensures the value
 * is trimmed and provides a safe way to handle empty descriptions.
 * </p>
 */
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TextProperty {

    private final String value;

    /**
     * Factory method for creating a {@code Description}.
     *
     * @param description The raw description string.
     * @return An {@link Optional} containing the {@code Description}, or empty if input is null/blank.
     */
    public static Optional<TextProperty> of(String description) {
        if (description == null || description.isBlank()) return Optional.empty();

        return Optional.of(new TextProperty(description.trim()));
    }

    /**
     * Reconstructs the object from persistence.
     *
     * @param description The raw string from the database.
     * @return A {@code Description} instance, or {@code null} if the input is empty.
     */
    public static TextProperty restore(String description) {
        if (description == null || description.isBlank()) return null;
        return new TextProperty(description);
    }

    @Override
    public String toString() {
        return value;
    }
}