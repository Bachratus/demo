# Kafka consumer configuration

This package contains the consumer side of the Kafka integration: listener
registration, listener retry handling, processed-event idempotency, and
dead-letter topic routing.

## 1. Kafka listener infrastructure

Kafka listeners are enabled by `KafkaMessagingConfiguration` through
`@EnableKafka`.

The Kafka consumer infrastructure is active only when:

```yaml
app.kafka.enabled: true
```

When `app.kafka.enabled=false`, Kafka listeners, listener error handling, and
dead-letter topic routing are not registered.

## 2. Spring Kafka consumer settings

The actual Kafka consumer client is configured through `spring.kafka.consumer.*`.

The current configuration uses:

- `group-id=demo-app`
- `auto-offset-reset=earliest`
- `StringDeserializer` for record keys
- `ErrorHandlingDeserializer` for record values
- `JsonDeserializer` as the value delegate
- `JsonNode` as the default payload type

`auto-offset-reset=earliest` only applies when the consumer group has no stored
offset for a partition yet. Once offsets exist, Kafka resumes from the committed
offsets.

`ErrorHandlingDeserializer` allows deserialization failures to flow through
Spring Kafka error handling instead of breaking the consumer loop directly.

## 3. CustomerAccountCreatedKafkaListener

`listener.CustomerAccountCreatedKafkaListener` listens to the configured
customer account created topic:

```yaml
app.kafka.topics.customer-account-created.name
```

The listener uses:

```java
@KafkaListener(
    id = "customer-account-created-console-logger",
    groupId = "${spring.kafka.consumer.group-id}",
    topics = "${app.kafka.topics.customer-account-created.name}",
    concurrency = "${app.kafka.topics.customer-account-created.concurrency}"
)
```

`groupId` decides which consumer group owns the offsets.

`topics` points to the physical Kafka topic name from application topic
configuration.

`concurrency` controls how many listener containers Spring starts for this
listener. The useful concurrency is limited by the number of partitions in the
topic. If a topic has fewer partitions than the configured concurrency, some
consumer threads will remain idle.

The listener currently accepts the full `ConsumerRecord<String, JsonNode>` and
delegates it to `handler.CustomerAccountCreatedKafkaEventHandler`. The listener
is kept as a transport adapter; processing concerns live in the handler layer.

## 4. Processed-event idempotency

Kafka event handlers can be protected with:

```java
@IdempotentKafkaEventHandler
```

The `KafkaEventIdempotencyAspect` intercepts annotated handler methods before
the handler body runs. It expects the method to receive a `ConsumerRecord`
argument and reads these Kafka headers:

- `event-id`
- `event-type`

Before processing starts, the aspect inserts a marker into the
`processed_events` table. The table has a unique constraint on:

```text
event_type, event_id
```

If the insert succeeds, the handler is invoked.

If the insert fails because the same event was already processed, the handler is
skipped and the listener completes without throwing. This lets the consumer move
past duplicate records.

The marker insert and handler invocation run in the same transaction. If the
handler fails, the transaction rolls back and the marker is not kept. The event
can then be retried normally.

## 5. Listener error handling

`KafkaListenerErrorHandlingConfiguration` creates a `DefaultErrorHandler`.

The handler uses:

```java
new FixedBackOff(retryIntervalMs, retries)
```

where:

```java
retries = max(0, app.kafka.listener.max-attempts - 1)
```

With:

```yaml
app.kafka.listener.max-attempts: 3
app.kafka.listener.retry-interval-ms: 1000
```

Spring Kafka performs one initial attempt and two retries, waiting 1000 ms
between retry attempts. After the attempts are exhausted, the record is handed to
the configured `DeadLetterPublishingRecoverer`.

## 6. Dead-letter topic routing

`KafkaDeadLetterTopicResolver` decides where failed records should be published
after listener retries are exhausted.

It resolves the dead-letter topic from `AppKafkaProperties` by using the original
record topic:

```java
kafkaProperties.deadLetterTopicNameForTopic(record.topic())
```

The failed record is routed to the same partition number in the dead-letter
topic:

```java
new TopicPartition(dltTopicName, record.partition())
```

For example:

```text
store.customer-account-created.v1
```

is routed to:

```text
store.customer-account-created.v1.dlt
```

on the same partition.

## 7. Scenario: successful record processing

1. Kafka delivers a record to the listener.
2. The key is deserialized as `String`.
3. The value is deserialized as `JsonNode`.
4. `CustomerAccountCreatedKafkaListener` receives the `ConsumerRecord`.
5. The listener delegates the record to `CustomerAccountCreatedKafkaEventHandler`.
6. The idempotency aspect stores `(event-type, event-id)` in `processed_events`.
7. The handler completes without throwing an exception.
8. Spring Kafka treats the record as successfully processed.
9. The consumer can continue with subsequent records.

## 8. Scenario: duplicate event

1. Kafka delivers a record to the listener.
2. The listener delegates the record to an annotated handler.
3. The idempotency aspect tries to store `(event-type, event-id)`.
4. The `processed_events_event_unique` constraint rejects the insert.
5. The aspect treats the record as already processed.
6. The handler body is skipped.
7. The listener completes successfully, so the duplicate does not block the
   partition.

## 9. Scenario: listener or handler throws an exception

1. Kafka delivers a record to the listener.
2. The listener delegates the record to the handler.
3. The idempotency aspect stores the processed-event marker.
4. The listener or handler throws an exception during processing.
5. The transaction rolls back, including the processed-event marker.
6. `DefaultErrorHandler` handles the failure.
7. The same record is retried according to `app.kafka.listener.*`.
8. If one retry succeeds, processing continues normally.
9. If all attempts fail, `DeadLetterPublishingRecoverer` publishes the record to
   the configured dead-letter topic.

## 10. Scenario: deserialization fails

1. Kafka delivers a record to the consumer.
2. The value cannot be deserialized by the delegate `JsonDeserializer`.
3. `ErrorHandlingDeserializer` captures the deserialization failure.
4. Spring Kafka routes the failure through listener error handling.
5. After configured retries are exhausted, the record can be sent to the
   configured dead-letter topic.

## 11. Scenario: missing idempotency headers

1. Kafka delivers a record to the listener.
2. The listener delegates the record to an annotated handler.
3. The idempotency aspect cannot find `event-id` or `event-type`.
4. The aspect throws an exception before the handler body runs.
5. The error is handled by `DefaultErrorHandler`.
6. After configured retries are exhausted, the record can be sent to the
   configured dead-letter topic.

## 12. Scenario: dead-letter publication succeeds

1. The listener has failed all configured processing attempts.
2. `DeadLetterPublishingRecoverer` asks `KafkaDeadLetterTopicResolver` for the
   target topic and partition.
3. The record is published to the dead-letter topic.
4. Spring Kafka treats the original record as recovered.
5. The consumer can continue with later records from the partition.

## 13. Scenario: dead-letter publication fails

1. The listener has failed all configured processing attempts.
2. Spring Kafka tries to publish the record to the dead-letter topic.
3. The DLT publish fails, for example because Kafka is unavailable.
4. The record is not safely recovered.
5. Depending on container state and broker availability, the partition can keep
   retrying or become blocked on the problematic record.

This scenario should be monitored carefully. A failing DLT path means the system
cannot safely isolate poison messages.

## 14. Current delivery guarantees

The current consumer setup should be treated as at-least-once processing.

The listener can receive the same event more than once after restarts,
rebalances, retry edge cases, or duplicate publication from the outbox producer.

The `processed_events` table provides the foundation for effectively-once
business processing by skipping duplicate `(event-type, event-id)` pairs. This is
not the same as Kafka-level exactly-once delivery, but it protects handler side
effects when they are performed in the same database transaction as the
processed-event marker.
