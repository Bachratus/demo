package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.exception.validation.MissingRequiredFieldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    private static Stream<Arguments> customerParametersForSuccessfulCustomerCreation() {
        return Stream.of(
                Arguments.arguments(
                        "return valid object when args are not null",
                        CustomerId.create(),
                        UserId.of("1"),
                        CustomerDisplayName.required("Me")
                ),
                Arguments.arguments(
                        "return valid object when displayName is null",
                        CustomerId.create(),
                        UserId.of("1"),
                        null
                )
        );
    }

    @DisplayName("Tests for restore(CustomerId, UserId, CustomerDisplayName) method")
    @Nested
    class Restore {

        @Test
        void shouldThrowExceptionWhenIdIsNull() {
            // when & then
            assertThatThrownBy(() -> Customer.restore(null, UserId.of("1"), null))
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("id");

        }

        @Test
        void shouldThrowExceptionWhenUserIdIsNull() {
            // when & then
            assertThatThrownBy(() -> Customer.restore(CustomerId.create(), null, null))
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("userId");
        }

        @DisplayName("Should:")
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.domain.customer.CustomerTest#customerParametersForSuccessfulCustomerCreation")
        void shouldReturnValidCustomerWithExpectedPropertyValues(
                String scenario,
                CustomerId customerId,
                UserId userId,
                CustomerDisplayName displayName
        ) {
            // when
            Customer customer = Customer.restore(customerId, userId, displayName);

            // then
            assertThat(customer.getId()).isEqualTo(customerId);
            assertThat(customer.getUserId()).isEqualTo(userId);
            assertThat(customer.getCustomerDisplayName()).isEqualTo(displayName);
        }
    }

    @DisplayName("Tests for Customer builder")
    @Nested
    class Builder {

        @Test
        void shouldThrowExceptionWhenIdIsNull() {
            // when & then
            assertThatThrownBy(() -> Customer.builder().userId(UserId.of("1")).build())
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("id");

        }

        @Test
        void shouldThrowExceptionWhenUserIdIsNull() {
            // when & then
            assertThatThrownBy(() -> Customer.builder().id(CustomerId.create()).build())
                    .isInstanceOf(MissingRequiredFieldException.class)
                    .hasMessageContaining("userId");
        }

        @DisplayName("Should:")
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bachratus.demo.domain.customer.CustomerTest#customerParametersForSuccessfulCustomerCreation")
        void shouldReturnValidCustomerWithExpectedPropertyValues(
                String scenario,
                CustomerId customerId,
                UserId userId,
                CustomerDisplayName displayName
        ) {
            // when
            Customer customer = Customer.builder()
                    .id(customerId)
                    .userId(userId)
                    .displayName(displayName)
                    .build();

            // then
            assertThat(customer.getId()).isEqualTo(customerId);
            assertThat(customer.getUserId()).isEqualTo(userId);
            assertThat(customer.getCustomerDisplayName()).isEqualTo(displayName);
        }
    }

    @DisplayName("Tests for equals(Object) and hashCode() methods")
    @Nested
    class EqualsAndHashCode {

        @Test
        void shouldBeEqualWhenCustomersHaveSameId() {
            // given
            CustomerId id = CustomerId.create();

            Customer customer1 = Customer.restore(
                    id,
                    UserId.of("1"),
                    CustomerDisplayName.required("John")
            );

            Customer customer2 = Customer.restore(
                    id,
                    UserId.of("2"),
                    CustomerDisplayName.required("Jane")
            );

            // then
            assertThat(customer1).isEqualTo(customer2);
            assertThat(customer1.hashCode()).isEqualTo(customer2.hashCode());
        }

        @Test
        void shouldNotBeEqualWhenCustomersHaveDifferentIds() {
            // given
            Customer customer1 = Customer.restore(
                    CustomerId.create(),
                    UserId.of("1"),
                    CustomerDisplayName.required("John")
            );

            Customer customer2 = Customer.restore(
                    CustomerId.create(),
                    UserId.of("1"),
                    CustomerDisplayName.required("John")
            );

            // then
            assertThat(customer1).isNotEqualTo(customer2);
        }

        @Test
        void shouldNotBeEqualToNull() {
            Customer customer = Customer.restore(
                    CustomerId.create(),
                    UserId.of("1"),
                    CustomerDisplayName.required("John")
            );

            assertThat(customer).isNotEqualTo(null);
        }

        @Test
        void shouldNotBeEqualToDifferentType() {
            Customer customer = Customer.restore(
                    CustomerId.create(),
                    UserId.of("1"),
                    CustomerDisplayName.required("John")
            );

            //noinspection AssertBetweenInconvertibleTypes
            assertThat(customer).isNotEqualTo("not a customer");
        }
    }
}