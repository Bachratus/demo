package com.bachratus.demo.infra.db.customer;

import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerDisplayName;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerMapperTest {

    private final CustomerMapper customerMapper = new CustomerMapper();

    @DisplayName("Tests for toDomain(CustomerJpa) method")
    @Nested
    class ToDomain {

        @Test
        void shouldThrowExceptionWhenCustomerJpaIsNull() {
            // when & then
            assertThatThrownBy(() -> customerMapper.toDomain(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("customerJpa cannot be null");
        }

        @Test
        void shouldMapCustomerJpaToDomainCustomerWhenDisplayNameIsNotNull() {
            // given
            UUID publicId = UUID.randomUUID();

            CustomerJpa customerJpa = new CustomerJpa();
            customerJpa.setPublicId(publicId);
            customerJpa.setUserId("user-123");
            customerJpa.setDisplayName("John");

            // when
            Customer customer = customerMapper.toDomain(customerJpa);

            // then
            assertThat(customer.getId()).isEqualTo(CustomerId.of(publicId));
            assertThat(customer.getUserId()).isEqualTo(UserId.of("user-123"));
            assertThat(customer.getCustomerDisplayName())
                    .isEqualTo(CustomerDisplayName.required("John"));
        }

        @Test
        void shouldMapCustomerJpaToDomainCustomerWhenDisplayNameIsNull() {
            // given
            UUID publicId = UUID.randomUUID();

            CustomerJpa customerJpa = new CustomerJpa();
            customerJpa.setPublicId(publicId);
            customerJpa.setUserId("user-123");
            customerJpa.setDisplayName(null);

            // when
            Customer customer = customerMapper.toDomain(customerJpa);

            // then
            assertThat(customer.getId()).isEqualTo(CustomerId.of(publicId));
            assertThat(customer.getUserId()).isEqualTo(UserId.of("user-123"));
            assertThat(customer.getCustomerDisplayName()).isNull();
        }
    }

    @DisplayName("Tests for toEntity(Customer) method")
    @Nested
    class ToEntity {

        @Test
        void shouldThrowExceptionWhenCustomerIsNull() {
            // when & then
            assertThatThrownBy(() -> customerMapper.toEntity(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("customer cannot be null");
        }

        @Test
        void shouldMapDomainCustomerToCustomerJpaWhenDisplayNameIsNotNull() {
            // given
            CustomerId customerId = CustomerId.create();

            Customer customer = Customer.restore(
                    customerId,
                    UserId.of("user-123"),
                    CustomerDisplayName.required("John")
            );

            // when
            CustomerJpa customerJpa = customerMapper.toEntity(customer);

            // then
            assertThat(customerJpa).isNotNull();
            assertThat(customerJpa.getPublicId()).isEqualTo(customerId.value());
            assertThat(customerJpa.getUserId()).isEqualTo("user-123");
            assertThat(customerJpa.getDisplayName()).isEqualTo("John");
        }

        @Test
        void shouldMapDomainCustomerToCustomerJpaWhenDisplayNameIsNull() {
            // given
            CustomerId customerId = CustomerId.create();

            Customer customer = Customer.restore(
                    customerId,
                    UserId.of("user-123"),
                    null
            );

            // when
            CustomerJpa customerJpa = customerMapper.toEntity(customer);

            // then
            assertThat(customerJpa).isNotNull();
            assertThat(customerJpa.getPublicId()).isEqualTo(customerId.value());
            assertThat(customerJpa.getUserId()).isEqualTo("user-123");
            assertThat(customerJpa.getDisplayName()).isNull();
        }
    }

    @DisplayName("Tests for updateDisplayName(Customer, CustomerJpa) method")
    @Nested
    class UpdateDisplayName {

        @Test
        void shouldThrowExceptionWhenCustomerIsNull() {
            // given
            CustomerJpa customerJpa = new CustomerJpa();

            // when & then
            assertThatThrownBy(() -> customerMapper.updateDisplayName(null, customerJpa))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("customer cannot be null");
        }

        @Test
        void shouldThrowExceptionWhenCustomerJpaIsNull() {
            // given
            Customer customer = Customer.restore(
                    CustomerId.create(),
                    UserId.of("user-123"),
                    CustomerDisplayName.required("John")
            );

            // when & then
            assertThatThrownBy(() -> customerMapper.updateDisplayName(customer, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("customerJpa cannot be null");
        }

        @Test
        void shouldUpdateDisplayNameWhenCustomerDisplayNameIsNotNull() {
            // given
            Customer customer = Customer.restore(
                    CustomerId.create(),
                    UserId.of("user-123"),
                    CustomerDisplayName.required("New name")
            );

            CustomerJpa customerJpa = new CustomerJpa();
            customerJpa.setDisplayName("Old name");

            // when
            customerMapper.updateDisplayName(customer, customerJpa);

            // then
            assertThat(customerJpa.getDisplayName()).isEqualTo("New name");
        }

        @Test
        void shouldSetDisplayNameToNullWhenCustomerDisplayNameIsNull() {
            // given
            Customer customer = Customer.restore(
                    CustomerId.create(),
                    UserId.of("user-123"),
                    null
            );

            CustomerJpa customerJpa = new CustomerJpa();
            customerJpa.setDisplayName("Old name");

            // when
            customerMapper.updateDisplayName(customer, customerJpa);

            // then
            assertThat(customerJpa.getDisplayName()).isNull();
        }

        @Test
        void shouldUpdateOnlyDisplayName() {
            // given
            UUID publicId = UUID.randomUUID();

            Customer customer = Customer.restore(
                    CustomerId.create(),
                    UserId.of("new-user-id"),
                    CustomerDisplayName.required("New name")
            );

            CustomerJpa customerJpa = new CustomerJpa();
            customerJpa.setPublicId(publicId);
            customerJpa.setUserId("old-user-id");
            customerJpa.setDisplayName("Old name");

            // when
            customerMapper.updateDisplayName(customer, customerJpa);

            // then
            assertThat(customerJpa.getPublicId()).isEqualTo(publicId);
            assertThat(customerJpa.getUserId()).isEqualTo("old-user-id");
            assertThat(customerJpa.getDisplayName()).isEqualTo("New name");
        }
    }
}