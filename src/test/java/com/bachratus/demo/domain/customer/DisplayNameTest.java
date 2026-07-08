package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayNameTest {

    @DisplayName("Tests for required(String) method")
    @Nested
    class required{

        @Test
        void shouldThrowExceptionWhenParameterIsNullOrBlank(){
            // when & then
            assertThatThrownBy(()-> com.bachratus.demo.domain.customer.DisplayName.required(null))
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("displayName");

            assertThatThrownBy(()-> com.bachratus.demo.domain.customer.DisplayName.required("   "))
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("displayName");
        }

        @Test
        void shouldReturnDisplayNameWithTrimmedValue(){
            // given
            String value = "  a ";

            // when
            com.bachratus.demo.domain.customer.DisplayName displayName = com.bachratus.demo.domain.customer.DisplayName.required(value);

            // then
            assertThat(displayName.value()).isEqualTo("a");
        }
    }

    @DisplayName("Tests for optional(String) method")
    @Nested
    class optional{

        @Test
        void shouldReturnEmptyWhenParameterIsNullOrBlank(){
            // when
            Optional<com.bachratus.demo.domain.customer.DisplayName> ofNull = com.bachratus.demo.domain.customer.DisplayName.optional(null);
            Optional<com.bachratus.demo.domain.customer.DisplayName> ofBlank = com.bachratus.demo.domain.customer.DisplayName.optional("  ");

            // then
            assertThat(ofNull).isEmpty();
            assertThat(ofBlank).isEmpty();
        }

        @Test
        void shouldReturnDisplayNameWithTrimmedValue(){
            // given
            String value = "  a ";

            // when
            Optional<com.bachratus.demo.domain.customer.DisplayName> displayName = com.bachratus.demo.domain.customer.DisplayName.optional(value);

            // then
            assertThat(displayName).isNotEmpty().hasValueSatisfying(dn ->assertThat(dn.value()).isEqualTo("a"));
        }
    }

}