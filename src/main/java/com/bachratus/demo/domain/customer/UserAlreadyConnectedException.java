package com.bachratus.demo.domain.customer;

import com.bachratus.demo.domain.shared.exception.DomainException;
import com.bachratus.demo.domain.shared.value.id.CustomerId;
import com.bachratus.demo.domain.shared.value.id.UserId;
import lombok.Getter;

@Getter
public class UserAlreadyConnectedException extends DomainException {

    public UserAlreadyConnectedException(
            UserId existingUserId,
            UserId requestedUserId,
            CustomerId customerId
    ) {
        super(
                "Customer (%s) is already connected to user (%s), cannot connect requested user (%s)"
                        .formatted(
                                customerId.value(),
                                existingUserId.value(),
                                requestedUserId.value()
                        )
        );
    }
}