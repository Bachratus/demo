package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.utils.ValidationUtils;
import lombok.Getter;

@Getter
public class Customer {
    private final CustomerId id;
    private final UserId userId;
    private final CustomerDisplayName customerDisplayName;

    private Customer(CustomerId id, UserId userId, CustomerDisplayName customerDisplayName) {
        this.id = ValidationUtils.requireNotNull(id, "id");
        this.userId = ValidationUtils.requireNotNull(userId, "userId");
        this.customerDisplayName = customerDisplayName;
    }

    public static Customer restore(CustomerId id, UserId userId, CustomerDisplayName customerDisplayName) {
        return new Customer(id, userId, customerDisplayName);
    }

    public static CustomerBuilder builder() {
        return new CustomerBuilder();
    }

    public static class CustomerBuilder {
        private CustomerId id;
        private UserId userId;
        private CustomerDisplayName customerDisplayName;

        public Customer build() {
            return new Customer(id, userId, customerDisplayName);
        }

        public CustomerBuilder id(CustomerId id) {
            this.id = id;
            return this;
        }

        public CustomerBuilder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public CustomerBuilder displayName(CustomerDisplayName customerDisplayName) {
            this.customerDisplayName = customerDisplayName;
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer customer)) return false;
        return id.equals(customer.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
