# P0-8 — Messaging & Event-Driven Architecture: Kafka vs RabbitMQ, When and Why

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **The architecture of a resilient system is defined by how services communicate when things go wrong.** Synchronous HTTP calls between services are a ticking time bomb. This chapter explains exactly why, when to use RabbitMQ vs Kafka, and gives you the production Kotlin + Spring Boot 3 code to implement both correctly.

---

## 1. The Problem: Synchronous HTTP Between Services

Imagine Zepto's order placement flow:

```
Client → OrderService (HTTP POST /orders)
              ↓ (HTTP call)
         InventoryService.reserveStock()
              ↓ (HTTP call)
         PaymentService.charge()
              ↓ (HTTP call)
         NotificationService.sendOrderConfirmation()
              ↓ (HTTP call)
         DeliveryService.assignRider()
```

This chain of synchronous HTTP calls creates:

### 1.1 Cascading Failure

```
OrderService → InventoryService (down for 5 seconds)
                     ↓
              OrderService waits (thread held)
                     ↓
              Client timeout at 10s → OrderService still holding thread
                     ↓
              100 clients doing this = 100 threads blocked on InventoryService
                     ↓
              OrderService thread pool exhausted
                     ↓
              OrderService is now also effectively down
                     ↓
              API Gateway → OrderService (down)
                     ↓
              Users see 500 errors
```

A 5-second blip in InventoryService has cascaded into total service unavailability. This is called a **cascading failure** or **bulkhead breach**.

> [!CAUTION]
> At Uber's 2015 production incident, a single service's degradation cascaded through synchronous call chains and took down the entire platform for 2 hours. Their response was to aggressively introduce async patterns and circuit breakers across all inter-service communication.

### 1.2 Temporal Coupling

When OrderService calls PaymentService synchronously, OrderService MUST be running at the same time as PaymentService for the operation to succeed. This is called temporal coupling — neither service can work without the other being available simultaneously.

With messaging: OrderService publishes an `OrderCreated` event to a queue. PaymentService processes it whenever it's ready. If PaymentService restarts, it just picks up where it left off.

### 1.3 Tight Coupling and Deployment Coupling

```kotlin
// Tightly coupled — OrderService knows about every downstream service
class OrderService(
    private val inventoryClient: InventoryClient,
    private val paymentClient: PaymentClient,
    private val notificationClient: NotificationClient,
    private val deliveryClient: DeliveryClient,
) {
    fun placeOrder(order: Order) {
        inventoryClient.reserve(order)
        paymentClient.charge(order)
        notificationClient.notify(order)  // Adding a new service = modify OrderService
        deliveryClient.assign(order)
    }
}

// Loosely coupled with messaging
class OrderService(
    private val eventPublisher: EventPublisher
) {
    fun placeOrder(order: Order) {
        orderRepository.save(order)
        eventPublisher.publish(OrderCreatedEvent(order.id))
        // OrderService doesn't know about Inventory, Payment, or Notification
        // Each service subscribes to events they care about
    }
}
```

With the event-driven model, adding a new service (e.g., `FraudDetectionService`) requires zero changes to OrderService. It simply subscribes to `OrderCreated` events.

### 1.4 The Synchronous Call Performance Cost

Every synchronous call between services adds:
- Network round trip: 1-10ms per hop (within same data center)
- Service processing time
- Connection overhead

In a 10-service chain: 10 × (5ms avg latency) = 50ms minimum overhead before any business logic runs. With messaging, these services run in parallel — all downstream processing happens concurrently, and the API responds to the client after just persisting the order.

---

## 2. Message Broker Fundamentals

A **message broker** is a middleware system that enables asynchronous communication between services.

### Core Concepts

```
Producer                    Broker                    Consumer
─────────                  ────────                  ─────────
OrderService       →    [OrderCreated Queue]    →   PaymentService
                         [OrderCreated Queue]    →   InventoryService
                         [OrderCreated Queue]    →   NotificationService
```

**Producer**: Service that publishes messages to the broker.

**Consumer**: Service that reads and processes messages from the broker.

**Message**: A unit of data sent from producer to consumer. Contains: payload (body), routing key, headers/metadata.

**Queue**: A buffer that holds messages until consumers process them. FIFO (First In, First Out) by default.

**Topic**: A named channel (Kafka) or logical grouping (RabbitMQ) for related messages.

**Acknowledgement (ACK)**: When a consumer successfully processes a message, it sends an ACK to the broker. The broker then removes the message. If the consumer crashes before ACKing, the broker redelivers the message to another consumer.

**Dead Letter Queue (DLQ)**: Where messages go when they fail processing repeatedly (after N retries). Critical for observability — you can inspect failed messages without losing them.

---

## 3. RabbitMQ — AMQP-Based Message Broker

RabbitMQ implements the AMQP 0-9-1 protocol. Its key abstraction is the **Exchange** — messages are sent to exchanges, not directly to queues.

### Architecture

```
Producer → Exchange → (Binding Rules) → Queue → Consumer
```

The exchange applies routing rules to decide which queues receive a copy of each message.

### 3.1 Exchange Types

**Direct Exchange** — Route by exact routing key match

```
Producer → DirectExchange("order-events")
                    ↓ (routing key: "order.created")
             Queue: "payment-queue"     ← matches "order.created" binding
             Queue: "inventory-queue"   ← matches "order.created" binding

                    ↓ (routing key: "order.cancelled")
             Queue: "refund-queue"      ← matches "order.cancelled" binding
```

**Fanout Exchange** — Broadcast to ALL bound queues (ignore routing key)

```
Producer → FanoutExchange("notifications")
                    ↓ (every message goes everywhere)
             Queue: "email-queue"
             Queue: "sms-queue"
             Queue: "push-notification-queue"
```

**Topic Exchange** — Wildcard routing key matching

```
Producer → TopicExchange("app-events")
                    ↓ (routing key: "order.india.created")
             Queue: "india-orders"    ← bound with "order.india.*"
             Queue: "all-orders"      ← bound with "order.#" (# = any words)

                    ↓ (routing key: "payment.india.failed")
             Queue: "india-alerts"    ← bound with "*.india.*"
             Queue: "all-failures"    ← bound with "#.failed"
```

**Headers Exchange** — Route by message headers (not routing key)

```
Producer → HeadersExchange("content-router")
           Message with headers: {format: "pdf", region: "india"}
                    ↓
           Queue: "pdf-india-queue"   ← bound with x-match=all, format=pdf, region=india
```

### 3.2 When to Use RabbitMQ

| Use Case | Why RabbitMQ |
|---|---|
| Email / SMS / Push notification dispatch | Fire-and-forget, simple queue, Fanout for multi-channel |
| Background job processing | Direct exchange, worker queue pattern |
| Retry with exponential backoff | Built-in DLQ + TTL + re-queue mechanism |
| Short-lived task queues | Messages consumed and deleted — no retention needed |
| Request-Reply pattern | RabbitMQ supports reply-to queues |
| Small-medium teams | Simpler operations than Kafka |

> [!IMPORTANT]
> **RabbitMQ deletes messages after consumption.** This is its design. Once a consumer ACKs a message, it's gone forever. This means you cannot replay events to rebuild state. If you need event replay, audit logs, or event sourcing — you need Kafka.

### 3.3 Spring AMQP — Configuration

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-amqp")
}
```

```yaml
# application.yml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual          # We manually ACK/NACK
        prefetch: 10                       # Process 10 messages at a time per consumer
        retry:
          enabled: true
          initial-interval: 1000ms
          max-attempts: 3
          multiplier: 2.0                 # Exponential backoff: 1s, 2s, 4s
          max-interval: 10000ms
    template:
      mandatory: true                     # Return unroutable messages (don't silently drop)
```

```kotlin
// RabbitMQConfig.kt
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val ORDER_EXCHANGE         = "order-events"
        const val ORDER_CREATED_QUEUE    = "order.created"
        const val ORDER_CANCELLED_QUEUE  = "order.cancelled"
        const val ORDER_DLQ              = "order.created.dlq"
        const val ORDER_DLX              = "order.dlx"  // Dead letter exchange

        const val NOTIFICATION_EXCHANGE  = "notification-fanout"
        const val EMAIL_QUEUE            = "notification.email"
        const val SMS_QUEUE              = "notification.sms"
        const val PUSH_QUEUE             = "notification.push"
    }

    // =========================================================
    // Message Converter — Use JSON instead of Java serialization
    // =========================================================
    @Bean
    fun messageConverter() = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        return RabbitTemplate(connectionFactory).apply {
            messageConverter = messageConverter()
        }
    }

    // =========================================================
    // Exchanges
    // =========================================================
    @Bean fun orderExchange() = TopicExchange(ORDER_EXCHANGE)
    @Bean fun deadLetterExchange() = DirectExchange(ORDER_DLX)
    @Bean fun notificationFanout() = FanoutExchange(NOTIFICATION_EXCHANGE)

    // =========================================================
    // Queues (with DLQ configuration)
    // =========================================================
    @Bean
    fun orderCreatedQueue(): Queue {
        return QueueBuilder
            .durable(ORDER_CREATED_QUEUE)
            // On failure, send to DLX with routing key = queue name
            .withArgument("x-dead-letter-exchange", ORDER_DLX)
            .withArgument("x-dead-letter-routing-key", ORDER_DLQ)
            // Optional: max retries via TTL (messages expire and go to DLQ)
            .withArgument("x-message-ttl", 300000)  // 5 minutes
            .build()
    }

    @Bean
    fun orderDeadLetterQueue(): Queue = QueueBuilder
        .durable(ORDER_DLQ)
        .build()

    @Bean fun emailQueue()  = QueueBuilder.durable(EMAIL_QUEUE).build()
    @Bean fun smsQueue()    = QueueBuilder.durable(SMS_QUEUE).build()
    @Bean fun pushQueue()   = QueueBuilder.durable(PUSH_QUEUE).build()

    // =========================================================
    // Bindings
    // =========================================================
    @Bean
    fun orderCreatedBinding(): Binding = BindingBuilder
        .bind(orderCreatedQueue())
        .to(orderExchange())
        .with("order.#.created")  // Matches: order.india.created, order.created, etc.

    @Bean
    fun dlqBinding(): Binding = BindingBuilder
        .bind(orderDeadLetterQueue())
        .to(deadLetterExchange())
        .with(ORDER_DLQ)

    // Fanout bindings — all notification queues receive every notification message
    @Bean fun emailBinding() = BindingBuilder.bind(emailQueue()).to(notificationFanout())
    @Bean fun smsBinding()   = BindingBuilder.bind(smsQueue()).to(notificationFanout())
    @Bean fun pushBinding()  = BindingBuilder.bind(pushQueue()).to(notificationFanout())
}
```

### 3.4 RabbitMQ Producer

```kotlin
// OrderEventPublisher.kt
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class OrderEventPublisher(
    private val rabbitTemplate: RabbitTemplate
) {

    fun publishOrderCreated(order: Order) {
        val event = OrderCreatedEvent(
            orderId = order.id.toString(),
            userId = order.userId.toString(),
            totalAmount = order.totalAmount,
            items = order.items.map { ItemDto(it.skuId, it.quantity, it.price) },
            createdAt = Instant.now().toString()
        )

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            "order.created",   // Routing key
            event,
            { message ->
                message.messageProperties.apply {
                    contentType = "application/json"
                    messageId = order.id.toString()   // For idempotency on consumer side
                    timestamp = java.util.Date()
                }
                message
            }
        )
    }

    fun publishOrderCancelled(orderId: String, reason: String) {
        val event = OrderCancelledEvent(orderId = orderId, reason = reason)
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            "order.cancelled",
            event
        )
    }

    fun publishNotification(notification: NotificationEvent) {
        // Fanout — no routing key needed
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.NOTIFICATION_EXCHANGE,
            "",   // Routing key ignored for fanout
            notification
        )
    }
}

data class OrderCreatedEvent(
    val orderId: String,
    val userId: String,
    val totalAmount: Double,
    val items: List<ItemDto>,
    val createdAt: String
)

data class ItemDto(val skuId: String, val quantity: Int, val price: Double)
data class OrderCancelledEvent(val orderId: String, val reason: String)
data class NotificationEvent(
    val userId: String,
    val title: String,
    val body: String,
    val type: String  // "ORDER_CONFIRMED", "DELIVERY_UPDATE", etc.
)
```

### 3.5 RabbitMQ Consumer with Manual ACK

```kotlin
// PaymentConsumer.kt
import com.rabbitmq.client.Channel
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class PaymentConsumer(
    private val paymentService: PaymentService,
    private val idempotencyService: IdempotencyService,
) {

    @RabbitListener(queues = [RabbitMQConfig.ORDER_CREATED_QUEUE], ackMode = "MANUAL")
    fun handleOrderCreated(
        event: OrderCreatedEvent,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long,
        @Header(AmqpHeaders.MESSAGE_ID) messageId: String?
    ) {
        try {
            // IDEMPOTENCY CHECK — Kafka and RabbitMQ both can deliver messages more than once
            // (RabbitMQ: on consumer crash before ACK, message is redelivered)
            if (messageId != null && idempotencyService.alreadyProcessed(messageId)) {
                // Already processed — ACK and skip
                channel.basicAck(deliveryTag, false)
                return
            }

            // Process the payment
            paymentService.initiatePayment(
                orderId = event.orderId,
                userId = event.userId,
                amount = event.totalAmount
            )

            // Mark as processed (idempotency record)
            if (messageId != null) {
                idempotencyService.markProcessed(messageId)
            }

            // ACK — tells RabbitMQ the message was successfully processed, remove it
            channel.basicAck(deliveryTag, false)

        } catch (ex: TransientException) {
            // Transient error (e.g., DB temporarily unavailable) — NACK and requeue
            // Message will be retried (up to max-attempts in config)
            channel.basicNack(
                deliveryTag,
                false,    // multiple = false (only this message)
                true      // requeue = true (put back in queue)
            )
        } catch (ex: PermanentException) {
            // Permanent error (e.g., invalid order data) — NACK without requeue → goes to DLQ
            channel.basicNack(
                deliveryTag,
                false,    // multiple = false
                false     // requeue = false → routed to Dead Letter Queue
            )
        } catch (ex: Exception) {
            // Unknown error — don't requeue, let it go to DLQ for inspection
            channel.basicNack(deliveryTag, false, false)
            throw ex
        }
    }
}

class TransientException(message: String, cause: Throwable? = null) : Exception(message, cause)
class PermanentException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

> [!WARNING]
> **Never use `ackMode = "AUTO"` in production.** Auto-ACK acknowledges the message immediately when the listener method is invoked, before your business logic runs. If your DB call fails halfway through, the message is already gone — no retry, no DLQ. Always use `MANUAL` ACK in production so you control exactly when a message is considered "done."

### 3.6 Dead Letter Queue Consumer

```kotlin
// DLQConsumer.kt — Monitor and alert on failed messages
@Component
class DLQConsumer(
    private val alertingService: AlertingService,
    private val dlqRepository: DLQMessageRepository,
) {

    @RabbitListener(queues = [RabbitMQConfig.ORDER_DLQ])
    fun handleDeadLetter(
        event: OrderCreatedEvent,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long,
        @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) originalRoutingKey: String,
        @Header("x-death") xDeath: Any?  // Contains failure reason
    ) {
        try {
            // Persist for manual inspection
            dlqRepository.save(
                DLQMessage(
                    originalQueue = originalRoutingKey,
                    payload = event.toString(),
                    failureReason = xDeath.toString(),
                    receivedAt = java.time.Instant.now()
                )
            )

            // Alert on-call engineer (PagerDuty, Slack, etc.)
            alertingService.sendCriticalAlert(
                title = "Message Failed Processing — DLQ",
                message = "Order ${event.orderId} failed payment processing. Check DLQ.",
                context = mapOf(
                    "orderId" to event.orderId,
                    "userId" to event.userId,
                    "originalQueue" to originalRoutingKey
                )
            )

            channel.basicAck(deliveryTag, false)
        } catch (ex: Exception) {
            channel.basicNack(deliveryTag, false, false)  // Don't requeue DLQ messages
        }
    }
}
```

---

## 4. Apache Kafka — Distributed Commit Log

Kafka is fundamentally different from RabbitMQ. It is not a message queue — it is a **distributed, append-only log**.

### Core Architecture

```
                        ┌─────────────────────────────────┐
                        │  Topic: "order-events"           │
                        │                                   │
  Producer →            │  Partition 0: [msg0][msg1][msg5] │
  OrderService          │  Partition 1: [msg2][msg3][msg6] │
                        │  Partition 2: [msg4][msg7][msg8] │
                        └──────────────────┬──────────────┘
                                           │
                              ┌────────────┴────────────────┐
                              │                              │
                    Consumer Group A               Consumer Group B
                    "payment-service"              "analytics-service"
                    ┌──────────────────┐          ┌──────────────────┐
                    │ Consumer 1:      │          │ Consumer 1:      │
                    │ reads Partition 0│          │ reads ALL parts  │
                    │ Consumer 2:      │          │ (independent     │
                    │ reads Partition 1│          │  offset tracking)│
                    │ Consumer 3:      │          └──────────────────┘
                    │ reads Partition 2│
                    └──────────────────┘
```

### Key Concepts

**Topic**: A named stream of records. Like a table in a database, but append-only.

**Partition**: A topic is split into N partitions. Each partition is an ordered, immutable sequence of records. Parallelism in Kafka = number of partitions.

**Offset**: Each record in a partition has a sequential ID (offset). Consumer tracks its offset — where it last read. Kafka stores offsets in an internal topic (`__consumer_offsets`).

**Consumer Group**: A set of consumers that collectively consume a topic. Each partition is assigned to exactly one consumer in a group. Adding consumers = more parallelism. More consumers than partitions = some consumers idle.

**Retention**: Kafka retains messages for a configured time (default 7 days) or size, regardless of consumption. Messages are NOT deleted after consumption. This is the fundamental difference from RabbitMQ.

**Replication**: Each partition is replicated across N brokers (replicas). One replica is the leader, others are followers. If leader dies, a follower is elected.

### 4.1 When to Use Kafka

| Use Case | Why Kafka |
|---|---|
| Event sourcing | Messages retained, can replay to rebuild state |
| Audit logs | Immutable append-only log, compliance-friendly |
| Analytics pipeline | Multiple consumer groups can independently process same events |
| Cross-service event broadcasting | Multiple independent services consume same topic with separate offsets |
| High-throughput event streams | Kafka handles millions of events/second per partition |
| Real-time data pipelines | Kafka Connect, Kafka Streams for ETL |

**Kafka at production companies:**
- Netflix: Uses Kafka to process 700 billion events/day for analytics, recommendations, and monitoring
- Uber: Kafka handles 1.7 trillion messages/year for surge pricing, driver matching, and trip data
- Stripe: Kafka for event-driven architecture across payment processing microservices

> [!IMPORTANT]
> **The fundamental Kafka vs RabbitMQ decision comes down to one question: "Do you need to replay messages?"**
> - **Yes** (audit trail, event sourcing, analytics must process historical events): Kafka
> - **No** (task queue, process-and-forget, email dispatch): RabbitMQ
>
> Choosing Kafka when you need a simple task queue is over-engineering. Choosing RabbitMQ when you need event replay is a correctness bug.

### 4.2 Spring Kafka — Configuration

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.kafka:spring-kafka")
}
```

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      # Reliability settings:
      acks: all              # Wait for all replicas to ACK (strongest guarantee)
      retries: 3             # Retry on transient failures
      enable-idempotence: true  # Idempotent producer — no duplicates on retry
      # Performance settings:
      batch-size: 16384      # Batch messages for higher throughput (16KB)
      linger-ms: 5           # Wait up to 5ms to fill a batch
      compression-type: snappy  # Compress batches (reduces network IO)
      
    consumer:
      group-id: payment-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest    # Start from beginning if no committed offset
      enable-auto-commit: false      # NEVER use auto-commit in production
      max-poll-records: 50           # Process 50 records per poll
      properties:
        spring.json.trusted.packages: "com.myapp.events"
        
    listener:
      ack-mode: MANUAL_IMMEDIATE  # Manual offset commit
      concurrency: 3              # 3 listener threads (match partition count)
```

### 4.3 Kafka Topics and Partitions

```kotlin
// KafkaConfig.kt
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {

    companion object {
        const val ORDER_EVENTS_TOPIC     = "order-events"
        const val PAYMENT_EVENTS_TOPIC   = "payment-events"
        const val INVENTORY_EVENTS_TOPIC = "inventory-events"
        const val AUDIT_LOG_TOPIC        = "audit-log"
    }

    // Partitions = max parallelism for consumption
    // Replicas = fault tolerance (requires ≥ N brokers)
    @Bean
    fun orderEventsTopic(): NewTopic = TopicBuilder
        .name(ORDER_EVENTS_TOPIC)
        .partitions(12)    // 12 consumers can process in parallel
        .replicas(3)       // Survives 2 broker failures
        .compact()         // Keep only latest value per key (for state compaction)
        .build()

    @Bean
    fun paymentEventsTopic(): NewTopic = TopicBuilder
        .name(PAYMENT_EVENTS_TOPIC)
        .partitions(6)
        .replicas(3)
        .build()

    @Bean
    fun auditLogTopic(): NewTopic = TopicBuilder
        .name(AUDIT_LOG_TOPIC)
        .partitions(6)
        .replicas(3)
        .config("retention.ms", "31536000000")  // Retain for 1 year (compliance)
        .build()
}
```

> [!NOTE]
> **Partition count is the most important and least reversible decision in Kafka.** You can increase partition count later, but you cannot decrease it (requires topic recreation + data migration). Start with more partitions than you think you need. Rule of thumb: `partition_count = target_throughput / throughput_per_consumer`. If each consumer processes 10,000 msg/sec and you need 120,000 msg/sec, use 12 partitions.

### 4.4 Kafka Producer

```kotlin
// OrderEventPublisher.kt
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class KafkaOrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    /**
     * Publish an OrderCreated event to Kafka.
     * 
     * Key = orderId: Ensures all events for the same order go to the same partition,
     * preserving ordering for events about a single order.
     */
    fun publishOrderCreated(order: Order) {
        val event = OrderCreatedEvent(
            eventId = java.util.UUID.randomUUID().toString(),  // For idempotency
            orderId = order.id.toString(),
            userId = order.userId.toString(),
            totalAmount = order.totalAmount,
            items = order.items.map { ItemDto(it.skuId, it.quantity, it.price) },
            eventTimestamp = java.time.Instant.now().toString()
        )

        val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(
            KafkaConfig.ORDER_EVENTS_TOPIC,
            order.id.toString(),   // Partition key — same order → same partition → ordered
            event
        )

        future.whenComplete { result, ex ->
            if (ex != null) {
                // Producer failed AFTER retries — this is a serious problem
                // Log, alert, and consider the Outbox Pattern to handle this
                logger.error("Failed to publish OrderCreated event for order ${order.id}", ex)
                // The Outbox Pattern prevents data loss here — see Section 6
            } else {
                logger.info(
                    "Published OrderCreated: orderId=${order.id}, " +
                    "partition=${result.recordMetadata.partition()}, " +
                    "offset=${result.recordMetadata.offset()}"
                )
            }
        }
    }
}
```

### 4.5 Kafka Consumer

```kotlin
// PaymentKafkaConsumer.kt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.TopicPartition
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class PaymentKafkaConsumer(
    private val paymentService: PaymentService,
    private val idempotencyService: IdempotencyService,
) {

    /**
     * Consumer for order-events topic.
     * concurrency in @KafkaListener spins up multiple threads,
     * each consuming from a subset of partitions.
     */
    @KafkaListener(
        topics = [KafkaConfig.ORDER_EVENTS_TOPIC],
        groupId = "payment-service",
        concurrency = "3"   // 3 threads, each handling ~4 partitions (for 12-partition topic)
    )
    fun handleOrderCreated(
        record: ConsumerRecord<String, OrderCreatedEvent>,
        acknowledgment: Acknowledgment
    ) {
        val event = record.value()
        val eventId = event.eventId

        try {
            // Idempotency: event may be delivered more than once
            // (Kafka at-least-once delivery guarantee)
            if (idempotencyService.alreadyProcessed(eventId)) {
                acknowledgment.acknowledge()
                return
            }

            paymentService.initiatePayment(
                orderId = event.orderId,
                userId = event.userId,
                amount = event.totalAmount
            )

            idempotencyService.markProcessed(eventId)

            // Commit offset — tell Kafka we successfully processed up to this offset
            acknowledgment.acknowledge()

        } catch (ex: Exception) {
            // Do NOT acknowledge — Kafka will redeliver this message
            // Consider: after N retries, publish to a separate error topic
            logger.error("Failed to process OrderCreated event: orderId=${event.orderId}", ex)
            // Do NOT rethrow — Spring Kafka will handle retry via error handler
            throw ex
        }
    }

    /**
     * Consume from a specific partition — for ordered processing.
     * Use when you need strict ordering (e.g., payment state machine events).
     */
    @KafkaListener(
        topicPartitions = [
            TopicPartition(
                topic = "payment-events",
                partitions = ["0", "1", "2"]
            )
        ],
        groupId = "payment-reconciliation"
    )
    fun handlePaymentEventsOrdered(
        record: ConsumerRecord<String, PaymentEvent>,
        acknowledgment: Acknowledgment
    ) {
        // Processes partitions 0, 1, 2 only — strict in-partition ordering guaranteed
        processPaymentEvent(record.value())
        acknowledgment.acknowledge()
    }

    private fun processPaymentEvent(event: PaymentEvent) {
        // business logic
    }
}
```

### 4.6 Kafka Error Handling and Dead Letter Topics

```kotlin
// KafkaErrorConfig.kt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
class KafkaErrorConfig(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    /**
     * Configure retry with exponential backoff + dead letter topic after exhaustion.
     * 
     * Failed records are automatically published to a "topic-name.DLT" topic.
     */
    @Bean
    fun kafkaErrorHandler(): CommonErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        // ↑ Publishes failed records to: <original-topic>.DLT
        // e.g., "order-events" failures go to "order-events.DLT"

        val backoff = ExponentialBackOff(
            1_000L,  // Initial interval: 1 second
            2.0      // Multiplier: 1s, 2s, 4s, 8s, 16s
        ).apply {
            maxAttempts = 5L   // After 5 attempts, send to DLT
        }

        return DefaultErrorHandler(recoverer, backoff)
    }
}
```

```kotlin
// DLTConsumer.kt — Monitor dead letter topic
@Component
class KafkaDLTConsumer(
    private val alertingService: AlertingService,
    private val dlqRepository: KafkaDLQRepository,
) {

    @KafkaListener(
        topics = ["order-events.DLT"],
        groupId = "dlt-monitor"
    )
    fun handleDeadLetter(
        record: ConsumerRecord<String, ByteArray>,
        acknowledgment: Acknowledgment,
        @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) originalTopic: String,
        @Header(KafkaHeaders.DLT_ORIGINAL_PARTITION) originalPartition: Int,
        @Header(KafkaHeaders.DLT_ORIGINAL_OFFSET) originalOffset: Long,
        @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) exceptionMessage: String,
    ) {
        dlqRepository.save(
            KafkaDLQRecord(
                originalTopic = originalTopic,
                partition = originalPartition,
                offset = originalOffset,
                key = record.key().toString(),
                payload = String(record.value()),
                error = exceptionMessage,
                timestamp = java.time.Instant.now()
            )
        )

        alertingService.sendCriticalAlert(
            title = "Kafka DLT — Message Processing Failed",
            message = "Failed message on $originalTopic at offset $originalOffset: $exceptionMessage"
        )

        acknowledgment.acknowledge()
    }
}
```

---

## 5. Critical Comparison: RabbitMQ vs Kafka

| Dimension | RabbitMQ | Kafka |
|---|---|---|
| **Core abstraction** | Message queue | Distributed log |
| **Message retention** | Deleted after ACK | Retained by time/size (default 7 days) |
| **Message replay** | ❌ Not possible | ✅ Replay any historical offset |
| **Consumer model** | Push (broker pushes to consumer) | Pull (consumer polls broker) |
| **Ordering guarantee** | Per-queue FIFO | Per-partition ordered |
| **Throughput** | Hundreds of thousands/sec | Millions/sec |
| **Routing** | Flexible (Exchange types) | By partition key |
| **Protocol** | AMQP | Custom binary protocol |
| **Consumer groups** | Competing consumers share queue | Each group gets ALL messages independently |
| **Dead letters** | DLQ (manual setup) | DLT (built-in with Spring) |
| **Operations complexity** | Lower | Higher (ZooKeeper/KRaft, partition management) |
| **Best for** | Task queues, notifications, simple jobs | Event sourcing, analytics, high-volume streams |

### The Key Architectural Difference — Illustrated

```
RabbitMQ — One Queue, Multiple Consumers:

[OrderCreated Message] → Queue → PaymentConsumer
                                (message deleted after ACK)
                      ↗
                 Exchange
                      ↘
                    Queue → InventoryConsumer
                                (separate queue — still deleted after ACK)

→ Each service needs its own queue for the same event
→ Message lifecycle: born → consumed → gone


Kafka — One Topic, Multiple Independent Consumer Groups:

[OrderCreated Event] → Partition 0
                       Partition 1
                       Partition 2

PaymentService Consumer Group:     reads at offset 4,523,891
InventoryService Consumer Group:   reads at offset 4,523,891
AnalyticsService Consumer Group:   reads at offset 1,200,000  ← 3M events behind, catching up
NewFraudService Consumer Group:    reads at offset 0           ← reading ALL historical events

→ All groups read from THE SAME topic independently
→ Message lifecycle: born → stays for 7 days (or configured retention)
→ You can add new consumer groups at any time and replay all history
```

---

## 6. The Outbox Pattern — Solving the Dual-Write Problem

The dual-write problem: After saving an order to PostgreSQL, you publish an event to Kafka/RabbitMQ. These are two separate operations. What happens if the app crashes between them?

```
1. Save order to PostgreSQL ✅
2. (App crashes here — power cut, OOM kill, deploy)
3. Publish to Kafka ❌ — never happens
```

Result: Order exists in DB, but PaymentService never received the event. Order is stuck forever.

### Why You Cannot Just Use a Transaction

```kotlin
// This looks correct but has a race condition
@Transactional
fun placeOrder(request: OrderRequest): Order {
    val order = orderRepository.save(request.toOrder())
    kafkaTemplate.send("order-events", OrderCreatedEvent(order.id))  // Not part of DB transaction!
    return order
    // If Kafka publish fails, @Transactional rolls back DB — order lost
    // If app crashes after DB commit but before Kafka send — event lost
}
```

Kafka and your database are two separate systems. There is no two-phase commit (2PC) between them in most architectures.

### The Outbox Pattern Solution

```sql
-- Create an outbox table in your database
CREATE TABLE outbox_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    VARCHAR(255) NOT NULL,  -- e.g., order ID
    aggregate_type  VARCHAR(100) NOT NULL,  -- e.g., "Order"
    event_type      VARCHAR(100) NOT NULL,  -- e.g., "OrderCreated"
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMP,              -- NULL until published
    retry_count     INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
```

```kotlin
// OrderService.kt — Transactional write to DB + outbox atomically
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxRepository: OutboxRepository,
) {

    @Transactional
    fun placeOrder(request: OrderRequest): Order {
        // 1. Save order
        val order = orderRepository.save(request.toOrder())

        // 2. Write to outbox (SAME transaction as order save)
        val outboxEvent = OutboxEvent(
            aggregateId = order.id.toString(),
            aggregateType = "Order",
            eventType = "OrderCreated",
            payload = jacksonObjectMapper().writeValueAsString(
                OrderCreatedEvent(
                    eventId = java.util.UUID.randomUUID().toString(),
                    orderId = order.id.toString(),
                    userId = order.userId.toString(),
                    totalAmount = order.totalAmount,
                    items = order.items.map { ItemDto(it.skuId, it.quantity, it.price) },
                    eventTimestamp = java.time.Instant.now().toString()
                )
            )
        )
        outboxRepository.save(outboxEvent)

        // Both saves are in ONE DB transaction — atomic.
        // If the transaction commits, BOTH the order AND the outbox event exist.
        // If it rolls back, NEITHER exists.
        // App crashes here? No problem — the outbox event persists in DB.
        return order
    }
}
```

```kotlin
// OutboxPoller.kt — Background job that publishes outbox events to Kafka
@Component
class OutboxPoller(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {

    @Scheduled(fixedDelay = 1000)  // Poll every 1 second
    @Transactional
    fun pollAndPublish() {
        val unpublishedEvents = outboxRepository.findUnpublished(limit = 100)

        unpublishedEvents.forEach { event ->
            try {
                val payload = objectMapper.readValue(event.payload, Map::class.java)
                val topic = resolveTopicForEventType(event.eventType)

                kafkaTemplate.send(topic, event.aggregateId, payload).get()
                // .get() makes it synchronous — we know it succeeded before marking published

                event.publishedAt = java.time.Instant.now()
                outboxRepository.save(event)

            } catch (ex: Exception) {
                event.retryCount++
                outboxRepository.save(event)

                if (event.retryCount >= 5) {
                    alertingService.sendAlert("Outbox event stuck: ${event.id}")
                }
            }
        }
    }

    private fun resolveTopicForEventType(eventType: String) = when (eventType) {
        "OrderCreated"   -> KafkaConfig.ORDER_EVENTS_TOPIC
        "OrderCancelled" -> KafkaConfig.ORDER_EVENTS_TOPIC
        "PaymentCharged" -> KafkaConfig.PAYMENT_EVENTS_TOPIC
        else -> throw IllegalArgumentException("Unknown event type: $eventType")
    }
}
```

> [!IMPORTANT]
> **Debezium CDC is the production-grade alternative to polling.** Instead of a scheduler that polls the outbox table (which has 1-second latency and adds DB load), Debezium connects to PostgreSQL's WAL (Write-Ahead Log) and streams changes in real-time to Kafka. This gives sub-100ms latency and zero polling overhead. The architecture: `PostgreSQL WAL → Debezium Connector → Kafka → Consumer Services`. Debezium is what Netflix, Stripe, and most mature event-driven systems use for exactly this problem.

---

## 7. Idempotency — Consumers MUST Handle Duplicate Delivery

Both RabbitMQ and Kafka provide **at-least-once delivery**. This means:
- A message may be delivered once (normal case)
- A message may be delivered multiple times (on consumer crash before ACK/commit)
- A message will NEVER be lost (assuming durable queues/topics with replication)

**There is no exactly-once delivery by default.** Your consumer MUST be idempotent.

### Why Duplicates Happen

```
Kafka Scenario:
1. Consumer reads OrderCreated(orderId=123) at offset 500
2. Consumer calls PaymentService.charge(orderId=123) → Payment succeeds ✅
3. Consumer starts to commit offset 500...
4. Consumer process is killed (deploy, OOM, crash)
5. Kafka never received the offset commit
6. Consumer restarts, sees uncommitted offset, re-reads offset 500
7. Consumer calls PaymentService.charge(orderId=123) AGAIN → 💥 Double charge!
```

### Idempotency Implementation

```kotlin
// IdempotencyService.kt
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class IdempotencyService(
    private val redisTemplate: StringRedisTemplate
) {

    /**
     * Check if a message has already been processed.
     * Uses Redis with TTL — eventId must be unique per event.
     */
    fun alreadyProcessed(eventId: String): Boolean {
        val key = "processed:$eventId"
        return redisTemplate.hasKey(key)
    }

    /**
     * Mark an event as processed.
     * TTL = how long to remember (longer than Kafka retention for safety)
     */
    fun markProcessed(eventId: String, ttl: Duration = Duration.ofDays(30)) {
        val key = "processed:$eventId"
        redisTemplate.opsForValue().set(key, "1", ttl)
    }

    /**
     * Atomic check-and-set — use this to prevent race conditions
     * in concurrent consumer setups.
     */
    fun checkAndMarkProcessed(eventId: String, ttl: Duration = Duration.ofDays(30)): Boolean {
        val key = "processed:$eventId"
        // SETNX (Set if Not eXists) is atomic — returns true if key was set (first time)
        return redisTemplate.opsForValue().setIfAbsent(key, "1", ttl) == true
    }
}
```

```kotlin
// PaymentKafkaConsumer.kt — Using atomic idempotency check
@KafkaListener(topics = ["order-events"], groupId = "payment-service")
fun handleOrderCreated(
    record: ConsumerRecord<String, OrderCreatedEvent>,
    acknowledgment: Acknowledgment
) {
    val event = record.value()

    // Atomic check: if already processed, skip
    if (!idempotencyService.checkAndMarkProcessed(event.eventId)) {
        // Already processed (duplicate delivery)
        acknowledgment.acknowledge()
        return
    }

    // First time seeing this event — process it
    paymentService.initiatePayment(event.orderId, event.userId, event.totalAmount)
    acknowledgment.acknowledge()
}
```

### Database-Level Idempotency

For payment and order operations, Redis-based idempotency is not sufficient — Redis can lose data on failure. Use a database unique constraint:

```sql
-- Idempotency keys table
CREATE TABLE idempotency_keys (
    key         VARCHAR(255) PRIMARY KEY,  -- eventId
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP NOT NULL
);

-- Index for cleanup job
CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at);
```

```kotlin
// DatabaseIdempotencyService.kt
@Service
class DatabaseIdempotencyService(
    private val idempotencyKeyRepository: IdempotencyKeyRepository
) {

    @Transactional
    fun processIfNotDuplicate(eventId: String, operation: () -> Unit): Boolean {
        return try {
            idempotencyKeyRepository.save(
                IdempotencyKey(
                    key = eventId,
                    expiresAt = java.time.Instant.now().plusSeconds(86400 * 30) // 30 days
                )
            )
            operation()
            true
        } catch (ex: DataIntegrityViolationException) {
            // Unique constraint violation = duplicate event
            false
        }
    }
}
```

---

## 8. Exactly-Once Semantics in Kafka

Kafka supports exactly-once semantics (EOS) through:

### 8.1 Idempotent Producer

```yaml
spring:
  kafka:
    producer:
      enable-idempotence: true  # Producer assigns sequence numbers
      acks: all                  # Required for idempotent producer
      retries: 2147483647        # Max retries (effectively infinite)
      max-in-flight-requests-per-connection: 5  # Max 5 for idempotent
```

With `enable-idempotence: true`, the Kafka broker deduplicates retried producer messages. If the producer retries a failed send, the broker recognizes the sequence number and ignores the duplicate.

This gives exactly-once within a single partition. It does NOT give exactly-once across multiple topics.

### 8.2 Transactional Producer — Atomic Multi-Topic Writes

```kotlin
// TransactionalKafkaService.kt
@Service
class TransactionalKafkaService(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    /**
     * Atomically publish to multiple topics.
     * Either ALL succeed or NONE are visible to consumers.
     */
    fun publishAtomically(orderId: String, order: OrderCreatedEvent, payment: PaymentInitiatedEvent) {
        kafkaTemplate.executeInTransaction { template ->
            template.send(KafkaConfig.ORDER_EVENTS_TOPIC, orderId, order)
            template.send(KafkaConfig.PAYMENT_EVENTS_TOPIC, orderId, payment)
            // Both are published atomically — consumers see both or neither
        }
    }
}
```

```yaml
spring:
  kafka:
    producer:
      transaction-id-prefix: myapp-tx-  # Required for transactional producer
      enable-idempotence: true
```

> [!WARNING]
> **Transactional producers have significant performance overhead** (10-100ms per transaction due to coordinator overhead). Use them only when you genuinely need atomic multi-topic writes. For single-topic publishing, idempotent producers are sufficient. Overusing transactional producers at Kafka scale (millions of messages/sec) can be a significant bottleneck.

---

## 9. Running Kafka and RabbitMQ Locally

### Docker Compose for Development

```yaml
# docker-compose.yml
version: '3.8'

services:
  # RabbitMQ with management UI
  rabbitmq:
    image: rabbitmq:3.13-management
    ports:
      - "5672:5672"    # AMQP
      - "15672:15672"  # Management UI (http://localhost:15672, guest/guest)
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Kafka with KRaft (no ZooKeeper needed since Kafka 3.3)
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    volumes:
      - kafka_data:/var/lib/kafka/data

  # Kafka UI (optional but very useful)
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    depends_on:
      - kafka

volumes:
  rabbitmq_data:
  kafka_data:
```

---

## 10. Choosing Between RabbitMQ and Kafka — Production Decision Framework

```
Is message ordering per entity critical?
├── YES → Kafka (partition by entity ID ensures ordering)
└── NO → Either works

Do you need to replay historical events?
├── YES → Kafka (retention + offset management)
└── NO → Either works

Do you have multiple independent consumers for the same event?
├── YES → Kafka (consumer groups, each gets full copy)
│         RabbitMQ requires one queue per consumer (more complex setup)
└── NO → RabbitMQ simpler

Do you have complex routing requirements?
├── YES → RabbitMQ (Topic/Headers exchanges are more flexible)
└── NO → Either works

Is throughput > 1M messages/sec?
├── YES → Kafka
└── NO → Either works

Is your team small and ops overhead matters?
├── YES → RabbitMQ (simpler to operate, great management UI)
└── NO → Either works

Is this a task queue (process-and-forget, e.g., send email)?
├── YES → RabbitMQ (purpose-built for this pattern)
└── NO → Consider Kafka
```

### Hybrid Pattern — Use Both

Many mature systems use both:
- **RabbitMQ**: Task queues — email sending, SMS dispatch, PDF generation, short-lived jobs
- **Kafka**: Event streaming — order events, payment events, audit logs, analytics

This is the pattern used by many companies at scale. Don't feel you must choose one. They solve different problems.

---

## 11. Production Monitoring

### RabbitMQ Key Metrics

```
Queue Depth:     messages in queue (alert if > 10,000 for critical queues)
Consumer Count:  alert if 0 consumers on critical queue
Message Rate:    publish rate vs consume rate (alert if consume << publish)
DLQ Depth:       alert if DLQ has any messages (indicates processing failures)
Connection Count: alert on sudden drops (consumer crash)
```

### Kafka Key Metrics

```
Consumer Lag:    offset(latest) - offset(consumer) per partition
                 Alert if lag > N for critical consumer groups
Under-Replicated Partitions: alert if > 0 (replication degraded, data at risk)
Active Controller Count: alert if != 1
ISR (In-Sync Replicas): alert if ISR < expected replicas
Log End Offset Rate: message production rate
```

```kotlin
// Expose Kafka consumer lag as a Spring Boot Actuator metric
@Component
class KafkaLagMetrics(
    private val adminClient: AdminClient
) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("kafka.consumer.lag", this) { collector ->
            // Calculate total lag across all partitions for your consumer group
            val offsets = adminClient.listConsumerGroupOffsets("payment-service")
                .partitionsToOffsetAndMetadata().get()
            // (simplified — real implementation would be more detailed)
            offsets.values.sumOf { it.offset() }.toDouble()
        }.register(registry)
    }
}
```

---

## Summary

| Concern | Solution |
|---|---|
| Synchronous coupling | Async messaging (RabbitMQ or Kafka) |
| Simple task queues | RabbitMQ with Direct/Fanout exchange |
| Event sourcing / replay | Kafka with configurable retention |
| Dual-write atomicity | Outbox Pattern (DB + message table in same transaction) |
| Duplicate message delivery | Idempotency check (Redis SETNX or DB unique constraint) |
| Failed message handling | DLQ (RabbitMQ) / DLT (Kafka) with alerting |
| Exactly-once production | Idempotent producer (single topic) or transactional producer (multi-topic) |
| Ordered processing per entity | Kafka partition key = entity ID |
| Multiple independent consumers | Kafka consumer groups |

The event-driven architecture is not about using a fancy tool. It's about designing systems that degrade gracefully, scale horizontally, and never lose data — even when services crash at the worst possible moment.

---

*References: Apache Kafka Documentation, RabbitMQ Documentation, Spring AMQP Reference, Spring for Apache Kafka Reference, Pro Spring Boot 3 with Kotlin (Späth & Gutierrez, 2025), Designing Data-Intensive Applications (Kleppmann)*
## Book-Aligned Correction

The book covers message-oriented middleware through JMS, AMQP/RabbitMQ, WebSockets/STOMP, and RSocket. So the practical rule is:

- RabbitMQ/AMQP is excellent for queues, routing, and task/command-style async work.
- Kafka is excellent for durable event streams, replay, analytics, and high-throughput event pipelines.
- WebSocket/STOMP is for realtime client communication.
- RSocket is for bidirectional/reactive service communication.

Do not choose Kafka just because "scale." Choose by message semantics.
