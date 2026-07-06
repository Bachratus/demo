package com.bachratus.demo.domain.shared.exception;

/**
 * Base abstract class for all exceptions originating from the business domain.
 * <p>
 * This class extends {@link RuntimeException} and serves as the root of the domain
 * exception hierarchy. Subclasses should represent specific business rule violations,
 * validation errors, or process failures (e.g., {@code UserCreationException}).
 * </p>
 *
 * <p>Design Goals:</p>
 * <ul>
 *     <li><b>Separation of Concerns:</b> Distinguishes domain-specific errors from
 *         infrastructure-level exceptions (e.g., SQL errors or Network timeouts).</li>
 *     <li><b>Global Handling:</b> Allows for a unified approach in the API layer
 *         (e.g., {@code @ControllerAdvice}) to map these exceptions to appropriate
 *         HTTP status codes like 400, 409, or 422.</li>
 *     <li><b>Developer Intent:</b> Clearly signals that the error was expected
 *         and handled by the business logic, rather than being an unhandled bug.</li>
 * </ul>
 */
public abstract class DomainException extends RuntimeException {

    /**
     * Constructs a new domain exception with a detailed error message.
     *
     * @param message A descriptive message explaining the business rule violation.
     */
    protected DomainException(String message) {
        super(message);
    }
}