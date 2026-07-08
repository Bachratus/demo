package com.bachratus.demo.infra.web.config;

import com.bachratus.demo.domain.shared.exception.AlreadyExistsException;
import com.bachratus.demo.domain.shared.exception.DomainException;
import com.bachratus.demo.domain.shared.exception.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice(annotations = RestController.class)
public class GlobalRestControllerAdvice {

    // --------------------------- INFRASTRUCTURE ---------------------------

//    @ExceptionHandler(RateLimitExceededException.class)
//    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
//    public ErrorResponse handleRateLimitExceeded(
//            RateLimitExceededException ex,
//            Locale locale
//    ) {
//        log.warn("Rate limit exceeded. IP={}, URL={}", ex.getIp(), ex.getUrl());
//        return new ErrorResponse("SYSTEM", "TOO_MANY_REQUESTS", null, "Too many requests. Try again later");
//    }
//
//    @ExceptionHandler(OptimisticLockingFailureException.class)
//    @ResponseStatus(HttpStatus.CONFLICT)
//    public ErrorResponse handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
//        log.warn("Optimistic lock conflict detected: {}", ex.getMessage());
//        return new ErrorResponse("DATABASE", "OPTIMISTIC_LOCK_CONFLICT", null,
//                "The data you are trying to update has been modified by another process. Please refresh and try again.");
//    }
//
//    @ExceptionHandler(EntityPersistenceException.class)
//    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//    public ErrorResponse handleEntityPersistenceException(EntityPersistenceException ex) {
//        log.error("Persistence integrity failure for entity {}: ID [{}] not found after save.",
//                ex.getEntityName(), ex.getEntityId());
//        return new ErrorResponse("DATABASE", "PERSISTENCE_INTEGRITY_ERROR", ex.getEntityName(),
//                "Critical data integrity error occurred.");
//    }

    // --------------------------- DOMAIN ---------------------------

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleDomainException(DomainException ex) {
        return domainError(ex);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleAlreadyExistsException(AlreadyExistsException ex) {
        return domainError(ex);
    }

    private ErrorResponse domainError(RuntimeException ex) {
        log.warn("Domain rule violation: {}", ex.getMessage());

        String code = ex.getClass().getSimpleName()
                .replace("Exception", "")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();

        return new ErrorResponse("DOMAIN", code, null, ex.getMessage());
    }

    // --------------------------- VALIDATION ---------------------------

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(ValidationException ex) {
        log.debug("Validation failed for field '{}': {}", ex.getField(), ex.getMessage());

        String reasonCode = ex.getMessage().contains(": ")
                ? ex.getMessage().split(": ")[1]
                : ex.getMessage();

        return new ErrorResponse("VALIDATION", reasonCode.toUpperCase(), ex.getField(), ex.getMessage());
    }
}