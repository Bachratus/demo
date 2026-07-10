package com.bachratus.demo.infra.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExceptionCodeExtractorTest {

    @Test
    void shouldExtractCodeFromRuntimeException() {
        Throwable exception = new UserNotFoundException();

        String result = ExceptionCodeExtractor.extract(exception);

        assertThat(result).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void shouldExtractCodeFromCheckedException() {
        Throwable exception = new PaymentRejectedException();

        String result = ExceptionCodeExtractor.extract(exception);

        assertThat(result).isEqualTo("PAYMENT_REJECTED");
    }

    @Test
    void shouldExtractCodeFromError() {
        Throwable exception = new DatabaseConnectionError();

        String result = ExceptionCodeExtractor.extract(exception);

        assertThat(result).isEqualTo("DATABASE_CONNECTION_ERROR");
    }

    @Test
    void shouldExtractCodeFromThrowableWithoutExceptionSuffix() {
        Throwable exception = new InvalidRequest();

        String result = ExceptionCodeExtractor.extract(exception);

        assertThat(result).isEqualTo("INVALID_REQUEST");
    }

    private static class UserNotFoundException extends RuntimeException {
    }

    private static class PaymentRejectedException extends Exception {
    }

    private static class DatabaseConnectionError extends Error {
    }

    private static class InvalidRequest extends Throwable {
    }
}