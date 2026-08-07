package com.bachratus.demo.infra.db.outbox;

import com.bachratus.demo.application.events.OutboxEventDraft;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OutboxEventJpaTest {

    private static final UUID EVENT_ID = UUID.fromString("7ab06985-44a9-4964-b616-dd0b264f31e4");
    private static final String TOPIC_KEY = "customer-account-created";
    private static final String TOPIC_NAME = "store.customer-account-created.v1";
    private static final String AGGREGATE_TYPE = "customer";
    private static final String AGGREGATE_ID = "c390a411-b177-4628-8058-64cd3cb7bf93";
    private static final String EVENT_TYPE = "customer.account-created.v1";
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void shouldCreatePendingOutboxEventFromDraft() {
        // given
        OutboxEventDraft draft = draft();

        // when
        OutboxEventJpa event = OutboxEventJpa.from(draft, " " + TOPIC_NAME + " ", CREATED_AT);

        // then
        assertThat(event.getId()).isEqualTo(EVENT_ID);
        assertThat(event.getVersion()).isNull();
        assertThat(event.getTopicKey()).isEqualTo(TOPIC_KEY);
        assertThat(event.getTopicName()).isEqualTo(TOPIC_NAME);
        assertThat(event.getAggregateType()).isEqualTo(AGGREGATE_TYPE);
        assertThat(event.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(event.getEventType()).isEqualTo(EVENT_TYPE);
        assertThat(event.getPayload()).isEqualTo(payload());
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextAttemptAt()).isEqualTo(CREATED_AT);
        assertThat(event.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getDeadLetteredAt()).isNull();
        assertThat(event.getLastError()).isNull();
        assertThat(event.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(event.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldDefensivelyCopyDraftPayload() {
        // given
        var payload = JsonNodeFactory.instance.objectNode().put("displayName", "Before");
        OutboxEventDraft draft = draft(payload);

        // when
        OutboxEventJpa event = OutboxEventJpa.from(draft, TOPIC_NAME, CREATED_AT);
        payload.put("displayName", "After");

        // then
        assertThat(event.getPayload().get("displayName").asText()).isEqualTo("Before");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFactoryArguments")
    void shouldRejectInvalidFactoryArguments(
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

    @Test
    void shouldMarkEventAsPublishedAndClearLastError() {
        // given
        OutboxEventJpa event = OutboxEventJpa.from(draft(), TOPIC_NAME, CREATED_AT);
        event.markMainTopicPublishFailed("temporary failure", 3, 500, CREATED_AT.plusSeconds(1));
        Instant publishedAt = CREATED_AT.plusSeconds(2);

        // when
        event.markPublished(publishedAt);

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(event.getLastError()).isNull();
        assertThat(event.getNextAttemptAt()).isEqualTo(publishedAt);
        assertThat(event.getUpdatedAt()).isEqualTo(publishedAt);
        assertThat(event.getRetryCount()).isEqualTo(1);
    }

    @Test
    void shouldMarkMainTopicFailureAsRetryableBeforeMaxAttempts() {
        // given
        OutboxEventJpa event = OutboxEventJpa.from(draft(), TOPIC_NAME, CREATED_AT);
        Instant failedAt = CREATED_AT.plusSeconds(1);

        // when
        event.markMainTopicPublishFailed("broker unavailable", 3, 750, failedAt);

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("broker unavailable");
        assertThat(event.getNextAttemptAt()).isEqualTo(failedAt.plusMillis(750));
        assertThat(event.getUpdatedAt()).isEqualTo(failedAt);
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getDeadLetteredAt()).isNull();
    }

    @Test
    void shouldMarkMainTopicFailureAsDltPendingWhenMaxAttemptsIsReached() {
        // given
        OutboxEventJpa event = OutboxEventJpa.from(draft(), TOPIC_NAME, CREATED_AT);
        Instant firstFailureAt = CREATED_AT.plusSeconds(1);
        Instant secondFailureAt = CREATED_AT.plusSeconds(2);

        // when
        event.markMainTopicPublishFailed("first failure", 2, 100, firstFailureAt);
        event.markMainTopicPublishFailed("second failure", 2, 200, secondFailureAt);

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DLT_PENDING);
        assertThat(event.getRetryCount()).isEqualTo(2);
        assertThat(event.getLastError()).isEqualTo("second failure");
        assertThat(event.getNextAttemptAt()).isEqualTo(secondFailureAt.plusMillis(200));
        assertThat(event.getUpdatedAt()).isEqualTo(secondFailureAt);
    }

    @Test
    void shouldMarkEventAsDeadLettered() {
        // given
        OutboxEventJpa event = OutboxEventJpa.from(draft(), TOPIC_NAME, CREATED_AT);
        event.markMainTopicPublishFailed("main exhausted", 1, 0, CREATED_AT.plusSeconds(1));
        Instant deadLetteredAt = CREATED_AT.plusSeconds(2);

        // when
        event.markDeadLettered("main exhausted", deadLetteredAt);

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTERED);
        assertThat(event.getDeadLetteredAt()).isEqualTo(deadLetteredAt);
        assertThat(event.getLastError()).isEqualTo("main exhausted");
        assertThat(event.getNextAttemptAt()).isEqualTo(deadLetteredAt);
        assertThat(event.getUpdatedAt()).isEqualTo(deadLetteredAt);
    }

    @Test
    void shouldKeepEventDltPendingWhenDeadLetterPublishFails() {
        // given
        OutboxEventJpa event = OutboxEventJpa.from(draft(), TOPIC_NAME, CREATED_AT);
        event.markMainTopicPublishFailed("main exhausted", 1, 0, CREATED_AT.plusSeconds(1));
        Instant failedAt = CREATED_AT.plusSeconds(2);

        // when
        event.markDeadLetterPublishFailed("dlt unavailable", 1_500, failedAt);

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DLT_PENDING);
        assertThat(event.getLastError()).isEqualTo("dlt unavailable");
        assertThat(event.getNextAttemptAt()).isEqualTo(failedAt.plusMillis(1_500));
        assertThat(event.getUpdatedAt()).isEqualTo(failedAt);
        assertThat(event.getDeadLetteredAt()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullTransitionInstants")
    void shouldRejectNullTransitionInstants(String caseName, ThrowingCallable action) {
        assertThat(caseName).isNotBlank();

        assertThatThrownBy(action)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("now cannot be null");
    }

    private static Stream<Arguments> invalidFactoryArguments() {
        return Stream.of(
                arguments(
                        "null draft",
                        (ThrowingCallable) () -> OutboxEventJpa.from(null, TOPIC_NAME, CREATED_AT),
                        NullPointerException.class,
                        "draft cannot be null"
                ),
                arguments(
                        "null topic name",
                        (ThrowingCallable) () -> OutboxEventJpa.from(draft(), null, CREATED_AT),
                        NullPointerException.class,
                        "topicName cannot be null"
                ),
                arguments(
                        "blank topic name",
                        (ThrowingCallable) () -> OutboxEventJpa.from(draft(), "   ", CREATED_AT),
                        IllegalArgumentException.class,
                        "topicName cannot be blank"
                ),
                arguments(
                        "null creation instant",
                        (ThrowingCallable) () -> OutboxEventJpa.from(draft(), TOPIC_NAME, null),
                        NullPointerException.class,
                        "now cannot be null"
                )
        );
    }

    private static Stream<Arguments> nullTransitionInstants() {
        return Stream.of(
                arguments("markPublished null now", (ThrowingCallable) () -> event().markPublished(null)),
                arguments(
                        "markMainTopicPublishFailed null now",
                        (ThrowingCallable) () -> event().markMainTopicPublishFailed("error", 3, 100, null)
                ),
                arguments("markDeadLettered null now", (ThrowingCallable) () -> event().markDeadLettered("error", null)),
                arguments(
                        "markDeadLetterPublishFailed null now",
                        (ThrowingCallable) () -> event().markDeadLetterPublishFailed("error", 100, null)
                )
        );
    }

    private static OutboxEventJpa event() {
        return OutboxEventJpa.from(draft(), TOPIC_NAME, CREATED_AT);
    }

    private static OutboxEventDraft draft() {
        return draft(payload());
    }

    private static OutboxEventDraft draft(JsonNode payload) {
        return new OutboxEventDraft(
                EVENT_ID,
                TOPIC_KEY,
                AGGREGATE_TYPE,
                AGGREGATE_ID,
                EVENT_TYPE,
                payload,
                OCCURRED_AT
        );
    }

    private static JsonNode payload() {
        return JsonNodeFactory.instance.objectNode()
                .put("schemaVersion", 1)
                .put("customerId", AGGREGATE_ID)
                .put("userId", "user-123");
    }
}
