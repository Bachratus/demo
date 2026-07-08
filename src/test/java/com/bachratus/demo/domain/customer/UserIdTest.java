package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @DisplayName("Tests for of(String) method")
    @Nested
    class of {

        @Test
        void shouldThrowExceptionWhenValueIsNullOrBlank() {
            // when & then
            assertThatThrownBy(() -> UserId.of(null))
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("userId");

            assertThatThrownBy(() -> UserId.of("   "))
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        void shouldReturnUserIdWithTrimmedValue(){
            // given
            String id = " 12321d ";

            // when
            UserId result = UserId.of(id);

            // then
            assertThat(result.value()).isEqualTo("12321d");
        }
    }
}