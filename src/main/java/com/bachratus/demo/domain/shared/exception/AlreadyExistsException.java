package com.bachratus.demo.domain.shared.exception;

import lombok.Getter;

@Getter
public class AlreadyExistsException extends DomainException {

    String entityName;
    String identifierName;
    String identifier;

    public AlreadyExistsException(Class<?> entity, String identifierName, String identifier) {
        super("Entity %s with %s of value [%s] already exists".formatted(entity.getSimpleName(), identifierName, identifier));
        this.entityName = entity.getSimpleName();
        this.identifierName = identifierName;
        this.identifier = identifier;
    }
}