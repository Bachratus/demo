# Kafka Consumer Configuration

This package documents the consumer side of the Kafka integration.

The consumer side is responsible for receiving Kafka records, delegating business
handling to infrastructure handlers, protecting handler side effects from
duplicates, retrying transient failures, and publishing poison records to
dead-letter topics with enough diagnostics for later investigation.

The important boundary is:

```text
Kafka record -> listener adapter -> handler -> retry/error handling -> DLT
```

This package intentionally does not implement a local DLT browser, local DLT API,
or local DLT reprocessor. Those responsibilities belong to a future
`dlt-resolver` or DLT monitor service that can read DLT topics across
applications.

## 1. Activation

Kafka listener infrastructure is enabled by `KafkaMessagingConfiguration`
through `@EnableKafka`.

The consumer side is active only when:

```yaml
app.kafka.enabled: true
```

When `app.kafka.enabled=false`, these beans are not registered:

- Kafka listener infrastructure from this module
- listener error handling configuration
- dead-letter topic resolver
- diagnostic DLT headers factory
- consumer handlers and listeners
- idempotency aspect

This is the application-level kill switch for Kafka integration.

## 2. Configuration Layers

There are two configuration layers.

`spring.kafka.consumer.*` configures the real Kafka consumer client. Spring Boot
uses it to create the consumer factory and listener containers.

`app.kafka.*` configures application behavior. It is bound to
`AppKafkaProperties` and used for listener retry policy, topic metadata, DLT
resolution, and fail-fast validation.

The current consumer client configuration uses:

- `group-id=demo-app`
- `auto-offset-reset=earliest`
- `ErrorHandlingDeserializer` for record keys
- `ErrorHandlingDeserializer` for record values
- `StringDeserializer` as the key delegate
- `JsonDeserializer` as the value delegate
- `JsonNode` as the default payload type

`auto-offset-reset=earliest` applies only when the consumer group has no stored
offset for a partition. Once offsets exist, Kafka resumes from committed offsets.

Both key and value deserializers are wrapped in `ErrorHandlingDeserializer`.
This is important because deserialization failures happen before the listener
method can run. Without the wrapper, a bad key or value could break the consumer
loop before Spring Kafka's normal listener error handling can recover the record.

## 3. Listener Shape

`CustomerAccountCreatedKafkaListener` is a transport adapter. It receives the
Kafka `ConsumerRecord` and delegates to
`CustomerAccountCreatedKafkaEventHandler`.

The listener is configured as:

```java
@KafkaListener(
    id = "customer-account-created-console-logger",
    info = "customer-account-created-console-logger",
    groupId = "${spring.kafka.consumer.group-id}",
    topics = "${app.kafka.topics.customer-account-created.name}",
    concurrency = "${app.kafka.topics.customer-account-created.concurrency}"
)
```

`id` identifies the Spring Kafka listener container.

`info` is diagnostic listener metadata. Spring Kafka adds it to records as
`KafkaHeaders.LISTENER_INFO`. `KafkaDeadLetterHeadersFactory` reads that header
and copies it into `dlt-listener-id`, so a DLT monitor can identify the exact
listener path that failed.

`groupId` decides which consumer group owns offsets for this listener.

`topics` points to the physical Kafka topic name from `app.kafka.topics.*`.

`concurrency` controls how many listener containers Spring starts for this
listener. Useful concurrency is limited by the number of partitions in the
topic. If concurrency is greater than partition count, some consumer threads
will stay idle.

## 4. Why Listener And Handler Are Separate

The listener is deliberately thin.

Its job is transport adaptation:

```text
ConsumerRecord<String, JsonNode> -> handler.handle(record)
```

The handler is where processing behavior lives. This separation matters because
cross-cutting concerns such as idempotency can wrap handlers without coupling
them to listener container mechanics.

The listener should not contain business logic, retry logic, DLT routing, or
idempotency logic. Those concerns are handled by dedicated components.

## 5. Processed-Event Idempotency

Handlers can be protected with:

```java
@IdempotentKafkaEventHandler
```

`KafkaEventIdempotencyAspect` intercepts annotated handler methods before the
handler body runs.

The aspect expects a `ConsumerRecord` argument and reads:

- `event-id`
- `event-type`

It inserts a marker into `processed_events`, which has a unique constraint on:

```text
event_type, event_id
```

If the insert succeeds, the handler is invoked.

If the insert fails because the same `(event-type, event-id)` already exists,
the handler body is skipped and the listener completes successfully. This lets
the consumer move past duplicate Kafka records without repeating handler side
effects.

The marker insert and handler execution run in the same transaction. If the
handler throws, the transaction rolls back and the marker is not kept. The same
record can then be retried normally.

This gives effectively-once business processing for side effects that
participate in the same database transaction. It is not Kafka-level exactly-once
delivery.

## 6. Listener Error Handling

`KafkaListenerErrorHandlingConfiguration` creates a `DefaultErrorHandler` and
attaches it to the `ConcurrentKafkaListenerContainerFactory`.

Spring Boot first configures the listener factory:

```java
configurer.configure(factory, consumerFactory);
```

Then this module attaches the common error handler:

```java
factory.setCommonErrorHandler(kafkaDefaultErrorHandler);
```

All listeners using this factory now share the same retry and DLT behavior.

The retry policy is:

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

the flow is:

```text
initial attempt -> retry after 1000 ms -> retry after 1000 ms -> DLT
```

Some exception types are fatal by default in Spring Kafka. A
`DeserializationException` is one of them. Retrying the same raw bytes usually
cannot make an invalid key or payload valid, so normal listener retries are
skipped and recovery is attempted immediately.

## 7. Dead-Letter Publishing

After retries are exhausted, or immediately for fatal exceptions,
`DefaultErrorHandler` calls `LoggingDeadLetterPublishingRecoverer`.

The recoverer does four things:

1. Logs that a record is about to be published to DLT.
2. Asks `KafkaDeadLetterTopicResolver` for the DLT destination.
3. Adds diagnostic headers from `KafkaDeadLetterHeadersFactory`.
4. Publishes the record to Kafka using `KafkaTemplate`.

It is configured with:

```java
recoverer.addHeadersFunction(deadLetterHeadersFactory::create);
recoverer.setFailIfSendResultIsError(true);
recoverer.setWaitForSendResultTimeout(Duration.ofMillis(sendResultTimeoutMs));
recoverer.setLogRecoveryRecord(true);
recoverer.setThrowIfNoDestinationReturned(true);
```

`setFailIfSendResultIsError(true)` means DLT publish errors are not swallowed.
If Kafka rejects the DLT record, the original record is not considered recovered.

`setWaitForSendResultTimeout(...)` means the recoverer waits for broker
confirmation. The timeout uses `app.kafka.producer.send-result-timeout-ms`, so
consumer DLT publication and producer outbox publication follow the same wait
budget.

`setThrowIfNoDestinationReturned(true)` matches the module rule that DLT routing
must be explicit. No destination means configuration is wrong.

`setLogRecoveryRecord(true)` keeps Spring Kafka's own recovery logging enabled.
`LoggingDeadLetterPublishingRecoverer` adds application-specific structured
logs around the same recovery.

## 8. Dead-Letter Topic Resolution

`KafkaDeadLetterTopicResolver` resolves the DLT topic by the original physical
topic name:

```java
kafkaProperties.deadLetterTopicNameForTopic(record.topic())
```

The resolver returns the same partition number:

```java
new TopicPartition(dltTopicName, record.partition())
```

Example:

```text
store.customer-account-created.v1 partition 2
 -> store.customer-account-created.v1.dlt partition 2
```

Same-partition routing keeps the original partition coordinate easy to reason
about. Operationally, the DLT topic should have at least the same partition count
as the main topic.

There is no fallback to `<topic>.dlt`.

Missing DLT configuration is a deployment/configuration error. Failing loudly is
safer than silently publishing poison messages to a guessed topic nobody watches.

## 9. DLT Diagnostic Headers

Spring Kafka already adds framework-level DLT headers with original record
coordinates and exception details.

This module also adds stable application-level headers for a future DLT monitor
or resolver service.

`dlt-source`

Set to `consumer`. It distinguishes consumer DLT records from producer-side DLT
records created by the outbox publisher.

`dlt-application`

The Spring application name, for example `demo`.

`dlt-consumer-group`

The configured consumer group, for example `demo-app`.

`dlt-listener-id`

The listener metadata from `@KafkaListener(info = "...")`. If Spring Kafka does
not provide it, the value is `unknown`.

`dlt-original-topic`

The physical topic from which the failed record was consumed.

`dlt-original-partition`

The original partition number.

`dlt-original-offset`

The original Kafka offset.

`dlt-original-timestamp`

The timestamp from the original Kafka record.

`dlt-failed-at`

UTC timestamp when the DLT headers were created.

`dlt-error-class`

The exception class seen by the recoverer.

`dlt-error-message`

The exception message seen by the recoverer.

`dlt-root-cause-class`

The deepest cause class.

`dlt-root-cause-message`

The deepest cause message.

`dlt-event-id`

Copy of the original `event-id` header when present.

`dlt-event-type`

Copy of the original `event-type` header when present.

`dlt-aggregate-type`

Copy of the original `aggregate-type` header when present.

`dlt-aggregate-id`

Copy of the original `aggregate-id` header when present.

The original event headers are still preserved on the DLT record. The `dlt-*`
copies make the most important fields easy to index, query, and display without
depending on Spring Kafka's internal header naming.

## 10. DLT Logging

`LoggingDeadLetterPublishingRecoverer` logs three moments.

Before DLT publish:

```text
Publishing Kafka record to DLT
```

After successful DLT publish:

```text
Published Kafka record to DLT
```

After failed DLT publish:

```text
Failed to publish Kafka record to DLT
```

The logs include:

- source topic
- source partition
- source offset
- DLT topic
- DLT partition
- consumer group
- event id
- event type
- exception class or failure reason

These logs are meant for immediate operations. The DLT headers are meant for
later automated processing by a DLT resolver.

## 11. What This Module Deliberately Does Not Do

This module does not listen to its own DLT topics.

It does not store consumer DLT records in a local table.

It does not expose an API listing DLT messages.

It does not update producer outbox rows when a consumer sends a record to DLT.

Those decisions avoid coupling normal application services to DLT operations.
The intended direction is a separate DLT monitor/resolver service that can:

- read DLT topics across applications
- expose API/UI for failed records
- support retry, reprocess, ignore, or manual repair workflows
- republish fixed records to main topics when appropriate

Normal applications only need to publish reliable, well-described DLT records.

## 12. Scenario: Successful Processing

1. Kafka delivers a record to the listener container.
2. `ErrorHandlingDeserializer` delegates key deserialization to
   `StringDeserializer`.
3. `ErrorHandlingDeserializer` delegates value deserialization to
   `JsonDeserializer`.
4. Spring Kafka adds listener metadata such as `KafkaHeaders.LISTENER_INFO`.
5. `CustomerAccountCreatedKafkaListener` receives `ConsumerRecord<String,
   JsonNode>`.
6. The listener delegates to `CustomerAccountCreatedKafkaEventHandler`.
7. `KafkaEventIdempotencyAspect` stores `(event-type, event-id)` in
   `processed_events`.
8. The handler completes.
9. Spring Kafka treats the record as processed and can commit/move the offset.

## 13. Scenario: Duplicate Event

1. Kafka delivers a record already processed before.
2. The listener delegates to an annotated handler.
3. `KafkaEventIdempotencyAspect` tries to insert `(event-type, event-id)`.
4. The unique constraint rejects the insert.
5. The aspect treats the record as duplicate.
6. The handler body is skipped.
7. The listener completes successfully.
8. The duplicate does not block the partition.

## 14. Scenario: Handler Failure

1. Kafka delivers a valid record.
2. The listener delegates to the handler.
3. The idempotency marker is inserted in the active transaction.
4. The handler throws.
5. The transaction rolls back, including the idempotency marker.
6. `DefaultErrorHandler` retries according to `app.kafka.listener.*`.
7. If a retry succeeds, processing completes normally.
8. If all attempts fail, the recoverer publishes the record to DLT.

## 15. Scenario: Key Deserialization Failure

1. Kafka delivers a record.
2. The key cannot be deserialized by `StringDeserializer`.
3. `ErrorHandlingDeserializer` captures the key failure.
4. The listener method is not invoked with a normal `ConsumerRecord`.
5. `DefaultErrorHandler` receives a `DeserializationException`.
6. Normal listener retries are skipped because the exception is fatal by default.
7. The recoverer attempts DLT publication.

## 16. Scenario: Value Deserialization Failure

1. Kafka delivers a record.
2. The value cannot be deserialized by `JsonDeserializer`.
3. `ErrorHandlingDeserializer` captures the value failure.
4. The listener method is not invoked with a normal `JsonNode` payload.
5. `DefaultErrorHandler` receives a `DeserializationException`.
6. Normal listener retries are skipped because the exception is fatal by default.
7. The recoverer attempts DLT publication.

## 17. Scenario: Missing Idempotency Headers

1. Kafka delivers a record.
2. The listener delegates to an annotated handler.
3. `KafkaEventIdempotencyAspect` cannot find `event-id` or `event-type`.
4. The aspect throws before the handler body runs.
5. `DefaultErrorHandler` applies the normal retry policy.
6. If the record still fails, the recoverer publishes it to DLT.

This is treated as a recoverable listener failure today. It can be made
not-retryable later if we decide malformed event metadata should go to DLT
immediately.

## 18. Scenario: DLT Publication Succeeds

1. The record is selected for recovery.
2. `KafkaDeadLetterTopicResolver` resolves the configured DLT topic.
3. `KafkaDeadLetterHeadersFactory` adds diagnostic `dlt-*` headers.
4. `LoggingDeadLetterPublishingRecoverer` logs the DLT attempt.
5. `KafkaTemplate` publishes the record to the DLT topic.
6. The recoverer waits for the send result.
7. Kafka confirms the DLT send before timeout.
8. `LoggingDeadLetterPublishingRecoverer` logs successful DLT publication.
9. Spring Kafka treats the original record as recovered.

## 19. Scenario: DLT Publication Fails

1. The record is selected for recovery.
2. The recoverer tries to publish to DLT.
3. Kafka rejects the DLT record, Kafka is unavailable, or the send times out.
4. `LoggingDeadLetterPublishingRecoverer` logs the failure.
5. The recoverer throws.
6. Spring Kafka does not treat the original record as recovered.
7. The partition can keep retrying or remain blocked on the problematic record.

This is fail-closed behavior. A poison message is bad, but silently losing the
poison message because DLT is unavailable is worse.

## 20. Scenario: DLT Topic Is Not Configured

1. A record needs DLT recovery.
2. `KafkaDeadLetterTopicResolver` tries to resolve DLT by original topic name.
3. `AppKafkaProperties` cannot find a matching topic mapping or DLT name.
4. Routing fails with a configuration error.
5. The record is not treated as recovered.

This is intentional. All DLT topics must be explicit in configuration.

## 21. Delivery Guarantees

The consumer side should be treated as at-least-once processing.

Kafka can redeliver records after restarts, rebalances, commit timing edges, and
producer duplicates.

The `processed_events` table gives effectively-once business handling for
database side effects protected by the same transaction as the idempotency
marker.

This is not Kafka-level exactly-once delivery.

## 22. Adding A New Consumer

When adding another Kafka consumer:

1. Add `app.kafka.topics.<event-key>` with both `name` and `dlt-name`.
2. Use `ErrorHandlingDeserializer` for key and value if adding custom consumer
   configuration.
3. Give the listener stable `id` and `info` values.
4. Keep the listener thin and delegate to a handler.
5. Annotate the handler with `@IdempotentKafkaEventHandler` when side effects
   must be duplicate-safe.
6. Make sure produced events include `event-id` and `event-type` headers.
7. Keep DLT topic partition count compatible with same-partition routing.
8. Do not add local DLT APIs or DLT database tables unless the architecture
   explicitly changes.
