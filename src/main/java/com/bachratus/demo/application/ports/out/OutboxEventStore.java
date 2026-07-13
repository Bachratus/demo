package com.bachratus.demo.application.ports.out;

import com.bachratus.demo.application.events.OutboxEventDraft;

public interface OutboxEventStore {

    void append(OutboxEventDraft event);
}
