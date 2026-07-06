package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.utils.ValidationUtils;
import com.bachratus.demo.domain.customer.id.CustomerId;
import com.bachratus.demo.domain.shared.value.TextProperty;
import com.bachratus.demo.domain.customer.id.UserId;
import lombok.Getter;

@Getter
public class Customer {
    private final CustomerId id;
    private final UserId userId;
    private TextProperty displayName;

    private Customer(CustomerId id, UserId userId, TextProperty displayName) {
        this.id = ValidationUtils.requireNotNull(id, "id");
        this.userId = ValidationUtils.requireNotNull(userId, "userId");
        this.displayName = displayName;
    }

    public void changeDisplayName(TextProperty displayName) {
        this.displayName = ValidationUtils.requireNotNull(displayName, "displayName");
    }

    public void clearDisplayName() {
        this.displayName = null;
    }

    public static CustomerBuilder builder() {
        return new CustomerBuilder();
    }

    public static class CustomerBuilder {
        private CustomerId id;
        private UserId userId;
        private TextProperty displayName;

        public Customer build() {
            return new Customer(id, userId, displayName);
        }

        public CustomerBuilder id(CustomerId id) {
            this.id = id;
            return this;
        }

        public CustomerBuilder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public CustomerBuilder displayName(TextProperty displayName) {
            this.displayName = displayName;
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
