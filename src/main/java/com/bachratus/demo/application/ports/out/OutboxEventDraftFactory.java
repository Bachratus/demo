package com.bachratus.demo.application.ports.out;

import com.bachratus.demo.application.events.OutboxApplicationEvent;
import com.bachratus.demo.application.events.OutboxEventDraft;

public interface OutboxEventDraftFactory {

    OutboxEventDraft create(OutboxApplicationEvent event);
}
