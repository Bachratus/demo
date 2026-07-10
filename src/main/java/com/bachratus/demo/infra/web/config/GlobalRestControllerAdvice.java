package com.bachratus.demo.infra.web.config;

import com.bachratus.demo.domain.shared.exception.AlreadyExistsException;
import com.bachratus.demo.domain.shared.exception.DomainException;
import com.bachratus.demo.domain.shared.exception.validation.ValidationException;
import com.nimbusds.jose.shaded.gson.stream.MalformedJsonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice(annotations = RestController.class)
public class GlobalRestControllerAdvice {

    // --------------------------- VALIDATION ---------------------------

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(ValidationException ex) {
        return validationError(ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMalformedJsonException(HttpMessageNotReadableException ex) {
        return new ErrorResponse("VALIDATION", "MalformedJson", null, ex.getMessage());
    }

    private ErrorResponse validationError(ValidationException ex){
        log.debug("Validation failed for field '{}': {}", ex.getField(), ex.getMessage());

        String reasonCode = ex.getMessage().contains(": ")
                ? ex.getMessage().split(": ")[1]
                : ex.getMessage();

        return new ErrorResponse("VALIDATION", reasonCode.toUpperCase(), ex.getField(), ex.getMessage());
    }

    // --------------------------- DOMAIN ---------------------------

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(AlreadyExistsException.class)
    public ErrorResponse handleAlreadyExistsException(AlreadyExistsException ex) {
        return domainError(ex);
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(DomainException.class)
    public ErrorResponse handleDomainException(DomainException ex) {
        return domainError(ex);
    }

    private ErrorResponse domainError(DomainException ex) {
        log.warn("Domain rule violation: {}", ex.getMessage());

        String code = ex.getClass().getSimpleName()
                .replace("Exception", "")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();

        return new ErrorResponse("DOMAIN", code, null, ex.getMessage());
    }

    // --------------------------- INFRASTRUCTURE ---------------------------

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict", ex);
        return new ErrorResponse("DATABASE", "OPTIMISTIC_LOCK_CONFLICT", null,
                "Resource was modified by another request");
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected server error", ex);
        return new ErrorResponse("SERVER", "INTERNAL_SERVER_ERROR", null,
                "Unexpected server error");
    }
}