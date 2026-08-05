# Kafka consumer configuration

This package contains the consumer side of the Kafka integration: listener
registration, listener retry handling, and dead-letter topic routing.

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

`CustomerAccountCreatedKafkaListener` listens to the configured customer account
created topic:

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
logs topic, partition, offset, key, and payload. It does not yet execute business
logic.

## 4. Listener error handling

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

## 5. Dead-letter topic routing

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

## 6. Scenario: successful record processing

1. Kafka delivers a record to the listener.
2. The key is deserialized as `String`.
3. The value is deserialized as `JsonNode`.
4. `CustomerAccountCreatedKafkaListener` receives the `ConsumerRecord`.
5. The listener completes without throwing an exception.
6. Spring Kafka treats the record as successfully processed.
7. The consumer can continue with subsequent records.

## 7. Scenario: listener throws an exception

1. Kafka delivers a record to the listener.
2. The listener throws an exception during processing.
3. `DefaultErrorHandler` handles the failure.
4. The same record is retried according to `app.kafka.listener.*`.
5. If one retry succeeds, processing continues normally.
6. If all attempts fail, `DeadLetterPublishingRecoverer` publishes the record to
   the configured dead-letter topic.

## 8. Scenario: deserialization fails

1. Kafka delivers a record to the consumer.
2. The value cannot be deserialized by the delegate `JsonDeserializer`.
3. `ErrorHandlingDeserializer` captures the deserialization failure.
4. Spring Kafka routes the failure through listener error handling.
5. After configured retries are exhausted, the record can be sent to the
   configured dead-letter topic.

## 9. Scenario: dead-letter publication succeeds

1. The listener has failed all configured processing attempts.
2. `DeadLetterPublishingRecoverer` asks `KafkaDeadLetterTopicResolver` for the
   target topic and partition.
3. The record is published to the dead-letter topic.
4. Spring Kafka treats the original record as recovered.
5. The consumer can continue with later records from the partition.

## 10. Scenario: dead-letter publication fails

1. The listener has failed all configured processing attempts.
2. Spring Kafka tries to publish the record to the dead-letter topic.
3. The DLT publish fails, for example because Kafka is unavailable.
4. The record is not safely recovered.
5. Depending on container state and broker availability, the partition can keep
   retrying or become blocked on the problematic record.

This scenario should be monitored carefully. A failing DLT path means the system
cannot safely isolate poison messages.

## 11. Current delivery guarantees

The current consumer setup should be treated as at-least-once processing.

The listener can receive the same event more than once after restarts,
rebalances, retry edge cases, or duplicate publication from the outbox producer.

Exactly-once business processing is intentionally not implemented here yet. When
the listener starts performing side effects, idempotent processing should be
added separately, typically by storing processed `event-id` values with a unique
constraint.
