package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.utils.ValidationUtils;
import com.bachratus.demo.domain.shared.value.id.CustomerId;
import com.bachratus.demo.domain.shared.value.TextProperty;
import com.bachratus.demo.domain.shared.value.Email;
import com.bachratus.demo.domain.shared.value.id.UserId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Customer {
    private final CustomerId id;
    private UserId userId;
    private final Email email;
    private TextProperty displayName;

    public static Customer restore(
            CustomerId id,
            UserId userId,
            Email email,
            TextProperty displayName
    ) {
        return new Customer(id, userId, email, displayName);
    }

    public void connectToAuthorizedUser(UserId userId) {
        UserId requiredUserId = ValidationUtils.requireNotNull(userId, "userId");

        if (this.userId != null) {
            throw new UserAlreadyConnectedException(this.userId, requiredUserId, this.id);
        }

        this.userId = requiredUserId;
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
        private Email email;
        private TextProperty displayName;

        public Customer build() {
            return new Customer(
                    ValidationUtils.requireNotNull(id, "id"),
                    userId,
                    ValidationUtils.requireNotNull(email, "email"),
                    displayName
            );
        }

        public CustomerBuilder id(CustomerId id) {
            this.id = id;
            return this;
        }

        public CustomerBuilder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public CustomerBuilder email(Email email) {
            this.email = email;
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
