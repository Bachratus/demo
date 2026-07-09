package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerDisplayNameTest {

    @DisplayName("Tests for required(String) method")
    @Nested
    class required{

        @Test
        void shouldThrowExceptionWhenParameterIsNullOrBlank(){
            // when & then
            assertThatThrownBy(()-> CustomerDisplayName.required(null))
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("displayName");

            assertThatThrownBy(()-> CustomerDisplayName.required("   "))
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("displayName");
        }

        @Test
        void shouldReturnDisplayNameWithTrimmedValue(){
            // given
            String value = "  a ";

            // when
            CustomerDisplayName customerDisplayName = CustomerDisplayName.required(value);

            // then
            assertThat(customerDisplayName.value()).isEqualTo("a");
        }
    }

    @DisplayName("Tests for optional(String) method")
    @Nested
    class optional{

        @Test
        void shouldReturnEmptyWhenParameterIsNullOrBlank(){
            // when
            Optional<CustomerDisplayName> ofNull = CustomerDisplayName.optional(null);
            Optional<CustomerDisplayName> ofBlank = CustomerDisplayName.optional("  ");

            // then
            assertThat(ofNull).isEmpty();
            assertThat(ofBlank).isEmpty();
        }

        @Test
        void shouldReturnDisplayNameWithTrimmedValue(){
            // given
            String value = "  a ";

            // when
            Optional<CustomerDisplayName> displayName = CustomerDisplayName.optional(value);

            // then
            assertThat(displayName).isNotEmpty().hasValueSatisfying(dn ->assertThat(dn.value()).isEqualTo("a"));
        }
    }

}