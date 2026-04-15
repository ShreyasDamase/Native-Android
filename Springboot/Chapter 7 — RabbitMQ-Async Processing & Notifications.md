# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 7 — RabbitMQ: Async Processing & Notifications

### _Decoupling your app — so slow work never blocks fast responses_

---

## 7.1 The Problem This Chapter Solves

Imagine a user submits an interview. Your service saves it to the database. Now you need to send a notification to the admin: "New interview pending review."

The naive approach:

```kotlin
@Transactional
fun submit(dto: CreateInterviewDto, user: User): InterviewDetailDto {
    val interview = interviewRepository.save(toEntity(dto, user))

    // Send push notification to admin RIGHT NOW
    fcmService.sendToAdmin("New interview submitted", interview.title)
    // ↑ This calls Firebase's HTTP API
    // Firebase takes 200-800ms to respond
    // Your user is WAITING this entire time
    // If Firebase is down — your entire submit endpoint fails

    return interview.toDetailDto()
}
```

Three problems:

1. **Speed** — The user waits for Firebase to respond before getting their 201 Created
2. **Reliability** — If Firebase is temporarily down, the submit fails completely
3. **Coupling** — Your submit service now depends on Firebase, FCM, and notification logic

The correct solution: **do the slow work asynchronously**.

```kotlin
@Transactional
fun submit(dto: CreateInterviewDto, user: User): InterviewDetailDto {
    val interview = interviewRepository.save(toEntity(dto, user))

    // Publish a message to RabbitMQ — takes 1-2ms
    // "Someone else will send this notification. Not my problem anymore."
    rabbitTemplate.convertAndSend("notifications", NotificationEvent(...))

    return interview.toDetailDto()   // Returns immediately — user is not waiting
}

// Separately, a consumer picks up the message and sends the notification
// If Firebase is down, the message stays in the queue and is retried
// The submit endpoint does not care
```

RabbitMQ is the message broker in the middle. It receives messages, stores them reliably, and delivers them to consumers. Your API publishes and moves on. The consumer processes at its own pace.

---

## 7.2 Core Concepts — How RabbitMQ Thinks

Before writing code, understand the four objects you work with in RabbitMQ. Every piece of code in this chapter maps to one of these.

### Exchange

The entry point. When you publish a message, you publish it to an **exchange**. The exchange decides where the message goes next.

```
Your app publishes message → Exchange → routes to → Queue(s)
```

Four exchange types exist. HireStory uses two:

**Direct Exchange** — routes based on an exact routing key match. You publish with key `"interview.submitted"` → message goes to the queue bound with key `"interview.submitted"`. One-to-one routing.

**Topic Exchange** — routes based on a pattern match. You publish with key `"interview.submitted"` → goes to any queue bound with `"interview.*"` or `"#"`. One-to-many routing. Used for notifications.

### Queue

Where messages wait until a consumer picks them up. Messages are stored reliably — even if your consumer is offline, messages accumulate and are delivered when the consumer comes back online.

### Binding

The rule that connects an exchange to a queue. "Messages published to exchange X with routing key Y go to queue Z."

### Message

The actual data you send. In Spring Boot, you send a Kotlin object. Spring serialises it to JSON and deserialises it back on the consumer side.

### The Full Picture

```
Publisher (your API)
        ↓ publishes to
    Exchange (routes messages)
        ↓ based on routing key, sends to
      Queue (stores messages)
        ↓ delivers to
    Consumer (your background processor)
```

---

## 7.3 HireStory's Queue Architecture

Before writing code, design what queues you need and why:

```
Exchanges:
  hirestory.notifications   (topic)   — all notification events
  hirestory.crawler         (direct)  — crawl job processing

Queues:
  notification.queue        — FCM push notifications to users
  crawl.queue               — AI extraction for crawled content
  crawl.dlq                 — Dead letter queue: failed crawl jobs land here

Bindings:
  hirestory.notifications → notification.queue     (routing key: "#")
  hirestory.crawler       → crawl.queue            (routing key: "crawl.process")
  crawl.queue → crawl.dlq                          (on rejection after max retries)
```

### Dead Letter Queue

A dead letter queue (DLQ) catches messages that failed to process. Instead of losing failed messages, they land in the DLQ where you can inspect them, fix the problem, and replay them.

For HireStory: if the AI extraction fails 3 times, the crawl job message goes to `crawl.dlq`. You see it in the admin panel. You fix it manually or retrigger.

---

## 7.4 Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Spring AMQP — RabbitMQ support for Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    // For testing RabbitMQ
    testImplementation("org.springframework.amqp:spring-rabbit-test")
}
```

---

## 7.5 Configuration — application.yml

```yaml
# application.yml

spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /
    # Connection recovery — automatically reconnect if RabbitMQ restarts
    connection-timeout: 5000ms
    listener:
      simple:
        # How many messages to fetch at once per consumer
        # 1 = process one at a time — safe for heavy AI processing
        # Higher = more throughput — use for lightweight notification sending
        prefetch: 1
        # Acknowledge manually — message is only removed from queue after
        # your consumer explicitly acknowledges it (covered in Section 7.8)
        acknowledge-mode: manual
        # Retry failed messages before sending to DLQ
        retry:
          enabled: true
          initial-interval: 1000ms    # Wait 1s before first retry
          max-attempts: 3             # Try 3 times total
          multiplier: 2.0             # Double wait time each retry: 1s, 2s, 4s
```

---

## 7.6 RabbitMQ Configuration — Declaring Exchanges, Queues, Bindings

Spring Boot creates all exchanges, queues, and bindings automatically when the app starts — if they do not already exist.

```kotlin
// src/main/kotlin/com/example/hirestory/config/RabbitMQConfig.kt

package com.example.hirestory.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    // ── Exchange names ────────────────────────────────────────────────
    companion object {
        const val NOTIFICATIONS_EXCHANGE = "hirestory.notifications"
        const val CRAWLER_EXCHANGE       = "hirestory.crawler"

        // Queue names
        const val NOTIFICATION_QUEUE     = "notification.queue"
        const val CRAWL_QUEUE            = "crawl.queue"
        const val CRAWL_DLQ              = "crawl.dlq"

        // Routing keys
        const val CRAWL_ROUTING_KEY      = "crawl.process"
        const val CRAWL_DLQ_ROUTING_KEY  = "crawl.failed"
    }

    // ── Exchanges ─────────────────────────────────────────────────────

    // Topic exchange for notifications — pattern-based routing
    @Bean
    fun notificationsExchange(): TopicExchange =
        ExchangeBuilder.topicExchange(NOTIFICATIONS_EXCHANGE)
            .durable(true)    // Survives RabbitMQ restart
            .build()

    // Direct exchange for crawler — exact routing key match
    @Bean
    fun crawlerExchange(): DirectExchange =
        ExchangeBuilder.directExchange(CRAWLER_EXCHANGE)
            .durable(true)
            .build()

    // ── Queues ────────────────────────────────────────────────────────

    @Bean
    fun notificationQueue(): Queue =
        QueueBuilder.durable(NOTIFICATION_QUEUE)
            .build()

    @Bean
    fun crawlQueue(): Queue =
        QueueBuilder.durable(CRAWL_QUEUE)
            // Dead letter exchange — where failed messages go after max retries
            .deadLetterExchange(CRAWLER_EXCHANGE)
            .deadLetterRoutingKey(CRAWL_DLQ_ROUTING_KEY)
            // Message TTL — if not processed in 24 hours, send to DLQ
            .ttl(86_400_000)     // 24 hours in milliseconds
            .build()

    @Bean
    fun crawlDeadLetterQueue(): Queue =
        QueueBuilder.durable(CRAWL_DLQ)
            .build()

    // ── Bindings — connecting exchanges to queues ─────────────────────

    // All notifications (routing key "#" matches everything)
    @Bean
    fun notificationBinding(): Binding =
        BindingBuilder
            .bind(notificationQueue())
            .to(notificationsExchange())
            .with("#")    // "#" in topic exchange matches any routing key

    // Crawl jobs
    @Bean
    fun crawlBinding(): Binding =
        BindingBuilder
            .bind(crawlQueue())
            .to(crawlerExchange())
            .with(CRAWL_ROUTING_KEY)

    // Dead letter queue for failed crawl jobs
    @Bean
    fun crawlDlqBinding(): Binding =
        BindingBuilder
            .bind(crawlDeadLetterQueue())
            .to(crawlerExchange())
            .with(CRAWL_DLQ_ROUTING_KEY)

    // ── Message converter ─────────────────────────────────────────────
    // Tells Spring to serialise/deserialise messages as JSON
    // Without this, Spring uses Java serialisation — fragile and unreadable
    @Bean
    fun messageConverter(objectMapper: ObjectMapper): Jackson2JsonMessageConverter =
        Jackson2JsonMessageConverter(objectMapper)

    // ── RabbitTemplate — for publishing messages ──────────────────────
    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: Jackson2JsonMessageConverter
    ): RabbitTemplate = RabbitTemplate(connectionFactory).apply {
        this.messageConverter = messageConverter
        // Publisher confirms — know when RabbitMQ received your message
        isMandatory = true
    }
}
```

---

## 7.7 Message Classes — What You Send Through the Queue

Define the data each message carries. These are just Kotlin data classes:

```kotlin
// src/main/kotlin/com/example/hirestory/messaging/Messages.kt

package com.example.hirestory.messaging

import java.time.LocalDateTime

// ── Notification messages ─────────────────────────────────────────────

sealed class NotificationMessage {
    abstract val userId: Long
    abstract val timestamp: LocalDateTime
}

// Sent when an interview the user submitted gets published
data class InterviewPublishedMessage(
    override val userId: Long,
    val interviewId: Long,
    val interviewTitle: String,
    val companyName: String,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : NotificationMessage()

// Sent when someone comments on the user's interview
data class CommentReceivedMessage(
    override val userId: Long,
    val interviewId: Long,
    val interviewTitle: String,
    val commenterName: String,
    val commentPreview: String,    // First 100 chars of the comment
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : NotificationMessage()

// Sent when someone uses the user's referral code
data class ReferralUsedMessage(
    override val userId: Long,      // The referrer
    val referredUserName: String,
    val bonusReadsAdded: Int,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : NotificationMessage()

// Sent when a new interview is published for a company the user targets
data class NewInterviewForTargetCompanyMessage(
    override val userId: Long,
    val interviewId: Long,
    val companyName: String,
    val role: String,
    val outcome: String,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : NotificationMessage()

// Onboarding sequence messages
data class OnboardingMessage(
    override val userId: Long,
    val day: Int,                  // 1, 3, or 7
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : NotificationMessage()

// ── Crawler messages ──────────────────────────────────────────────────

data class CrawlJobMessage(
    val crawlJobId: Long,
    val sourceUrl: String,
    val rawText: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
```

---

## 7.8 The Notification Publisher — Sending Messages

```kotlin
// src/main/kotlin/com/example/hirestory/messaging/NotificationPublisher.kt

package com.example.hirestory.messaging

import com.example.hirestory.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class NotificationPublisher(private val rabbitTemplate: RabbitTemplate) {

    private val log = LoggerFactory.getLogger(NotificationPublisher::class.java)

    fun publishInterviewPublished(userId: Long, interviewId: Long, title: String, company: String) {
        publish(
            routingKey = "notification.interview.published",
            message = InterviewPublishedMessage(
                userId = userId,
                interviewId = interviewId,
                interviewTitle = title,
                companyName = company
            )
        )
    }

    fun publishCommentReceived(
        userId: Long,
        interviewId: Long,
        interviewTitle: String,
        commenterName: String,
        commentPreview: String
    ) {
        publish(
            routingKey = "notification.comment.received",
            message = CommentReceivedMessage(
                userId = userId,
                interviewId = interviewId,
                interviewTitle = interviewTitle,
                commenterName = commenterName,
                commentPreview = commentPreview.take(100)
            )
        )
    }

    fun publishReferralUsed(referrerId: Long, referredUserName: String, bonusReads: Int) {
        publish(
            routingKey = "notification.referral.used",
            message = ReferralUsedMessage(
                userId = referrerId,
                referredUserName = referredUserName,
                bonusReadsAdded = bonusReads
            )
        )
    }

    fun publishNewInterviewForTargetCompany(
        userId: Long,
        interviewId: Long,
        companyName: String,
        role: String,
        outcome: String
    ) {
        publish(
            routingKey = "notification.new.interview",
            message = NewInterviewForTargetCompanyMessage(
                userId = userId,
                interviewId = interviewId,
                companyName = companyName,
                role = role,
                outcome = outcome
            )
        )
    }

    fun publishOnboarding(userId: Long, day: Int) {
        publish(
            routingKey = "notification.onboarding",
            message = OnboardingMessage(userId = userId, day = day)
        )
    }

    // Generic publish — all notifications go to the same exchange
    // Different routing keys allow future routing to different queues
    private fun publish(routingKey: String, message: Any) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATIONS_EXCHANGE,
                routingKey,
                message
            )
            log.debug("Published message to {}: {}", routingKey, message::class.simpleName)
        } catch (e: Exception) {
            // Publishing failed — log but do not crash the caller
            // The user's action (submit, comment) already succeeded
            // The notification failure is secondary
            log.error("Failed to publish notification message {}: {}", routingKey, e.message, e)
        }
    }
}
```

> **💡 Why wrap publish in try-catch?** If RabbitMQ is temporarily down, you do not want the interview submission to fail. The interview save already succeeded. The notification is secondary. Log the failure — you can send the notification manually later. Never let the message queue failure cascade into a user-facing error.

---

## 7.9 The Crawl Publisher

```kotlin
// src/main/kotlin/com/example/hirestory/messaging/CrawlPublisher.kt

package com.example.hirestory.messaging

import com.example.hirestory.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class CrawlPublisher(private val rabbitTemplate: RabbitTemplate) {

    private val log = LoggerFactory.getLogger(CrawlPublisher::class.java)

    fun publishCrawlJob(crawlJobId: Long, sourceUrl: String, rawText: String) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.CRAWL_ROUTING_KEY,
                CrawlJobMessage(
                    crawlJobId = crawlJobId,
                    sourceUrl = sourceUrl,
                    rawText = rawText
                )
            )
            log.info("Queued crawl job {} for processing", crawlJobId)
        } catch (e: Exception) {
            log.error("Failed to queue crawl job {}: {}", crawlJobId, e.message, e)
            // Mark the job as failed in the database if we cannot queue it
        }
    }
}
```

---

## 7.10 The Notification Consumer — Processing Messages

The consumer listens to the queue and processes messages as they arrive. This runs in a background thread — completely separate from your API threads.

```kotlin
// src/main/kotlin/com/example/hirestory/messaging/NotificationConsumer.kt

package com.example.hirestory.messaging

import com.example.hirestory.config.RabbitMQConfig
import com.example.hirestory.service.FcmService
import com.example.hirestory.service.NotificationStorageService
import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class NotificationConsumer(
    private val fcmService: FcmService,
    private val notificationStorageService: NotificationStorageService
) {
    private val log = LoggerFactory.getLogger(NotificationConsumer::class.java)

    // @RabbitListener — this method is called whenever a message arrives in the queue
    // queues = the queue name to listen to
    // The message is deserialised from JSON back to the Kotlin object automatically
    @RabbitListener(
        queues = [RabbitMQConfig.NOTIFICATION_QUEUE],
        ackMode = "MANUAL"    // We acknowledge manually — gives full control
    )
    fun handleNotification(
        message: Map<String, Any>,    // Raw map — we inspect the type to dispatch
        channel: Channel,             // RabbitMQ channel — used for acknowledgement
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long,
        @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) routingKey: String
    ) {
        try {
            log.debug("Processing notification: routingKey={}", routingKey)

            // Dispatch based on routing key
            when {
                routingKey.contains("interview.published") ->
                    handleInterviewPublished(message)

                routingKey.contains("comment.received") ->
                    handleCommentReceived(message)

                routingKey.contains("referral.used") ->
                    handleReferralUsed(message)

                routingKey.contains("new.interview") ->
                    handleNewInterviewForTargetCompany(message)

                routingKey.contains("onboarding") ->
                    handleOnboarding(message)

                else -> log.warn("Unknown notification routing key: {}", routingKey)
            }

            // Acknowledge — message is removed from queue permanently
            channel.basicAck(deliveryTag, false)

        } catch (e: Exception) {
            log.error("Failed to process notification (deliveryTag={}): {}", deliveryTag, e.message, e)

            // Negative acknowledge with requeue=false
            // Message goes to DLQ (if configured) instead of being requeued endlessly
            channel.basicNack(deliveryTag, false, false)
        }
    }

    private fun handleInterviewPublished(message: Map<String, Any>) {
        val userId = (message["userId"] as Number).toLong()
        val interviewTitle = message["interviewTitle"] as String
        val companyName = message["companyName"] as String

        // 1. Store in notifications table
        notificationStorageService.store(
            userId = userId,
            type = "INTERVIEW_PUBLISHED",
            title = "Your interview was published!",
            body = "Your $companyName interview is now live.",
            deepLink = "/interview/${message["interviewId"]}"
        )

        // 2. Send push notification via FCM
        fcmService.sendToUser(
            userId = userId,
            title = "Your interview is live! 🎉",
            body = "\"$interviewTitle\" is now published on HireStory.",
            deepLink = "/interview/${message["interviewId"]}"
        )
    }

    private fun handleCommentReceived(message: Map<String, Any>) {
        val userId = (message["userId"] as Number).toLong()
        val commenterName = message["commenterName"] as String
        val commentPreview = message["commentPreview"] as String
        val interviewTitle = message["interviewTitle"] as String

        notificationStorageService.store(
            userId = userId,
            type = "COMMENT_RECEIVED",
            title = "$commenterName commented on your interview",
            body = "\"$commentPreview\"",
            deepLink = "/interview/${message["interviewId"]}#comments"
        )

        fcmService.sendToUser(
            userId = userId,
            title = "$commenterName commented on your interview",
            body = commentPreview,
            deepLink = "/interview/${message["interviewId"]}#comments"
        )
    }

    private fun handleReferralUsed(message: Map<String, Any>) {
        val userId = (message["userId"] as Number).toLong()
        val referredUserName = message["referredUserName"] as String
        val bonusReads = (message["bonusReadsAdded"] as Number).toInt()

        notificationStorageService.store(
            userId = userId,
            type = "REFERRAL_USED",
            title = "Your referral earned you $bonusReads bonus reads!",
            body = "$referredUserName joined using your referral code.",
            deepLink = "/profile/referrals"
        )

        fcmService.sendToUser(
            userId = userId,
            title = "Referral bonus unlocked! 🎁",
            body = "$referredUserName joined HireStory with your code. You earned $bonusReads extra reads.",
            deepLink = "/profile/referrals"
        )
    }

    private fun handleNewInterviewForTargetCompany(message: Map<String, Any>) {
        val userId = (message["userId"] as Number).toLong()
        val companyName = message["companyName"] as String
        val role = message["role"] as String
        val outcome = message["outcome"] as String

        notificationStorageService.store(
            userId = userId,
            type = "NEW_INTERVIEW_FOR_TARGET_COMPANY",
            title = "New $companyName interview added",
            body = "$role — ${outcome.lowercase().replaceFirstChar { it.uppercase() }}",
            deepLink = "/interview/${message["interviewId"]}"
        )

        fcmService.sendToUser(
            userId = userId,
            title = "New $companyName interview 🏢",
            body = "$role — $outcome",
            deepLink = "/interview/${message["interviewId"]}"
        )
    }

    private fun handleOnboarding(message: Map<String, Any>) {
        val userId = (message["userId"] as Number).toLong()
        val day = (message["day"] as Number).toInt()

        val (title, body) = when (day) {
            1 -> Pair(
                "Welcome to HireStory! 👋",
                "Start reading real interview experiences from top companies."
            )
            3 -> Pair(
                "Your feed is getting smarter",
                "The more you read, the more personalised your feed becomes."
            )
            7 -> Pair(
                "One week on HireStory!",
                "You have been preparing for 7 days. Keep going — interviews are winnable."
            )
            else -> return
        }

        notificationStorageService.store(
            userId = userId,
            type = "ONBOARDING",
            title = title,
            body = body,
            deepLink = "/feed"
        )

        fcmService.sendToUser(
            userId = userId,
            title = title,
            body = body,
            deepLink = "/feed"
        )
    }
}
```

---

## 7.11 Manual Acknowledgement — Why It Matters

In the consumer above, you use `ackMode = "MANUAL"`. This is critical.

```
Default (auto-ack):
Message arrives → Spring calls your method → Message DELETED from queue
If your method throws: message is gone forever. Data lost.

Manual ack:
Message arrives → Spring calls your method
If success → channel.basicAck() → Message DELETED from queue
If failure → channel.basicNack(requeue=false) → Message goes to DLQ
```

With manual acknowledgement:

- **Success path** — you process it, you acknowledge it, it is gone
- **Failure path** — you fail, you nack it, it goes to the dead letter queue where you can inspect it and replay it later

**Nothing is lost.** A notification that fails to send is in the DLQ. You can see it, understand why it failed, fix the issue, and republish it.

```kotlin
// Acknowledge — message processed successfully, remove from queue
channel.basicAck(deliveryTag, false)
// false = do not acknowledge multiple messages at once (just this one)

// Negative acknowledge — processing failed
channel.basicNack(
    deliveryTag,
    false,     // multiple = false — only this message
    false      // requeue = false — do not put back in original queue
               // With DLQ configured: goes to DLQ instead
               // Without DLQ: message is discarded
)

// Requeue — put back for immediate retry (use carefully — can cause infinite loops)
channel.basicNack(deliveryTag, false, true)
```

---

## 7.12 The FCM Service — Sending Push Notifications

Firebase Cloud Messaging delivers your notifications to Android and iOS.

Add the Firebase Admin SDK:

```kotlin
// build.gradle.kts
implementation("com.google.firebase:firebase-admin:9.4.1")
```

Initialize Firebase in your config:

```kotlin
// src/main/kotlin/com/example/hirestory/config/FirebaseConfig.kt

package com.example.hirestory.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.util.Base64

@Configuration
class FirebaseConfig {

    @Bean
    fun firebaseApp(): FirebaseApp {
        // Firebase service account key stored as base64 env variable
        // FIREBASE_SERVICE_ACCOUNT_KEY = base64(service-account.json content)
        val serviceAccountKey = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY")
            ?: throw IllegalStateException("FIREBASE_SERVICE_ACCOUNT_KEY not set")

        val keyBytes = Base64.getDecoder().decode(serviceAccountKey)
        val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(keyBytes))

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        return if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
    }
}
```

```kotlin
// src/main/kotlin/com/example/hirestory/service/FcmService.kt

package com.example.hirestory.service

import com.example.hirestory.repository.FcmTokenRepository
import com.google.firebase.messaging.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FcmService(private val fcmTokenRepository: FcmTokenRepository) {

    private val log = LoggerFactory.getLogger(FcmService::class.java)

    fun sendToUser(userId: Long, title: String, body: String, deepLink: String? = null) {
        // Look up all FCM tokens for this user
        // A user might have multiple devices (phone + tablet)
        val tokens = fcmTokenRepository.findTokensByUserId(userId)

        if (tokens.isEmpty()) {
            log.debug("No FCM tokens found for user {}", userId)
            return
        }

        tokens.forEach { token ->
            sendToToken(token, title, body, deepLink)
        }
    }

    private fun sendToToken(token: String, title: String, body: String, deepLink: String?) {
        try {
            val messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                // Data payload — your Android app reads this for navigation
                .putData("deepLink", deepLink ?: "")
                .putData("timestamp", System.currentTimeMillis().toString())
                .setAndroidConfig(
                    AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(
                            AndroidNotification.builder()
                                .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                .build()
                        )
                        .build()
                )
                .setApnsConfig(
                    ApnsConfig.builder()
                        .setAps(
                            Aps.builder()
                                .setSound("default")
                                .build()
                        )
                        .build()
                )

            val messageId = FirebaseMessaging.getInstance().send(messageBuilder.build())
            log.debug("FCM message sent: {}", messageId)

        } catch (e: FirebaseMessagingException) {
            when (e.messagingErrorCode) {
                // Token is invalid — remove it from database
                MessagingErrorCode.UNREGISTERED,
                MessagingErrorCode.INVALID_ARGUMENT -> {
                    log.info("Removing invalid FCM token: {}", token.take(20))
                    fcmTokenRepository.deleteByToken(token)
                }
                else -> log.error("FCM send failed for token {}: {}", token.take(20), e.message)
            }
        }
    }
}
```

---

## 7.13 Notification Storage Service

Every notification sent via FCM is also stored in your database. This powers the in-app notification list on the profile screen.

```kotlin
// src/main/kotlin/com/example/hirestory/service/NotificationStorageService.kt

package com.example.hirestory.service

import com.example.hirestory.entity.Notification
import com.example.hirestory.repository.NotificationRepository
import com.example.hirestory.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationStorageService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun store(
        userId: Long,
        type: String,
        title: String,
        body: String,
        deepLink: String? = null
    ) {
        val user = userRepository.findByIdOrNull(userId) ?: return

        notificationRepository.save(
            Notification(
                user = user,
                type = type,
                title = title,
                body = body,
                deepLink = deepLink
            )
        )
    }

    @Transactional(readOnly = true)
    fun getForUser(userId: Long, page: Int, size: Int): List<NotificationDto> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .content
            .map { NotificationDto(
                id = it.id!!,
                type = it.type,
                title = it.title,
                body = it.body,
                deepLink = it.deepLink,
                isRead = it.isRead,
                createdAt = it.createdAt.toString()
            )}
    }

    @Transactional
    fun markAsRead(notificationId: Long, userId: Long) {
        notificationRepository.markAsRead(notificationId, userId)
    }

    @Transactional
    fun markAllAsRead(userId: Long) {
        notificationRepository.markAllAsRead(userId)
    }

    fun getUnreadCount(userId: Long): Long {
        return notificationRepository.countByUserIdAndIsReadFalse(userId)
    }
}

data class NotificationDto(
    val id: Long,
    val type: String,
    val title: String,
    val body: String,
    val deepLink: String?,
    val isRead: Boolean,
    val createdAt: String
)
```

---

## 7.14 Wiring Publishers Into Your Services

Now connect the publishers to the places where events happen:

```kotlin
// AdminService — when an interview is approved
@Transactional
fun approve(id: Long): InterviewDetailDto {
    val interview = interviewRepository.findByIdOrNull(id)
        ?: throw ResourceNotFoundException("Interview", id)

    interviewRepository.updateStatus(id, InterviewStatus.PUBLISHED, LocalDateTime.now())
    companyRepository.incrementInterviewCount(interview.company.id!!)
    interviewService.evictFeedCache()

    // Notify the submitter — but only if it was user-submitted (not crawled)
    if (interview.user != null) {
        notificationPublisher.publishInterviewPublished(
            userId = interview.user.id!!,
            interviewId = interview.id!!,
            title = interview.title,
            company = interview.company.name
        )
    }

    // Also notify users who target this company
    // (done asynchronously in the consumer — explained below)
    notifyUsersTargetingCompany(interview)

    val updated = interviewRepository.findByIdOrNull(id)!!
    return updated.toDetailDto()
}

private fun notifyUsersTargetingCompany(interview: Interview) {
    // Find all users whose target company matches this interview's company
    val targetingUsers = userPreferencesRepository
        .findUsersTargetingCompany(interview.company.name)

    targetingUsers.forEach { userId ->
        notificationPublisher.publishNewInterviewForTargetCompany(
            userId = userId,
            interviewId = interview.id!!,
            companyName = interview.company.name,
            role = interview.role,
            outcome = interview.outcome.name
        )
    }
}
```

```kotlin
// CommentService — when a comment is posted
@Transactional
fun addComment(interviewId: Long, content: String, currentUser: User): CommentDto {
    val interview = interviewRepository.findByIdOrNull(interviewId)
        ?: throw ResourceNotFoundException("Interview", interviewId)

    val comment = commentRepository.save(
        Comment(interview = interview, user = currentUser, content = content)
    )

    // Notify the interview author — but not if they commented on their own interview
    val authorId = interview.user?.id
    if (authorId != null && authorId != currentUser.id) {
        notificationPublisher.publishCommentReceived(
            userId = authorId,
            interviewId = interviewId,
            interviewTitle = interview.title,
            commenterName = currentUser.name,
            commentPreview = content
        )
    }

    return comment.toDto()
}
```

```kotlin
// ReferralService — when a referral code is applied
@Transactional
fun applyReferralCode(code: String, currentUser: User) {
    if (currentUser.referralCode == code) {
        throw BusinessRuleException("You cannot use your own referral code")
    }

    val referrer = userRepository.findByReferralCode(code)
        ?: throw ResourceNotFoundException("Referral code", code)

    if (referralRepository.existsByReferred(currentUser)) {
        throw BusinessRuleException("You have already used a referral code")
    }

    referralRepository.save(Referral(referrer = referrer, referred = currentUser))

    // Add bonus reads to both users via Redis
    val bonusReads = 10
    cacheService.incrementReadCount(referrer.id!!, bonusReads)
    cacheService.incrementReadCount(currentUser.id!!, bonusReads)

    // Notify the referrer
    notificationPublisher.publishReferralUsed(
        referrerId = referrer.id!!,
        referredUserName = currentUser.name,
        bonusReads = bonusReads
    )
}
```

---

## 7.15 The Crawl Consumer — AI Extraction Preview

The crawl consumer picks up raw text from the crawler and sends it to the AI extraction service. Full AI implementation is in Chapter 9. The queue infrastructure is built here.

```kotlin
// src/main/kotlin/com/example/hirestory/messaging/CrawlConsumer.kt

package com.example.hirestory.messaging

import com.example.hirestory.config.RabbitMQConfig
import com.example.hirestory.entity.CrawlStatus
import com.example.hirestory.repository.CrawlJobRepository
import com.example.hirestory.service.AiExtractionService
import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.data.repository.findByIdOrNull
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CrawlConsumer(
    private val crawlJobRepository: CrawlJobRepository,
    private val aiExtractionService: AiExtractionService   // Chapter 9
) {
    private val log = LoggerFactory.getLogger(CrawlConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.CRAWL_QUEUE], ackMode = "MANUAL")
    fun processCrawlJob(
        message: CrawlJobMessage,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long
    ) {
        val jobId = message.crawlJobId
        log.info("Processing crawl job {}: {}", jobId, message.sourceUrl)

        val crawlJob = crawlJobRepository.findByIdOrNull(jobId)
        if (crawlJob == null) {
            log.warn("Crawl job {} not found in database — acking to clear queue", jobId)
            channel.basicAck(deliveryTag, false)
            return
        }

        try {
            // Mark as processing
            crawlJob.status = CrawlStatus.PROCESSING
            crawlJobRepository.save(crawlJob)

            // Send to AI for extraction — Chapter 9 implements this
            val extractedInterview = aiExtractionService.extract(
                rawText = message.rawText,
                sourceUrl = message.sourceUrl
            )

            // Update the crawl job with results
            crawlJob.status = CrawlStatus.DONE
            crawlJob.confidenceScore = extractedInterview.confidenceScore
            crawlJob.processedAt = LocalDateTime.now()
            crawlJobRepository.save(crawlJob)

            log.info("Crawl job {} processed successfully. Confidence: {}",
                jobId, extractedInterview.confidenceScore)

            channel.basicAck(deliveryTag, false)

        } catch (e: Exception) {
            log.error("Crawl job {} failed: {}", jobId, e.message, e)

            // Update error in database
            crawlJob.status = CrawlStatus.FAILED
            crawlJob.errorMessage = e.message?.take(500)
            crawlJob.processedAt = LocalDateTime.now()
            crawlJobRepository.save(crawlJob)

            // Nack — goes to DLQ after max retries
            channel.basicNack(deliveryTag, false, false)
        }
    }
}
```

---

## 7.16 The Onboarding Scheduler

The onboarding notification sequence runs on a daily schedule. It finds users at day 1, 3, and 7 of their journey and publishes onboarding messages to the queue.

```kotlin
// src/main/kotlin/com/example/hirestory/service/OnboardingScheduler.kt

package com.example.hirestory.service

import com.example.hirestory.messaging.NotificationPublisher
import com.example.hirestory.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class OnboardingScheduler(
    private val userRepository: UserRepository,
    private val notificationPublisher: NotificationPublisher
) {
    private val log = LoggerFactory.getLogger(OnboardingScheduler::class.java)

    // Run every day at 10:00 AM
    @Scheduled(cron = "0 0 10 * * *")
    fun sendOnboardingNotifications() {
        log.info("Running onboarding notification scheduler")

        val now = LocalDateTime.now()
        var sent = 0

        listOf(1, 3, 7).forEach { day ->
            val start = now.minusDays(day.toLong()).withHour(0).withMinute(0).withSecond(0)
            val end = start.plusDays(1)

            val users = userRepository.findUsersCreatedBetween(start, end)
            users.forEach { user ->
                notificationPublisher.publishOnboarding(user.id!!, day)
                sent++
            }

            log.info("Queued day-{} onboarding for {} users", day, users.size)
        }

        log.info("Onboarding scheduler complete — queued {} notifications", sent)
    }
}
```

Add the repository query to support this:

```kotlin
// UserRepository addition
@Query("""
    SELECT u FROM User u
    WHERE u.createdAt >= :start AND u.createdAt < :end
""")
fun findUsersCreatedBetween(
    @Param("start") start: LocalDateTime,
    @Param("end") end: LocalDateTime
): List<User>
```

---

## 7.17 The Notification Controller

```kotlin
// src/main/kotlin/com/example/hirestory/controller/NotificationController.kt

package com.example.hirestory.controller

import com.example.hirestory.entity.User
import com.example.hirestory.service.FcmTokenService
import com.example.hirestory.service.NotificationStorageService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationStorageService: NotificationStorageService,
    private val fcmTokenService: FcmTokenService
) {

    // GET /api/notifications
    @GetMapping
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal currentUser: User
    ): ResponseEntity<NotificationsResponse> {
        val notifications = notificationStorageService.getForUser(currentUser.id!!, page, size)
        val unreadCount = notificationStorageService.getUnreadCount(currentUser.id!!)
        return ResponseEntity.ok(NotificationsResponse(notifications, unreadCount))
    }

    // PUT /api/notifications/{id}/read
    @PutMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: Long,
        @AuthenticationPrincipal currentUser: User
    ): ResponseEntity<Void> {
        notificationStorageService.markAsRead(id, currentUser.id!!)
        return ResponseEntity.noContent().build()
    }

    // PUT /api/notifications/read-all
    @PutMapping("/read-all")
    fun markAllAsRead(
        @AuthenticationPrincipal currentUser: User
    ): ResponseEntity<Void> {
        notificationStorageService.markAllAsRead(currentUser.id!!)
        return ResponseEntity.noContent().build()
    }

    // POST /api/notifications/token — register FCM device token
    @PostMapping("/token")
    fun registerToken(
        @RequestBody @Valid dto: RegisterTokenDto,
        @AuthenticationPrincipal currentUser: User
    ): ResponseEntity<Void> {
        fcmTokenService.register(currentUser, dto.token, dto.platform)
        return ResponseEntity.noContent().build()
    }
}

data class RegisterTokenDto(
    @field:NotBlank val token: String,
    @field:NotBlank val platform: String   // "ANDROID" or "IOS"
)

data class NotificationsResponse(
    val notifications: List<NotificationDto>,
    val unreadCount: Long
)
```

---

## 7.18 Common Mistakes in Chapter 7

### Mistake 1 — Using auto-ack and losing messages on failure

```kotlin
// ❌ Auto-ack: message is deleted the moment it is received
// If your method throws: message is gone forever
@RabbitListener(queues = ["notification.queue"])
fun handleNotification(message: Map<String, Any>) {
    fcmService.sendToUser(...)   // If this throws — message is lost
}

// ✅ Manual ack: message only deleted after explicit acknowledgement
@RabbitListener(queues = ["notification.queue"], ackMode = "MANUAL")
fun handleNotification(message: Map<String, Any>, channel: Channel,
    @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long) {
    try {
        fcmService.sendToUser(...)
        channel.basicAck(deliveryTag, false)      // Success — remove from queue
    } catch (e: Exception) {
        channel.basicNack(deliveryTag, false, false)  // Failure — to DLQ
    }
}
```

### Mistake 2 — Crashing the publisher when RabbitMQ is down

```kotlin
// ❌ If RabbitMQ is down, the interview submission fails entirely
@Transactional
fun submit(dto: CreateInterviewDto): InterviewDetailDto {
    val interview = interviewRepository.save(toEntity(dto))
    rabbitTemplate.convertAndSend(...)   // Throws if RabbitMQ is unavailable
    return interview.toDetailDto()
}

// ✅ Catch publishing failures — the core operation already succeeded
@Transactional
fun submit(dto: CreateInterviewDto): InterviewDetailDto {
    val interview = interviewRepository.save(toEntity(dto))
    try {
        rabbitTemplate.convertAndSend(...)
    } catch (e: Exception) {
        log.error("Failed to publish notification after submit: {}", e.message)
        // Interview is saved. Notification lost. Acceptable trade-off.
    }
    return interview.toDetailDto()
}
```

### Mistake 3 — Requeuing on every failure — infinite loops

```kotlin
// ❌ requeue=true causes the same failing message to loop forever
channel.basicNack(deliveryTag, false, true)

// ✅ requeue=false sends to DLQ after max retries
// Combined with retry config in application.yml: 3 retries then DLQ
channel.basicNack(deliveryTag, false, false)
```

### Mistake 4 — Not configuring the message converter

```kotlin
// ❌ Without Jackson converter, Spring uses Java serialisation
// Messages are binary blobs — unreadable in RabbitMQ management UI
// Deserialisation breaks between app restarts or version changes

// ✅ Always configure Jackson2JsonMessageConverter
@Bean
fun rabbitTemplate(factory: ConnectionFactory, converter: Jackson2JsonMessageConverter): RabbitTemplate {
    return RabbitTemplate(factory).apply {
        messageConverter = converter
    }
}
```

### Mistake 5 — Non-durable queues losing messages on RabbitMQ restart

```kotlin
// ❌ Non-durable — all queued messages lost if RabbitMQ restarts
QueueBuilder.nonDurable(NOTIFICATION_QUEUE).build()

// ✅ Durable — messages survive RabbitMQ restarts
QueueBuilder.durable(NOTIFICATION_QUEUE).build()
```

---

## 7.19 HireStory Connection — What You Built in Chapter 7

By the end of Chapter 7, HireStory has a complete async processing backbone:

- `RabbitMQConfig` — two exchanges, three queues, bindings, DLQ for failed crawl jobs, Jackson JSON message converter
- `NotificationPublisher` — publishes 5 notification types with try-catch protection
- `CrawlPublisher` — queues crawl jobs for AI processing
- `NotificationConsumer` — processes all notification types with manual ack, stores in database, sends via FCM
- `CrawlConsumer` — processes crawl jobs, marks status, nacks to DLQ on failure
- `FcmService` — sends push notifications to Android and iOS, removes invalid tokens automatically
- `NotificationStorageService` — stores every notification in database for the in-app list
- `OnboardingScheduler` — daily job that finds users at day 1/3/7 and queues their notifications
- `NotificationController` — list, mark read, register FCM token

The impact on your API:

- Interview submission: no longer waits for Firebase — returns in milliseconds
- Comment posting: no longer waits for push delivery — returns immediately
- Referral apply: notification queued, user gets their response instantly
- Any Firebase outage: notifications queue up, delivered when Firebase recovers

---

## 7.20 Chapter Project — Build It Before You Move On

### What to build

Wire async notifications into your existing project.

**Step 1 — Start RabbitMQ locally**

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

RabbitMQ management UI: `http://localhost:15672` (guest/guest). You can see your queues, messages, and consumers here.

**Step 2 — Add the dependency and configure**

Add `spring-boot-starter-amqp` to Gradle. Add RabbitMQ config to `application.yml`.

**Step 3 — Write RabbitMQConfig**

Declare the notification exchange, notification queue, and the binding. Verify in the management UI that they are created on startup.

**Step 4 — Write a simple publisher and consumer**

Publish a test message from any service. Write a consumer that logs the message. Verify it appears in the consumer's logs.

**Step 5 — Wire into interview approval**

When `AdminService.approve()` runs, publish an `InterviewPublishedMessage`. Write the consumer that logs the notification (FCM implementation is optional).

**Step 6 — Test the dead letter queue**

Make your consumer throw an exception. After 3 retries, verify the message appears in the DLQ in the management UI.

### Checkpoint questions — answer before moving on

1. An interview is submitted. The service saves it to the database. Then it publishes a message to RabbitMQ. RabbitMQ is down. What should happen to the interview? What should happen to the notification? How does your code handle this?
    
2. Your notification consumer uses manual ack. It processes the notification successfully and calls `channel.basicAck()`. Then the method throws an exception after the ack. What happens to the message?
    
3. You have 1000 users who all target Google. A new Google interview is published. Your approval method calls `notifyUsersTargetingCompany()` which loops through 1000 users and publishes 1000 messages. What is the problem with this happening inside `@Transactional`?
    
4. The `crawl.queue` has a DLQ configured. A crawl job fails 3 times. Where does the message go? How do you inspect it? How do you replay it once you fix the problem?
    
5. Your `NotificationConsumer` method receives a `Map<String, Any>`. Why not receive the specific message class directly (e.g. `InterviewPublishedMessage`)? What problem does that cause?
    

---

_Chapter 8 → The Web Crawler — Filling HireStory With Content Automatically_

---

> **Book Progress:** Chapter 7 of 15 complete. Chapters ahead: Web Crawler · Spring AI · Scheduled Jobs · Testing · Deployment