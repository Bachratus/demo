# Kafka producer configuration

This package contains the application producer side of the Kafka integration.
In this project, "producer" does not mean a manually declared `ProducerFactory`
bean. Spring Boot creates the Kafka producer infrastructure from
`spring.kafka.producer.*`, and this package uses the resulting
`KafkaTemplate<String, Object>` to publish outbox events.

## 1. Spring Boot creates the KafkaTemplate

There is no custom `ProducerFactory` or `KafkaTemplate` bean in
`com.bachratus.demo.infra.kafka.config`.

Spring Boot reads:

```yaml
spring:
  kafka:
    bootstrap-servers: ...
    producer:
      ...
```

and auto-configures the Kafka producer infrastructure. The outbox publisher then
injects:

```java
private final KafkaTemplate<String, Object> kafkaTemplate;
```

## 2. spring.kafka.producer

`spring.kafka.producer.*` is the real Kafka client configuration.

`acks=all` means the broker acknowledges a record only after the leader and the
required replicas confirm it. This favors durability.

`retries=10` allows the Kafka producer to retry transient broker or network
failures.

`key-serializer=StringSerializer` serializes record keys as strings. The outbox
publisher uses the aggregate id as the key, so events for the same aggregate are
routed consistently to the same partition.

`value-serializer=JsonSerializer` serializes the event payload as JSON. The
outbox stores the payload as a JSON object and publishes it as the Kafka record
value.

`enable.idempotence=true` lets the producer avoid duplicate records caused by
producer retries within a producer session. It fits well with `acks=all`.

`delivery.timeout.ms` is the total producer-side deadline for delivering a
record.

`request.timeout.ms` is the timeout for a single request to the broker. It must
be lower than `delivery.timeout.ms`.

`linger.ms` lets the producer wait briefly before sending a batch. A small value
such as 5 ms can improve throughput with minimal latency cost.

`spring.json.add.type.headers=false` prevents Spring Kafka from adding Java class
type headers. Events stay based on JSON and explicit event metadata instead of
Java implementation class names.

## 3. app.kafka.producer

`app.kafka.producer.*` is the application-level producer configuration bound to
`AppKafkaProperties.Producer`.

`delivery-timeout-ms`, `request-timeout-ms`, and `linger-ms` mirror the values
used under `spring.kafka.producer.properties`. This gives the application code a
typed view of the same timing settings and allows consistency validation.

`send-result-timeout-ms` is application-specific. It controls how long
`OutboxKafkaPublisher` waits for the result of:

```java
kafkaTemplate.send(record).get(timeout, TimeUnit.MILLISECONDS);
```

It should be greater than `delivery-timeout-ms`, so the application does not stop
waiting before the Kafka producer has exhausted its own delivery budget.

## 4. Producer validation

`AppKafkaProperties.Producer` rejects invalid timing values:

- `deliveryTimeoutMs` must be positive.
- `requestTimeoutMs` must be positive.
- `lingerMs` cannot be negative.
- `sendResultTimeoutMs` must be positive.
- `requestTimeoutMs` must be lower than `deliveryTimeoutMs`.
- `sendResultTimeoutMs` must be greater than `deliveryTimeoutMs`.

With the default values, Kafka has 45 seconds to deliver a record and the
application waits up to 50 seconds for the send result.

## 5. OutboxKafkaPublisher lifecycle

`OutboxKafkaPublisher` is active only when both properties are enabled:

```yaml
app.kafka.enabled: true
app.kafka.outbox.enabled: true
```

It reads publishable records from the `outbox_event` table, publishes them to the
configured Kafka topic, and updates their status.

On successful publication, the event is marked as `PUBLISHED`.

On failure, the event is marked for retry by incrementing `retryCount`, storing
`lastError`, and setting `nextAttemptAt` using `app.kafka.outbox.retry-backoff-ms`.

After `app.kafka.outbox.max-attempts` is reached, the event moves to
`DLT_PENDING` and is published to the configured dead-letter topic.

The dead-letter topic is resolved from `app.kafka.topics.<topic-key>.dlt-name`.
It must be configured explicitly and must be different from the main topic name.
The publisher does not fall back to a guessed `<topic>.dlt` name.

## 6. ProducerRecord shape

The publisher creates records as:

```java
new ProducerRecord<>(
    topicName,
    event.getAggregateId(),
    event.getPayload()
);
```

The topic name is resolved when the outbox row is created. The key is the
aggregate id. The value is the event JSON payload.

The publisher adds transport headers at send time:

- `event-id`
- `event-type`
- `aggregate-type`
- `aggregate-id`
- `occurred-at`
- `content-type`

When publishing to a dead-letter topic, it also adds:

- `dlt-original-topic`
- `dlt-error`

Custom outbox headers are intentionally not stored in the outbox table anymore.

## 7. Delivery semantics

The outbox publisher provides at-least-once delivery semantics.

After Kafka acknowledges a record, the publisher still needs to commit the
database transaction that marks the outbox row as `PUBLISHED` or
`DEAD_LETTERED`. If the application crashes after Kafka accepts the record but
before the database transaction is committed, the same outbox row can be picked
up again after restart and published again.

This is expected behavior for the outbox pattern. Consumers must be prepared for
duplicate messages and should use a stable identifier such as the `event-id`
header to make processing idempotent.
