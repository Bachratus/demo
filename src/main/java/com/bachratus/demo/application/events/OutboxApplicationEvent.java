package com.bachratus.demo.application.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface OutboxApplicationEvent {

    @JsonIgnore
    String eventKey();

    @JsonIgnore
    String aggregateId();

    int schemaVersion();
}
