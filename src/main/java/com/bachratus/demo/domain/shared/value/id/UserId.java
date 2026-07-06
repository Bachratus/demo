package com.bachratus.demo.domain.shared.value.id;

import com.bachratus.demo.domain.shared.utils.ValidationUtils;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        ValidationUtils.requireNotNull(value, "userId");
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }
}