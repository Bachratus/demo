package com.bachratus.demo.infra.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ExceptionCodeExtractorTest {

    @Test
    void shouldExtractCodeFromRuntimeException() {
        Exception exception = new UserNotFoundException();

        String result = ExceptionCodeExtractor.extract(exception);

        assertThat(result).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void shouldExtractCodeFromCheckedException() {
        Exception exception = new IOException();

        String result = ExceptionCodeExtractor.extract(exception);

        assertThat(result).isEqualTo("IO");
    }

    private static class UserNotFoundException extends RuntimeException {
    }
}