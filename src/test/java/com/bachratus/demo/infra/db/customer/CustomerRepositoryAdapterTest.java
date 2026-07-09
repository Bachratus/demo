package com.bachratus.demo.infra.db.customer;

import com.bachratus.demo.config.BaseJpaIntegrationTest;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerDisplayName;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({CustomerRepositoryAdapter.class, CustomerMapper.class})
class CustomerRepositoryAdapterTest extends BaseJpaIntegrationTest {

    @Autowired
    private CustomerRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @DisplayName("Tests for findById(CustomerId) method")
    @Nested
    class FindById {

        @Test
        void shouldThrowExceptionWhenCustomerIdIsNull() {
            // when & then
            assertThatThrownBy(() -> adapter.findById(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("customerId cannot be null");
        }

        @Test
        void shouldReturnEmptyWhenCustomerDoesNotExist() {
            // given
            CustomerId notExistingId = CustomerId.create();

            // when
            Optional<Customer> result = adapter.findById(notExistingId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldFindExistingCustomerWhenDisplayNameIsNull() {
            // given
            UUID existingCustomerId = UUID.randomUUID();
            String existingCustomerUserId = "123";

            persistCustomerJpa(existingCustomerId, existingCustomerUserId, null);
            entityManager.clear();

            // when
            Optional<Customer> result = adapter.findById(CustomerId.of(existingCustomerId));

            // then
            assertThat(result).hasValueSatisfying(value -> {
                assertThat(value.getId()).isEqualTo(CustomerId.of(existingCustomerId));
                assertThat(value.getUserId()).isEqualTo(UserId.of(existingCustomerUserId));
                assertThat(value.getCustomerDisplayName()).isNull();
            });
        }

        @Test
        void shouldFindExistingCustomerAndMapToDomain() {
            // given
            UUID existingCustomerId = UUID.randomUUID();
            String existingCustomerUserId = "123";
            String existingCustomerDisplayName = "Me";

            persistCustomerJpa(existingCustomerId, existingCustomerUserId, existingCustomerDisplayName);

            entityManager.clear();

            // when
            Optional<Customer> result = adapter.findById(CustomerId.of(existingCustomerId));

            // then
            assertThat(result).hasValueSatisfying(value -> {
                assertThat(value.getId()).isEqualTo(CustomerId.of(existingCustomerId));
                assertThat(value.getUserId()).isEqualTo(UserId.of(existingCustomerUserId));
                assertThat(value.getCustomerDisplayName()).isEqualTo(CustomerDisplayName.required(existingCustomerDisplayName));
            });
        }
    }

    @DisplayName("Tests for findByUserId(UserId) method")
    @Nested
    class FindByUserId {

        @Test
        void shouldThrowExceptionWhenUserIdIsNull() {
            // when & then
            assertThatThrownBy(() -> adapter.findByUserId(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId cannot be null");
        }

        @Test
        void shouldReturnEmptyWhenCustomerDoesNotExist() {
            // given
            UserId notExistingUserId = UserId.of("123");

            // when
            Optional<Customer> result = adapter.findByUserId(notExistingUserId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldFindExistingCustomerAndMapToDomain() {
            // given
            UUID existingCustomerId = UUID.randomUUID();
            String existingCustomerUserId = "123";
            String existingCustomerDisplayName = "Me";

            persistCustomerJpa(existingCustomerId, existingCustomerUserId, existingCustomerDisplayName);

            entityManager.clear();

            // when
            Optional<Customer> result = adapter.findByUserId(UserId.of(existingCustomerUserId));

            // then
            assertThat(result).hasValueSatisfying(value -> {
                assertThat(value.getId()).isEqualTo(CustomerId.of(existingCustomerId));
                assertThat(value.getUserId()).isEqualTo(UserId.of(existingCustomerUserId));
                assertThat(value.getCustomerDisplayName()).isEqualTo(CustomerDisplayName.required(existingCustomerDisplayName));
            });
        }
    }

    @DisplayName("Tests for createNewCustomer(Customer) method")
    @Nested
    class CreateNewCustomer {

        @Test
        void shouldThrowExceptionWhenCustomerIsNull() {
            // when & then
            assertThatThrownBy(() -> adapter.createNewCustomer(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("customer cannot be null");
        }

        @Test
        void shouldPersistNewCustomerAndReturnMappedToDomain() {
            // given
            Customer customer = Customer.builder()
                    .id(CustomerId.create())
                    .userId(UserId.of("123"))
                    .displayName(CustomerDisplayName.required("Me"))
                    .build();

            // when
            Customer saved = adapter.createNewCustomer(customer);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(saved).isNotNull();
            assertThat(saved.getUserId()).isEqualTo(customer.getUserId());
            assertThat(saved.getId()).isEqualTo(customer.getId());
            assertThat(saved.getCustomerDisplayName()).isEqualTo(customer.getCustomerDisplayName());

            Optional<Customer> found = adapter.findById(customer.getId());

            assertThat(found).hasValueSatisfying(value -> {
                assertThat(value.getId()).isEqualTo(customer.getId());
                assertThat(value.getUserId()).isEqualTo(customer.getUserId());
                assertThat(value.getCustomerDisplayName()).isEqualTo(customer.getCustomerDisplayName());
            });
        }
    }

    private void persistCustomerJpa(UUID publicId, String userId, String displayName) {
        CustomerJpa customerJpa = new CustomerJpa();
        customerJpa.setPublicId(publicId);
        customerJpa.setUserId(userId);
        customerJpa.setDisplayName(displayName);

        entityManager.persistAndFlush(customerJpa);
    }
}