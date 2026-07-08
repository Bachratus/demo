package com.bachratus.demo.infra.web.config;

import jakarta.annotation.Nullable;

public record ErrorResponse(
        String type,
        String code,
        @Nullable String field,
        @Nullable String message
) {
}