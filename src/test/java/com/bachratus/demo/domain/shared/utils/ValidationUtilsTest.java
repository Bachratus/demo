package com.bachratus.demo.domain.shared.utils;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilsTest {

    @DisplayName("Tests for requireNotNull(T, String) method")
    @Nested
    class RequireNotNull {

        @Test
        void shouldReturnValueWhenValueIsNotNull() {
            // given
            String value = "test-value";

            // when
            String result = ValidationUtils.requireNotNull(value, "fieldName");

            // then
            assertThat(result).isEqualTo(value);
        }

        @Test
        void shouldReturnSameObjectInstanceWhenValueIsNotNull() {
            // given
            Object value = new Object();

            // when
            Object result = ValidationUtils.requireNotNull(value, "fieldName");

            // then
            assertThat(result).isSameAs(value);
        }

        @Test
        void shouldThrowMissingRequiredFieldExceptionWhenValueIsNull() {
            // when & then
            //noinspection DataFlowIssue
            assertThatThrownBy(() -> ValidationUtils.requireNotNull(null, "userId"))
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("userId");
        }
    }
}