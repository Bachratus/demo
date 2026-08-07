# Kafka Producer Configuration

This package documents the producer side of the Kafka integration.

In this application, the producer side is not a controller or service directly
calling Kafka. It is an outbox publisher. Business code stores an outbox row in
the same database transaction as the business change, and a scheduled publisher
later sends that row to Kafka.

The important boundary is:

```text
business transaction -> outbox_event row -> scheduled publisher -> Kafka
```

This package owns only the last step: publishing persisted outbox rows to Kafka
and updating their producer-side publication status.

## 1. Responsibility

The producer side is responsible for:

- reading publishable rows from `outbox_event`
- publishing them to the configured main Kafka topic
- retrying transient publish failures
- moving exhausted producer failures to the configured producer DLT
- writing clear producer-side statuses back to the database
- attaching stable transport headers to every produced Kafka record

It is not responsible for:

- deciding when a business event should exist
- storing business state
- handling consumer failures after a record was successfully published
- changing `PUBLISHED` outbox rows when a downstream consumer later sends the
  same Kafka record to a consumer DLT

That last point is intentional. `outbox_event.status` describes what happened to
the producer publication attempt. A downstream consumer failure is a separate
consumer lifecycle.

## 2. Configuration Layers

There are two Kafka configuration layers.

`spring.kafka.producer.*` configures the real Kafka client. Spring Boot reads
these properties and auto-configures the `ProducerFactory` and
`KafkaTemplate<String, Object>`.

`app.kafka.*` configures application-level behavior. It is bound to
`AppKafkaProperties` and used by our code for validation, topic metadata,
outbox polling, retry decisions, and send-result wait budgets.

When `app.kafka.enabled=false`, `InfrastructureAutoConfigurationImportFilter`
excludes Spring Boot's `KafkaAutoConfiguration`. In that mode the application
should not have Boot-created Kafka infrastructure such as `KafkaAdmin`,
`KafkaTemplate`, `ProducerFactory`, or `ConsumerFactory`.

We do not declare a custom `ProducerFactory` or `KafkaTemplate` bean here. The
application uses Spring Boot's Kafka auto-configuration and injects the resulting
template:

```java
private final KafkaTemplate<String, Object> kafkaTemplate;
```

This keeps low-level Kafka client wiring in Spring Boot and keeps our code
focused on outbox behavior.

## 3. Producer Client Settings

The real producer client is configured under `spring.kafka.producer.*`.

`acks=all` requires the broker to acknowledge a record only after the leader and
the required replicas confirm it. This favors durability over latency.

`retries=10` lets the Kafka producer retry transient broker or network failures.

`key-serializer=StringSerializer` serializes record keys as strings. The outbox
publisher uses the aggregate id as the key, so records for the same aggregate are
routed consistently to the same partition.

`value-serializer=JsonSerializer` serializes the JSON payload stored in the
outbox row.

`enable.idempotence=true` protects against duplicates caused by producer retries
within a single producer session. It does not provide end-to-end exactly-once
delivery for the whole outbox flow.

`delivery.timeout.ms` is Kafka producer's total deadline for delivering one
record.

`request.timeout.ms` is the timeout for a single request to the broker. It must
be lower than `delivery.timeout.ms`.

`linger.ms` allows the producer to wait briefly before sending a batch. A small
value such as 5 ms can improve throughput without adding meaningful latency for
this application.

`spring.json.add.type.headers=false` prevents Spring Kafka from adding Java class
type headers. Events are described by explicit event metadata such as
`event-type`, not by Java implementation class names.

## 4. Application Producer Settings

`app.kafka.producer.*` is the typed producer configuration used by
`AppKafkaProperties.Producer`.

`delivery-timeout-ms`, `request-timeout-ms`, and `linger-ms` intentionally mirror
the corresponding Kafka client properties. This gives application code a typed
view of the same timing settings and lets `AppKafkaProperties` validate that the
values make sense together.

`send-result-timeout-ms` is application-specific. `OutboxKafkaPublisher` calls:

```java
kafkaTemplate.send(record).get(timeout, TimeUnit.MILLISECONDS);
```

The timeout used there comes from `send-result-timeout-ms`. It must be greater
than `delivery-timeout-ms`, because the application should not stop waiting
before Kafka producer's own delivery budget is exhausted.

With the default values, Kafka has 45 seconds to deliver a record and the
application waits up to 50 seconds for the send result.

## 5. Event-To-Topic Mapping

Application events do not carry physical Kafka topic names. They expose a
logical `eventKey()`, aggregate id, and schema version.

`KafkaOutboxEventDraftFactory` resolves the physical metadata from
`app.kafka.topics.<event-key>`:

```text
OutboxApplicationEvent
 -> KafkaOutboxEventDraftFactory
 -> AppKafkaProperties.topic(event.eventKey())
 -> OutboxEventDraft
```

The topic configuration contains:

- `name`: physical Kafka main topic
- `dlt-name`: physical Kafka dead-letter topic
- `event-type`: semantic event type base name, without schema suffix
- `aggregate-type`: semantic aggregate type
- `concurrency`: consumer-side listener concurrency for this topic

`event-type` is combined with the event schema version:

```text
customer.account-created + schemaVersion 1 -> customer.account-created.v1
```

The decision is deliberate. Schema version belongs to the payload contract, so it
is visible in the final `event-type` stored in the outbox and sent as a Kafka
header.

## 6. Topic Configuration Rules

Topic configuration is strict.

`AppKafkaProperties` fails if:

- a requested logical topic key is missing
- a logical topic key is null, blank, or duplicated after trimming
- a physical topic name is blank
- a DLT topic name is missing or blank
- a DLT topic name is equal to the main topic name
- a topic cannot be resolved by physical name on the consumer side

There is no fallback to `<topic>.dlt`.

This is intentional. A guessed DLT topic can hide configuration mistakes and
send poison messages to a topic nobody monitors. A missing DLT mapping is a
configuration error and should fail loudly.

There is also no global application prefix added in code. Topic names are
complete physical names from configuration, because events can come from, or be
consumed by, other applications.

## 7. Outbox Row Creation

The normal write path is:

```text
business use case
 -> creates application event
 -> OutboxEventDraftFactory creates OutboxEventDraft
 -> OutboxEventStoreAdapter saves OutboxEventJpa
```

`OutboxEventStoreAdapter` resolves the physical topic name when the outbox row is
created. This means the row contains the exact topic name that was intended at
the time of writing.

The outbox row starts with:

```text
status = PENDING
retryCount = 0
nextAttemptAt = now
```

## 8. Scheduler

`OutboxKafkaPublisherScheduler` runs only when both flags are enabled:

```yaml
app.kafka.enabled: true
app.kafka.outbox.enabled: true
```

The scheduler uses:

```java
@Scheduled(fixedDelayString = "${app.kafka.outbox.poll-delay-ms:500}")
```

This is `fixedDelay`, so the delay is counted after the previous batch finishes.
It avoids overlapping scheduler executions inside the same application instance.

Scheduling itself is enabled globally in `infra.jobs`. The outbox package only
decides whether this particular Kafka job should run.

## 9. Batch Selection And Locking

`OutboxKafkaPublisher` reads rows through `streamPublishableEvents`.

The query selects rows whose `next_attempt_at` is due and whose status is one of:

- `PENDING`
- `FAILED` while `retry_count < maxAttempts`
- `DLT_PENDING`

Rows are ordered by `occurred_at` and `id`.

The query uses:

```sql
FOR UPDATE SKIP LOCKED
```

This allows multiple application instances to run the publisher without picking
the same row at the same time. One transaction locks a row; another transaction
skips it and moves on.

## 10. Status Lifecycle

The producer status model is:

`PENDING`

The row exists and has not yet been successfully sent to the main Kafka topic.

`FAILED`

A main topic publish attempt failed, but the retry limit has not been reached.
`retryCount`, `lastError`, and `nextAttemptAt` are updated.

`DLT_PENDING`

The main topic publish attempts are exhausted. The publisher will now try to send
the row to the configured producer DLT.

`PUBLISHED`

Kafka acknowledged the main topic publish and the database transaction marked
the row as published.

`DEAD_LETTERED`

The producer could not publish the row to the main topic after all configured
attempts, but it did publish the row to the producer DLT.

`DEAD_LETTERED` here means "producer dead-lettered this outbox row". It does not
mean "some consumer later failed to process a published record".

## 11. Main Topic Publish Flow

For a normal row, `OutboxKafkaPublisher` creates:

```java
new ProducerRecord<>(
    event.getTopicName(),
    event.getAggregateId(),
    event.getPayload()
);
```

The record key is the aggregate id. This preserves per-aggregate ordering as long
as all events for the same aggregate use the same key and topic.

The record value is the JSON payload stored in the outbox row.

The publisher adds these headers:

- `event-id`
- `event-type`
- `aggregate-type`
- `aggregate-id`
- `occurred-at`
- `content-type`

These headers are generated at send time from persisted outbox fields. Custom
outbox headers are intentionally not stored anymore. The contract stays small,
stable, and queryable.

If the send succeeds, the row becomes `PUBLISHED`.

If the send fails, the row is marked as `FAILED` or `DLT_PENDING`, depending on
the retry count.

## 12. Producer DLT Flow

Producer DLT is used only when this application cannot publish an outbox row to
the main topic after `app.kafka.outbox.max-attempts`.

The DLT topic is resolved from:

```text
app.kafka.topics.<topic-key>.dlt-name
```

The publisher adds these extra DLT headers:

- `dlt-original-topic`
- `dlt-error`

If producer DLT publish succeeds, the row becomes `DEAD_LETTERED`.

If producer DLT publish fails, the row stays `DLT_PENDING`, `lastError` is
updated, and `nextAttemptAt` is moved forward using
`app.kafka.outbox.retry-backoff-ms`.

This means producer DLT publication is retried by the same outbox scheduler.

## 13. Why Consumer DLT Does Not Mutate Outbox

If a row is `PUBLISHED`, the producer did its job. Kafka accepted the record on
the main topic.

If a consumer later fails and sends that record to a consumer DLT, the original
outbox row should remain `PUBLISHED`. Changing it to `DEAD_LETTERED` would mix
two different lifecycles:

- producer publication lifecycle
- downstream consumer processing lifecycle

Consumer DLT records are meant to be handled by a separate DLT monitor or
resolver service. That service can read DLT topics, expose API/UI, and optionally
republish fixed records to main topics.

## 14. Delivery Guarantees

The producer side provides at-least-once delivery.

The critical crash window is:

```text
Kafka accepts record -> application crashes before DB transaction commits
```

After restart, the same outbox row can be selected again and published again.
This is expected for this outbox design.

`enable.idempotence=true` helps the Kafka producer avoid duplicates caused by
producer retries within a producer session. It does not remove duplicates caused
by application crashes between Kafka acknowledgment and database commit.

Consumers must therefore be idempotent. The consumer side uses the `event-id`
header and the `processed_events` table as the foundation for effectively-once
business handling.

## 15. Operational Checklist

When adding a new produced event:

1. Add a stable application event implementing `OutboxApplicationEvent`.
2. Give it a logical `eventKey()`.
3. Add `app.kafka.topics.<event-key>` configuration.
4. Configure both `name` and `dlt-name`.
5. Make sure the DLT topic has at least the same partition count as the main
   topic if same-partition routing is expected.
6. Keep `event-type` semantic and stable.
7. Increment `schemaVersion` only when the payload contract changes.
8. Make consumers idempotent using `event-id`.

When debugging producer publication:

1. Check `outbox_event.status`.
2. Check `retry_count`, `next_attempt_at`, and `last_error`.
3. If status is `DLT_PENDING`, the main topic attempts are exhausted and the
   publisher is retrying producer DLT publication.
4. If status is `DEAD_LETTERED`, the producer DLT publish succeeded.
5. If status is `PUBLISHED`, downstream consumer failures must be investigated
   through consumer logs, consumer DLT topics, or the future DLT resolver.
