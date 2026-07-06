package com.bachratus.demo.domain.shared.exception.validation;

import lombok.Getter;

@Getter
public class MissingRequiredFieldException extends ValidationException {

    public MissingRequiredFieldException(String parameterName) {
        super(parameterName, "Parameter is required and cannot be null");
    }
}