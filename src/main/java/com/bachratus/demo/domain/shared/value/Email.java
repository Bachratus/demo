package com.bachratus.demo.domain.shared.value;

import com.bachratus.demo.domain.shared.exception.validation.InvalidParameterFormatException;
import com.bachratus.demo.domain.shared.exception.validation.InvalidParameterLengthException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Value Object representing a validated and normalized email address.
 * <p>
 * This class ensures that all email addresses within the domain:
 * <ul>
 * <li>Are syntactically correct according to a standard RFC-compliant pattern.</li>
 * <li>Do not contain internal whitespace or control characters.</li>
 * <li>Are normalized to lowercase to prevent duplicate accounts with different casing.</li>
 * <li>Adhere to a maximum length of 100 characters to protect against ReDoS attacks.</li>
 * </ul>
 * </p>
 */
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Email {

    /**
     * Standard email regex pattern.
     * Note: Full RFC 5322 compliance is extremely complex; this pattern covers 99.9% of real-world use cases.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final int MAX_LENGTH = 100;

    /** The validated, trimmed, and lowercased email string. */
    private final String value;

    /**
     * Factory method for creating and validating an Email instance.
     * <p>
     * Validation steps:
     * 1. Null/Blank check.
     * 2. Internal whitespace check (Fast-fail).
     * 3. Length check (ReDoS protection).
     * 4. Regex format validation.
     * </p>
     *
     * @param email The raw email string to validate.
     * @return An {@link Optional} containing a valid {@code Email} instance, or empty if input is null/blank.
     * @throws InvalidParameterLengthException if the email exceeds 100 characters.
     * @throws InvalidParameterFormatException     if the email contains internal spaces or has an invalid format.
     */
    public static Optional<Email> of(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        // 1. Initial processing (Trim and Lowercase for consistency)
        String processed = email.trim().toLowerCase();

        // 2. Internal whitespace check (Fast-fail to avoid unnecessary Regex processing)
        if (processed.chars().anyMatch(Character::isWhitespace)) throw InvalidParameterFormatException.internalWhitespace("email");

        // 3. Length check (Crucial for preventing Regex Denial of Service - ReDoS)
        if (processed.length() > MAX_LENGTH) throw InvalidParameterLengthException.exceeded("email", MAX_LENGTH);

        // 4. Regex validation for general format
        if (!EMAIL_PATTERN.matcher(processed).matches()) throw InvalidParameterFormatException.controlCharacters("email");

        return Optional.of(new Email(processed));
    }

    /**
     * Reconstructs the Email object from the persistence layer.
     * <p>
     * This method skips standard domain validation to ensure system stability
     * when loading existing data, but still applies basic trimming and normalization.
     * </p>
     *
     * @param email The raw email string from the database.
     * @return An {@link Email} instance, or {@code null} if the input is null/blank.
     */
    public static Email restore(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return new Email(email.trim().toLowerCase());
    }

    @Override
    public String toString() {
        return value;
    }
}