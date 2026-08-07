package com.bachratus.demo.infra.kafka.config.producer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class OutboxKafkaPublisherSchedulerTest {

    @Test
    void shouldTriggerPublisherBatch() {
        // given
        OutboxKafkaPublisher publisher = mock(OutboxKafkaPublisher.class);
        OutboxKafkaPublisherScheduler scheduler = new OutboxKafkaPublisherScheduler(publisher);

        // when
        scheduler.publishScheduledBatch();

        // then
        verify(publisher).publishBatch();
    }

    @Test
    void shouldLogProcessedEventsWhenPublisherProcessesAnyRows(CapturedOutput output) {
        // given
        OutboxKafkaPublisher publisher = mock(OutboxKafkaPublisher.class);
        when(publisher.publishBatch()).thenReturn(2);
        OutboxKafkaPublisherScheduler scheduler = new OutboxKafkaPublisherScheduler(publisher);

        // when
        scheduler.publishScheduledBatch();

        // then
        assertThat(output).contains("Processed 2 outbox events");
    }

    @Test
    void shouldNotLogProcessedEventsWhenPublisherProcessesNoRows(CapturedOutput output) {
        // given
        OutboxKafkaPublisher publisher = mock(OutboxKafkaPublisher.class);
        when(publisher.publishBatch()).thenReturn(0);
        OutboxKafkaPublisherScheduler scheduler = new OutboxKafkaPublisherScheduler(publisher);

        // when
        scheduler.publishScheduledBatch();

        // then
        assertThat(output).doesNotContain("Processed 0 outbox events");
    }

    @Test
    void shouldCatchUnexpectedPublisherFailureAndLogIt(CapturedOutput output) {
        // given
        OutboxKafkaPublisher publisher = mock(OutboxKafkaPublisher.class);
        when(publisher.publishBatch()).thenThrow(new RuntimeException("database unavailable"));
        OutboxKafkaPublisherScheduler scheduler = new OutboxKafkaPublisherScheduler(publisher);

        // when & then
        assertThatNoException().isThrownBy(scheduler::publishScheduledBatch);
        assertThat(output)
                .contains("Unexpected outbox publisher failure")
                .contains("database unavailable");
    }
}
