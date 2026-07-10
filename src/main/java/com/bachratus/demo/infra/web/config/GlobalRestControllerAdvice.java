package com.bachratus.demo.infra.web.config;

import com.bachratus.demo.domain.shared.exception.AlreadyExistsException;
import com.bachratus.demo.domain.shared.exception.DomainException;
import com.bachratus.demo.domain.shared.exception.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
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
    public ErrorResponse handleValidationException(RuntimeException ex) {
        return validationError(ex);
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMalformedJsonException(RuntimeException ex) {
        return validationError(ex);
    }

    private ErrorResponse validationError(RuntimeException ex) {
        log.debug("Validation failed for reason: {}", ex.getMessage());
        return new ErrorResponse(ErrorType.VALIDATION, ExceptionCodeExtractor.extract(ex), ex.getMessage());
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
        return new ErrorResponse(ErrorType.DOMAIN, ExceptionCodeExtractor.extract(ex), ex.getMessage());
    }

    // --------------------------- INFRASTRUCTURE ---------------------------

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict", ex);
        return new ErrorResponse(ErrorType.DOMAIN, "OPTIMISTIC_LOCK_CONFLICT", "Resource was modified by another request");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception ex) {
        log.error("Unexpected server error", ex);
        return new ErrorResponse(ErrorType.SERVER, "INTERNAL_SERVER_ERROR", "Unexpected server error");
    }
}