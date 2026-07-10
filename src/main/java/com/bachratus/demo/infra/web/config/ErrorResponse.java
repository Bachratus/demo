package com.bachratus.demo.infra.web.config;

public record ErrorResponse(
        ErrorType type,
        String code,
        String message
) {
}