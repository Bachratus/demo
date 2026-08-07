package com.bachratus.demo.infra.db.processed;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ProcessedEventJpaTest {

    @Test
    void shouldCreateProcessedEventMarkerWithNormalizedIdentity() {
        // when
        ProcessedEventJpa event = ProcessedEventJpa.of(
                "  customer.account-created.v1  ",
                "  7ab06985-44a9-4964-b616-dd0b264f31e4  "
        );

        // then
        assertThat(event.getEventType()).isEqualTo("customer.account-created.v1");
        assertThat(event.getEventId()).isEqualTo("7ab06985-44a9-4964-b616-dd0b264f31e4");
        assertThat(event.getId()).isNull();
        assertThat(event.getVersion()).isNull();
        assertThat(event.getCreatedAt()).isNull();
        assertThat(event.getCreatedBy()).isNull();
        assertThat(event.getUpdatedAt()).isNull();
        assertThat(event.getUpdatedBy()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidIdentityArguments")
    void shouldRejectInvalidIdentity(
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

    private static Stream<Arguments> invalidIdentityArguments() {
        return Stream.of(
                arguments(
                        "null event type",
                        (ThrowingCallable) () -> ProcessedEventJpa.of(null, "event-1"),
                        NullPointerException.class,
                        "eventType cannot be null"
                ),
                arguments(
                        "blank event type",
                        (ThrowingCallable) () -> ProcessedEventJpa.of("   ", "event-1"),
                        IllegalArgumentException.class,
                        "eventType cannot be blank"
                ),
                arguments(
                        "null event id",
                        (ThrowingCallable) () -> ProcessedEventJpa.of("customer.account-created.v1", null),
                        NullPointerException.class,
                        "eventId cannot be null"
                ),
                arguments(
                        "blank event id",
                        (ThrowingCallable) () -> ProcessedEventJpa.of("customer.account-created.v1", "   "),
                        IllegalArgumentException.class,
                        "eventId cannot be blank"
                )
        );
    }
}
