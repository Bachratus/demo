package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerIdTest {

    @Nested
    @DisplayName("Tests for create() method")
    class create {

        @Test
        void shouldReturnCustomerIdWithNotNullValue() {
            // when & then
            assertThat(CustomerId.create()).isNotNull();
            assertThat(CustomerId.create().value()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tests for of(UUID) method")
    class of {

        @Test
        void shouldReturnValidIdWhenParameterIsNotNull() {
            // given
            UUID id = UUID.randomUUID();

            // when
            CustomerId customerId = CustomerId.of(id);

            // then
            assertThat(customerId).isNotNull();
            assertThat(customerId.value()).isEqualTo(id);
        }

        @Test
        void shouldThrowExceptionWhenParameterIsNull() {
            // then
            assertThatThrownBy(() -> CustomerId.of(null)).isInstanceOf(MissingRequiredFieldException.class);
        }
    }
}