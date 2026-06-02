# Chapter 17 — Messaging, Events, Outbox and Sagas

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

### _How production systems do work safely after the HTTP request returns_

---

## 17.1 Why Messaging Exists

A controller should not do everything synchronously.

Bad checkout request:

```text
Create order
Charge card
Reduce inventory
Assign driver
Send email
Send push notification
Index in Elasticsearch
Generate invoice
Return response
```

If any step is slow, the API is slow. If any provider fails, the whole request fails.

Better:

```text
HTTP request:
    create order in PostgreSQL
    write outbox events
    return response

Async workers:
    process payment
    update search index
    send notifications
    generate invoice
    dispatch driver
```

---

## 17.2 Kafka vs RabbitMQ

| Need | Prefer |
|---|---|
| Durable event log, replay, stream processing | Kafka |
| Job queue, routing, retries, commands | RabbitMQ |
| High-volume domain events | Kafka |
| Email/SMS/background jobs | RabbitMQ |
| Event sourcing/analytics pipeline | Kafka |
| Simple async workflow | RabbitMQ |

Both can work. Pick based on team experience and operational comfort.

---

## 17.3 Domain Events

Events should describe facts that already happened.

Good:

```kotlin
data class OrderCreatedEvent(
    val eventId: UUID,
    val orderId: UUID,
    val userId: UUID,
    val occurredAt: Instant
)
```

Bad:

```kotlin
data class SendEmailNowPlease(...)
```

Names:

- `OrderCreated`
- `PaymentAuthorized`
- `BookingCancelled`
- `DriverAssigned`
- `MenuChanged`

---

## 17.4 The Outbox Pattern

Problem: PostgreSQL transaction commits, but publishing to Kafka fails. Or Kafka publish succeeds, but DB transaction rolls back.

Outbox solution:

```text
Inside the same PostgreSQL transaction:
    1. Save business data.
    2. Save event row in outbox table.

Separate publisher:
    3. Reads unsent outbox rows.
    4. Publishes to Kafka/RabbitMQ.
    5. Marks outbox row as published.
```

Schema:

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending
ON outbox_events(status, next_attempt_at, created_at);
```

Entity:

```kotlin
@Entity
@Table(name = "outbox_events")
class OutboxEvent(
    @Id val id: UUID = UUID.randomUUID(),
    val aggregateType: String,
    val aggregateId: UUID,
    val eventType: String,
    @JdbcTypeCode(SqlTypes.JSON)
    val payload: Map<String, Any?>,
    var status: OutboxStatus = OutboxStatus.PENDING,
    var attempts: Int = 0,
    var nextAttemptAt: Instant = Instant.now(),
    var publishedAt: Instant? = null
)
```

Good practice: consumers must be idempotent because messages can be delivered more than once.

---

## 17.5 Kafka Producer

```kotlin
@Service
class DomainEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun publish(topic: String, key: String, event: Any) {
        kafkaTemplate.send(topic, key, event)
    }
}
```

Key idea: use the aggregate id as the Kafka key when event order matters for one entity.

```kotlin
producer.publish(
    topic = "orders.events",
    key = orderId.toString(),
    event = OrderCreatedEvent(...)
)
```

Events with the same key go to the same partition, preserving order for that key.

---

## 17.6 Kafka Consumer

```kotlin
@Component
class OrderEventConsumer(
    private val searchIndexer: OrderSearchIndexer
) {
    @KafkaListener(
        topics = ["orders.events"],
        groupId = "search-indexer"
    )
    fun onOrderEvent(event: OrderCreatedEvent) {
        searchIndexer.index(event.orderId)
    }
}
```

Annotation explanation:

- `@KafkaListener`: registers a method as a Kafka message consumer.
- `topics`: topic names to subscribe to.
- `groupId`: consumer group. Each group receives its own copy of events.

Good practice:

- Catch expected business exceptions.
- Let unexpected exceptions retry.
- Use dead-letter topics/queues after max attempts.
- Store processed event IDs for idempotency when side effects matter.

---

## 17.7 Sagas

A saga coordinates multiple steps without one giant distributed transaction.

Booking payment saga:

```text
1. BookingCreated
2. Payment authorization requested
3. PaymentAuthorized
4. BookingConfirmed
5. NotificationSent
```

If payment fails:

```text
PaymentFailed -> BookingCancelled -> InventoryReleased
```

Saga state table:

```sql
CREATE TABLE booking_sagas (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE,
    state VARCHAR(50) NOT NULL,
    last_event_id UUID,
    updated_at TIMESTAMPTZ NOT NULL
);
```

Good practice: each step must be retryable or compensatable.

---

## 17.8 Idempotent Consumer Pattern

```sql
CREATE TABLE processed_events (
    consumer_name VARCHAR(120) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_name, event_id)
);
```

```kotlin
@Transactional
fun handle(event: PaymentAuthorizedEvent) {
    if (processedEventRepository.exists("booking-saga", event.eventId)) {
        return
    }

    bookingSagaService.onPaymentAuthorized(event)
    processedEventRepository.save("booking-saga", event.eventId)
}
```

This protects against duplicate delivery.

---

## 17.9 Source Links

- Spring Boot Kafka: https://docs.spring.io/spring-boot/reference/messaging/kafka.html
- Spring Boot AMQP: https://docs.spring.io/spring-boot/reference/messaging/amqp.html

