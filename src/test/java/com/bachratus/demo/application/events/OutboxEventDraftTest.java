package com.bachratus.demo.application.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OutboxEventDraftTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void shouldCreateDraftWithGeneratedIdAndNormalizedTextFields() {
        // given
        JsonNode payload = payload();

        // when
        OutboxEventDraft draft = OutboxEventDraft.create(
                " customer-account-created ",
                " customer ",
                " aggregate-1 ",
                " customer.account-created.v1 ",
                payload,
                OCCURRED_AT
        );

        // then
        assertThat(draft.id()).isNotNull();
        assertThat(draft.topicKey()).isEqualTo("customer-account-created");
        assertThat(draft.aggregateType()).isEqualTo("customer");
        assertThat(draft.aggregateId()).isEqualTo("aggregate-1");
        assertThat(draft.eventType()).isEqualTo("customer.account-created.v1");
        assertThat(draft.payload()).isEqualTo(payload);
        assertThat(draft.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void shouldDefensivelyCopyInputPayload() {
        // given
        var payload = JsonNodeFactory.instance.objectNode().put("displayName", "Before");

        // when
        OutboxEventDraft draft = OutboxEventDraft.create(
                "customer-account-created",
                "customer",
                "aggregate-1",
                "customer.account-created.v1",
                payload,
                OCCURRED_AT
        );
        payload.put("displayName", "After");

        // then
        assertThat(draft.payload().get("displayName").asText()).isEqualTo("Before");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDraftArguments")
    void shouldRejectInvalidDraftArguments(
            String caseName,
            ThrowingCallable action,
            Class<? extends Throwable> expectedType,
            String expectedMessage
    ) {
        assertThat(caseName).isNotBlank();

        assertThatThrownBy(action)
                .isInstanceOf(expectedType)
                .hasMessageContaining(expectedMessage);
    }

    private static Stream<Arguments> invalidDraftArguments() {
        return Stream.of(
                arguments(
                        "null id",
                        (ThrowingCallable) () -> new OutboxEventDraft(
                                null,
                                "topic-key",
                                "aggregate-type",
                                "aggregate-id",
                                "event-type",
                                payload(),
                                OCCURRED_AT
                        ),
                        NullPointerException.class,
                        "id cannot be null"
                ),
                arguments(
                        "null topic key",
                        (ThrowingCallable) () -> create(null, "aggregate-type", "aggregate-id", "event-type", payload(), OCCURRED_AT),
                        NullPointerException.class,
                        "topicKey cannot be null"
                ),
                arguments(
                        "blank topic key",
                        (ThrowingCallable) () -> create("   ", "aggregate-type", "aggregate-id", "event-type", payload(), OCCURRED_AT),
                        IllegalArgumentException.class,
                        "topicKey cannot be blank"
                ),
                arguments(
                        "null aggregate type",
                        (ThrowingCallable) () -> create("topic-key", null, "aggregate-id", "event-type", payload(), OCCURRED_AT),
                        NullPointerException.class,
                        "aggregateType cannot be null"
                ),
                arguments(
                        "blank aggregate type",
                        (ThrowingCallable) () -> create("topic-key", "   ", "aggregate-id", "event-type", payload(), OCCURRED_AT),
                        IllegalArgumentException.class,
                        "aggregateType cannot be blank"
                ),
                arguments(
                        "null aggregate id",
                        (ThrowingCallable) () -> create("topic-key", "aggregate-type", null, "event-type", payload(), OCCURRED_AT),
                        NullPointerException.class,
                        "aggregateId cannot be null"
                ),
                arguments(
                        "blank aggregate id",
                        (ThrowingCallable) () -> create("topic-key", "aggregate-type", "   ", "event-type", payload(), OCCURRED_AT),
                        IllegalArgumentException.class,
                        "aggregateId cannot be blank"
                ),
                arguments(
                        "null event type",
                        (ThrowingCallable) () -> create("topic-key", "aggregate-type", "aggregate-id", null, payload(), OCCURRED_AT),
                        NullPointerException.class,
                        "eventType cannot be null"
                ),
                arguments(
                        "blank event type",
                        (ThrowingCallable) () -> create("topic-key", "aggregate-type", "aggregate-id", "   ", payload(), OCCURRED_AT),
                        IllegalArgumentException.class,
                        "eventType cannot be blank"
                ),
                arguments(
                        "null payload",
                        (ThrowingCallable) () -> create("topic-key", "aggregate-type", "aggregate-id", "event-type", null, OCCURRED_AT),
                        NullPointerException.class,
                        "payload cannot be null"
                ),
                arguments(
                        "non-object payload",
                        (ThrowingCallable) () -> create(
                                "topic-key",
                                "aggregate-type",
                                "aggregate-id",
                                "event-type",
                                TextNode.valueOf("not-an-object"),
                                OCCURRED_AT
                        ),
                        IllegalArgumentException.class,
                        "payload must be a parsed JSON object"
                ),
                arguments(
                        "null occurred at",
                        (ThrowingCallable) () -> create("topic-key", "aggregate-type", "aggregate-id", "event-type", payload(), null),
                        NullPointerException.class,
                        "occurredAt cannot be null"
                )
        );
    }

    private static void create(
            String topicKey,
            String aggregateType,
            String aggregateId,
            String eventType,
            JsonNode payload,
            Instant occurredAt
    ) {
        OutboxEventDraft.create(
                topicKey,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                occurredAt
        );
    }

    private static JsonNode payload() {
        return JsonNodeFactory.instance.objectNode().put("schemaVersion", 1);
    }
}
