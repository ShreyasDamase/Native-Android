# P0-11 — Payment Architecture: Razorpay/Cashfree, Webhooks, Idempotency, Refunds

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Production Truth**: Payment flows are where your business loses money, not just users. A double-charge ruins a customer relationship permanently. A missed payment confirmation means you delivered a product for free. A failed webhook handler means orders get stuck in limbo at 3am. This chapter is about making money movement reliable in a distributed, failure-prone world.

---

## Why Payments Are the Hardest Engineering Problem

Payments sit at the intersection of:
- **Distributed systems** (network partitions, timeouts, partial failures)
- **Financial correctness** (money cannot be created or destroyed)
- **Third-party dependency** (Razorpay/Cashfree are outside your control)
- **Regulatory compliance** (PCI DSS, RBI regulations)
- **User trust** (one bad experience = permanent churn)

The fundamental challenge: **you cannot rely on a single synchronous API call to confirm a payment**. The Razorpay API might time out after 30 seconds — did the payment go through or not? The user's mobile app might crash after payment — did they pay? Your server might crash between receiving confirmation and writing to DB — what's the state?

These scenarios happen thousands of times per day at Zepto/Blinkit scale. Your architecture must handle all of them correctly.

---

## The Complete Payment Flow

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Payment Flow Overview                        │
└─────────────────────────────────────────────────────────────────┘

1. [Mobile App]          → POST /api/v1/orders
                            Body: {items, address, walletAmount}
                            
2. [Order Service]       → Creates order with status: INITIATED
                         → POST /api/v1/payments/create-order
                         
3. [Payment Service]     → Creates Razorpay order (razorpay_order_id)
                         → Returns {razorpay_order_id, amount, key} to mobile
                         → Saves payment record: PAYMENT_PENDING
                         
4. [Mobile App]          → Opens Razorpay SDK with razorpay_order_id
                         → User completes payment (UPI/card/netbanking)
                         → Razorpay returns {payment_id, signature} to mobile
                         
5. [Mobile App]          → POST /api/v1/payments/verify
                            Body: {razorpay_order_id, razorpay_payment_id, razorpay_signature}
                            
6. [Payment Service]     → Verifies HMAC signature
                         → Updates payment: PAYMENT_CONFIRMED
                         → Publishes PaymentConfirmed event to Kafka
                         
7. [Order Service]       → Consumes PaymentConfirmed event
                         → Updates order: CONFIRMED
                         → Triggers fulfillment
                         
8. [Razorpay Webhook]    → POST /api/v1/webhooks/razorpay (async, from Razorpay servers)
                            Event: payment.captured
                         → SECONDARY confirmation (backup to step 6)
```

### Why Both Step 6 AND Step 8?

This redundancy is intentional and critical:

- **Step 6 (client-side verification)**: Fast, synchronous — user gets confirmation immediately
- **Step 8 (webhook)**: Asynchronous, delayed, but authoritative — Razorpay's backend confirms capture

If the mobile app crashes after Razorpay processes payment but before step 5, the user paid but your backend doesn't know. The webhook (step 8) saves you — it arrives minutes later and confirms the payment.

**This is why webhook handling is arguably more important than your synchronous verification**.

---

## Order State Machine

```
INITIATED ──────────────────────────────────────────────────────────────────┐
    │                                                                       │
    │ Payment created                                                        │
    ▼                                                                       │
PAYMENT_PENDING ─────────────────────────────────────────────────────────── │
    │                │                │                                     │
    │ Payment        │ Payment        │ Payment                             │
    │ verified       │ failed         │ timed out                           │
    │ (step 6)       │                │                                     │
    ▼                ▼                ▼                                     │
PAYMENT_CONFIRMED  PAYMENT_FAILED  PAYMENT_TIMEOUT ────────── User retries─┘
    │
    │ Fulfillment confirmed
    ▼
ORDER_CONFIRMED
    │
    │ Out for delivery
    ▼
DISPATCHED
    │
    │ Delivered
    ▼
DELIVERED ─── Refund initiated ──→ REFUND_INITIATED ──→ REFUND_COMPLETED
                                                       └──→ REFUND_FAILED
```

```kotlin
// src/main/kotlin/com/yourapp/payment/domain/PaymentStatus.kt
package com.yourapp.payment.domain

enum class PaymentStatus {
    INITIATED,          // Payment record created, awaiting user action
    PAYMENT_PENDING,    // Razorpay order created, user in payment flow
    PAYMENT_CONFIRMED,  // Payment verified (either by signature or webhook)
    PAYMENT_FAILED,     // Payment explicitly failed
    PAYMENT_EXPIRED,    // Payment session expired (15min timeout)
    REFUND_INITIATED,   // Refund requested to gateway
    REFUND_PROCESSING,  // Gateway processing refund
    REFUND_COMPLETED,   // Refund credited back to user
    REFUND_FAILED       // Refund failed (rare — needs manual intervention)
}
```

---

## Idempotency Key Pattern

### Why Every Payment Call Must Be Idempotent

Consider this failure scenario:
1. Your server sends a capture request to Razorpay
2. Razorpay processes the capture (charges the user)
3. Network connection drops before Razorpay's response reaches your server
4. Your server gets a `ConnectionTimeoutException`
5. Your retry logic retries the capture request
6. **User is charged twice**

The idempotency key prevents this. You send a unique key with each request. If Razorpay receives the same key twice, it returns the **same result as the first request** without processing it again.

```
Request 1: POST /capture {idempotency_key: "ord_7x9Pk2_capture_1", amount: 50000}
           → Razorpay charges user ₹500. Response: {status: "captured"}
           → [Network drops before response reaches you]

Request 2 (retry): POST /capture {idempotency_key: "ord_7x9Pk2_capture_1", amount: 50000}
           → Razorpay sees same idempotency key
           → Returns cached response: {status: "captured"} (no second charge)
```

> [!IMPORTANT]
> Idempotency keys must be **unique per operation attempt**, not per order. The key for capturing order `ord_7x9Pk2` should include the order ID + operation type. If a user retries payment for the same order after a failure (genuinely trying again), use a DIFFERENT key — this is a new payment attempt.

```kotlin
// src/main/kotlin/com/yourapp/payment/service/IdempotencyService.kt
package com.yourapp.payment.service

import com.yourapp.payment.repository.IdempotencyKeyRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class IdempotencyService(
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    private val redisTemplate: org.springframework.data.redis.core.StringRedisTemplate
) {

    /**
     * Generates a stable idempotency key for a given operation.
     * The key is deterministic — same inputs = same key.
     * This allows safe retries without generating new keys.
     */
    fun generateKey(orderId: String, operation: String, attempt: Int = 1): String {
        return "idem_${orderId}_${operation}_${attempt}"
    }

    /**
     * Checks if we've already processed this idempotency key.
     * Returns the cached response if so, null if this is a new operation.
     *
     * Uses Redis for fast lookup with automatic expiry.
     * TTL: 24 hours (payment operations don't need longer than this)
     */
    fun <T> executeIdempotently(
        key: String,
        responseClass: Class<T>,
        operation: () -> T
    ): T {
        val cacheKey = "idempotency:$key"

        // Check Redis first (sub-millisecond lookup)
        val cachedResult = redisTemplate.opsForValue().get(cacheKey)
        if (cachedResult != null) {
            // Already processed — return cached response
            return com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(cachedResult, responseClass)
        }

        // Execute the operation
        val result = operation()

        // Cache the result with 24-hour expiry
        val serialized = com.fasterxml.jackson.databind.ObjectMapper()
            .writeValueAsString(result)
        redisTemplate.opsForValue().set(
            cacheKey,
            serialized,
            java.time.Duration.ofHours(24)
        )

        return result
    }
}
```

```sql
-- If you want DB-level idempotency (more durable than Redis-only)
CREATE TABLE payment_idempotency_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    operation       VARCHAR(100) NOT NULL,
    order_id        VARCHAR(100) NOT NULL,
    response_body   JSONB,
    http_status     INT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW() + INTERVAL '24 hours'
);

CREATE INDEX idx_idempotency_key ON payment_idempotency_keys(idempotency_key);
CREATE INDEX idx_idempotency_expires ON payment_idempotency_keys(expires_at);

-- Cleanup job: delete expired keys
DELETE FROM payment_idempotency_keys WHERE expires_at < NOW();
```

---

## Webhook Security: HMAC Signature Verification

### Why You Cannot Trust Any Webhook Without Verification

A webhook is an HTTP POST request sent to your server from the internet. Without verification, **anyone can send fake payment confirmation webhooks to your server**. An attacker could:
1. Send a fake `payment.captured` webhook for any order
2. Your backend marks it as paid
3. Order gets fulfilled
4. No money ever changed hands

HMAC (Hash-based Message Authentication Code) signature verification prevents this. Razorpay signs every webhook payload with your webhook secret. You verify the signature before trusting the payload.

```
[Razorpay Server] → HMAC-SHA256(payload, webhook_secret) → signature header
[Your Server]     → Verify: HMAC-SHA256(received_payload, webhook_secret) == signature header
```

If the payload was tampered with in transit, or if the request wasn't from Razorpay, the signatures won't match.

```kotlin
// src/main/kotlin/com/yourapp/payment/controller/WebhookController.kt
package com.yourapp.payment.controller

import com.yourapp.payment.service.PaymentWebhookService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestController
@RequestMapping("/api/v1/webhooks")
class WebhookController(
    private val paymentWebhookService: PaymentWebhookService,
    @Value("\${razorpay.webhook.secret}") private val webhookSecret: String
) {

    private val log = LoggerFactory.getLogger(WebhookController::class.java)

    @PostMapping("/razorpay")
    fun handleRazorpayWebhook(
        @RequestBody payload: String,  // Raw string — DO NOT use @RequestBody with parsed DTO
        @RequestHeader("X-Razorpay-Signature") signature: String,
        @RequestHeader("X-Razorpay-Event-Id", required = false) eventId: String?,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, String>> {

        MDC.put("webhookEventId", eventId ?: "unknown")
        MDC.put("webhookSource", "razorpay")

        // ─── STEP 1: Verify HMAC signature ────────────────────────────────────────
        // This MUST happen BEFORE parsing the payload or doing ANY processing
        if (!verifyWebhookSignature(payload, signature)) {
            log.warn("Webhook signature verification FAILED - possible tampering or misconfiguration")
            // Return 400 — do NOT return 200 for invalid signatures
            // Also don't return 401 — that would tell attackers to try different auth
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Invalid signature"))
        }

        log.info("Webhook received and signature verified")

        // ─── STEP 2: Parse the event ────────────────────────────────────────────────
        val event = try {
            com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(payload, RazorpayWebhookEvent::class.java)
        } catch (e: Exception) {
            log.error("Failed to parse webhook payload", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Invalid payload format"))
        }

        // ─── STEP 3: Process idempotently ─────────────────────────────────────────
        // Razorpay retries webhooks multiple times — your handler MUST be idempotent
        // If the same event arrives twice, processing it twice must be safe
        try {
            paymentWebhookService.processWebhookEvent(event, eventId)
        } catch (e: Exception) {
            log.error("Webhook processing failed - event={} error={}", event.event, e.message, e)
            // IMPORTANT: Return 500 so Razorpay retries the webhook
            // Return 200 ONLY when you've successfully processed (or determined it's a duplicate)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Processing failed, please retry"))
        }

        // Return 200 to acknowledge receipt and stop Razorpay's retry loop
        return ResponseEntity.ok(mapOf("status" to "processed"))
    }

    /**
     * Verifies Razorpay webhook signature using HMAC-SHA256.
     *
     * Razorpay computes: HMAC_SHA256(raw_payload, webhook_secret)
     * We compute the same and compare. If they match, the webhook is authentic.
     */
    private fun verifyWebhookSignature(payload: String, receivedSignature: String): Boolean {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(webhookSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(secretKey)
            val computedSignature = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            val computedHex = computedSignature.joinToString("") { "%02x".format(it) }

            // Constant-time comparison to prevent timing attacks
            // Regular == comparison would leak information about how many characters match
            org.springframework.security.crypto.codec.Hex.decode(computedHex)
                .contentEquals(org.springframework.security.crypto.codec.Hex.decode(receivedSignature))
        } catch (e: Exception) {
            log.error("Error verifying webhook signature", e)
            false
        }
    }
}
```

> [!CAUTION]
> **Never use `==` for signature comparison** — use a constant-time comparison function. Regular string equality short-circuits on the first differing character. A timing attack can measure how long comparison takes to determine how many characters of the signature match, eventually reconstructing the valid signature character by character. Spring Security's `MessageDigest.isEqual()` or Kotlin's `contentEquals()` on byte arrays provides constant-time comparison.

```kotlin
// src/main/kotlin/com/yourapp/payment/domain/RazorpayWebhookEvent.kt
package com.yourapp.payment.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class RazorpayWebhookEvent(
    val entity: String,         // "event"
    val event: String,          // "payment.captured", "payment.failed", "refund.created"
    @JsonProperty("payload")
    val payload: WebhookPayload,
    @JsonProperty("created_at")
    val createdAt: Long         // Unix timestamp
)

data class WebhookPayload(
    val payment: PaymentEntity? = null,
    val refund: RefundEntity? = null
)

data class PaymentEntity(
    val entity: RazorpayPayment
)

data class RazorpayPayment(
    val id: String,             // pay_xxxxxxxxxx
    @JsonProperty("order_id")
    val orderId: String,        // order_xxxxxxxxxx
    val amount: Long,           // Amount in paise (500 = ₹5)
    val currency: String,
    val status: String,         // "captured", "failed", "authorized"
    val method: String,         // "upi", "card", "netbanking", "wallet"
    @JsonProperty("error_code")
    val errorCode: String?,
    @JsonProperty("error_description")
    val errorDescription: String?,
    @JsonProperty("captured_at")
    val capturedAt: Long?
)
```

---

## Webhook Reliability and Idempotent Processing

### Razorpay's Retry Policy

Razorpay retries failed webhooks (non-2xx response) with exponential backoff:
- Attempt 1: Immediate
- Attempt 2: 5 minutes later
- Attempt 3: 30 minutes later
- Attempt 4: 2 hours later
- Attempt 5: 24 hours later (final)

This means your webhook handler **will receive the same event multiple times**. Your code must be idempotent — processing the same event 5 times must have the same outcome as processing it once.

```kotlin
// src/main/kotlin/com/yourapp/payment/service/PaymentWebhookService.kt
package com.yourapp.payment.service

import com.yourapp.payment.domain.RazorpayWebhookEvent
import com.yourapp.payment.repository.PaymentRepository
import com.yourapp.payment.repository.WebhookEventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentWebhookService(
    private val paymentRepository: PaymentRepository,
    private val webhookEventRepository: WebhookEventRepository,
    private val orderEventPublisher: OrderEventPublisher
) {

    private val log = LoggerFactory.getLogger(PaymentWebhookService::class.java)

    @Transactional
    fun processWebhookEvent(event: RazorpayWebhookEvent, eventId: String?) {
        // ─── Idempotency Check ─────────────────────────────────────────────────────
        // Check if we've already processed this event ID
        // eventId is unique per webhook delivery attempt — same event, different retries = same eventId
        if (eventId != null && webhookEventRepository.existsByEventId(eventId)) {
            log.info("Webhook event already processed - eventId={} event={}", eventId, event.event)
            return  // Return normally — 200 to Razorpay to stop retrying
        }

        // Record this event as being processed BEFORE doing any work
        // This prevents concurrent duplicate processing
        eventId?.let {
            webhookEventRepository.save(WebhookEvent(
                eventId = it,
                eventType = event.event,
                rawPayload = event.toString(),
                processedAt = java.time.Instant.now()
            ))
        }

        // ─── Route to event-specific handler ─────────────────────────────────────
        when (event.event) {
            "payment.captured" -> handlePaymentCaptured(event)
            "payment.failed" -> handlePaymentFailed(event)
            "refund.processed" -> handleRefundProcessed(event)
            "payment.authorized" -> {
                // payment.authorized = payment authorized but NOT yet captured
                // Only relevant if you use manual capture flow
                log.info("Payment authorized but not captured - event={}", event)
            }
            else -> {
                log.warn("Unknown webhook event type: {}", event.event)
                // Still return 200 — unknown events shouldn't cause retries
            }
        }
    }

    private fun handlePaymentCaptured(event: RazorpayWebhookEvent) {
        val payment = event.payload.payment?.entity
            ?: throw IllegalStateException("payment.captured event missing payment payload")

        val razorpayOrderId = payment.orderId
        val razorpayPaymentId = payment.id

        log.info("Processing payment.captured - razorpayOrderId={} paymentId={}",
            razorpayOrderId, razorpayPaymentId)

        // Find our internal payment record by Razorpay order ID
        val internalPayment = paymentRepository.findByGatewayOrderId(razorpayOrderId)
            ?: run {
                log.error("Payment not found for razorpay order ID: {}", razorpayOrderId)
                throw IllegalStateException("Unknown order: $razorpayOrderId")
            }

        // Check if already confirmed (duplicate webhook)
        if (internalPayment.status == PaymentStatus.PAYMENT_CONFIRMED) {
            log.info("Payment already confirmed - skipping duplicate webhook processing")
            return
        }

        // Update payment status
        internalPayment.apply {
            status = PaymentStatus.PAYMENT_CONFIRMED
            gatewayPaymentId = razorpayPaymentId
            confirmedAt = java.time.Instant.now()
            paymentMethod = payment.method
        }
        paymentRepository.save(internalPayment)

        // Publish event so Order Service can proceed with fulfillment
        orderEventPublisher.publishPaymentConfirmed(
            orderId = internalPayment.orderId,
            paymentId = internalPayment.id.toString(),
            amount = payment.amount
        )

        log.info("Payment confirmed and order fulfillment triggered - orderId={}",
            internalPayment.orderId)
    }

    private fun handlePaymentFailed(event: RazorpayWebhookEvent) {
        val payment = event.payload.payment?.entity ?: return
        val razorpayOrderId = payment.orderId

        val internalPayment = paymentRepository.findByGatewayOrderId(razorpayOrderId) ?: return

        if (internalPayment.status == PaymentStatus.PAYMENT_FAILED) {
            return  // Already processed
        }

        internalPayment.apply {
            status = PaymentStatus.PAYMENT_FAILED
            failureReason = payment.errorDescription
            failedAt = java.time.Instant.now()
        }
        paymentRepository.save(internalPayment)

        log.info("Payment failed - orderId={} reason={}",
            internalPayment.orderId, payment.errorDescription)
    }
}
```

```sql
-- Webhook event deduplication table
CREATE TABLE webhook_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    VARCHAR(255) NOT NULL UNIQUE,  -- Razorpay's X-Razorpay-Event-Id header
    event_type  VARCHAR(100) NOT NULL,
    source      VARCHAR(50) NOT NULL DEFAULT 'razorpay',
    raw_payload TEXT,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_webhook_event_id ON webhook_events(event_id);
```

---

## Razorpay Implementation: Order Creation

```kotlin
// src/main/kotlin/com/yourapp/payment/service/RazorpayPaymentService.kt
package com.yourapp.payment.service

import com.razorpay.RazorpayClient
import com.razorpay.RazorpayException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RazorpayPaymentService(
    @Value("\${razorpay.key.id}") private val keyId: String,
    @Value("\${razorpay.key.secret}") private val keySecret: String
) {

    private val log = LoggerFactory.getLogger(RazorpayPaymentService::class.java)
    private val client = RazorpayClient(keyId, keySecret)

    /**
     * Creates a Razorpay order.
     *
     * IMPORTANT: Razorpay amount is in PAISE (smallest currency unit)
     * ₹500 = 50000 paise. NEVER work in rupees directly — floating point precision issues.
     */
    fun createOrder(
        amountInPaise: Long,
        currency: String = "INR",
        internalOrderId: String,
        notes: Map<String, String> = emptyMap()
    ): RazorpayOrderResponse {
        require(amountInPaise >= 100) { "Minimum amount is ₹1 (100 paise)" }
        require(amountInPaise <= 50_00_00_00_000L) { "Amount exceeds Razorpay maximum" }

        return try {
            val orderRequest = JSONObject().apply {
                put("amount", amountInPaise)
                put("currency", currency)
                put("receipt", internalOrderId)  // Your internal order ID (for reconciliation)
                put("notes", JSONObject(notes + mapOf(
                    "internal_order_id" to internalOrderId,
                    "source" to "app"
                )))
                // partial_payment: false (default) — user must pay full amount
                // For installments/partial payments, set to true
                put("partial_payment", false)
            }

            val razorpayOrder = client.orders.create(orderRequest)

            log.info("Razorpay order created - razorpayOrderId={} internalOrderId={}",
                razorpayOrder.get<String>("id"), internalOrderId)

            RazorpayOrderResponse(
                razorpayOrderId = razorpayOrder.get("id"),
                amount = razorpayOrder.get("amount"),
                currency = razorpayOrder.get("currency"),
                keyId = keyId  // Send key to mobile for SDK initialization
            )
        } catch (e: RazorpayException) {
            log.error("Razorpay order creation failed - internalOrderId={} error={}",
                internalOrderId, e.message)
            throw PaymentGatewayException("Failed to create payment order: ${e.message}", e)
        }
    }

    /**
     * Verifies payment signature after user completes payment.
     *
     * Razorpay computes: HMAC_SHA256(razorpay_order_id + "|" + razorpay_payment_id, key_secret)
     * You verify this to confirm the payment response came from Razorpay.
     */
    fun verifyPaymentSignature(
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String
    ): Boolean {
        return try {
            val attributes = JSONObject().apply {
                put("razorpay_order_id", razorpayOrderId)
                put("razorpay_payment_id", razorpayPaymentId)
                put("razorpay_signature", razorpaySignature)
            }
            com.razorpay.Utils.verifyPaymentSignature(attributes, keySecret)
            true
        } catch (e: RazorpayException) {
            log.warn("Payment signature verification failed - orderId={} paymentId={}",
                razorpayOrderId, razorpayPaymentId)
            false
        }
    }

    /**
     * Fetches payment details from Razorpay.
     * Used for reconciliation and manual verification.
     */
    fun fetchPayment(razorpayPaymentId: String): JSONObject {
        return client.payments.fetch(razorpayPaymentId).toJson()
    }
}
```

### Payment Controller: Verify and Confirm

```kotlin
// src/main/kotlin/com/yourapp/payment/controller/PaymentController.kt
package com.yourapp.payment.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentService: PaymentService,
    private val razorpayPaymentService: RazorpayPaymentService
) {

    @PostMapping("/create-order")
    fun createPaymentOrder(
        @RequestBody request: CreatePaymentOrderRequest,
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<CreatePaymentOrderResponse> {
        val response = paymentService.createPaymentOrder(
            orderId = request.orderId,
            userId = userId,
            amountInPaise = request.amountInPaise
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Called by mobile app after Razorpay SDK completes payment.
     * Verifies signature and marks payment as confirmed.
     *
     * This is the OPTIMISTIC confirmation — webhook provides authoritative confirmation.
     */
    @PostMapping("/verify")
    fun verifyPayment(
        @RequestBody request: PaymentVerificationRequest,
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<PaymentVerificationResponse> {
        // 1. Verify HMAC signature
        val signatureValid = razorpayPaymentService.verifyPaymentSignature(
            razorpayOrderId = request.razorpayOrderId,
            razorpayPaymentId = request.razorpayPaymentId,
            razorpaySignature = request.razorpaySignature
        )

        if (!signatureValid) {
            return ResponseEntity.status(400)
                .body(PaymentVerificationResponse(success = false, message = "Invalid payment signature"))
        }

        // 2. Mark payment as confirmed in our DB
        paymentService.confirmPayment(
            razorpayOrderId = request.razorpayOrderId,
            razorpayPaymentId = request.razorpayPaymentId,
            userId = userId
        )

        return ResponseEntity.ok(
            PaymentVerificationResponse(success = true, message = "Payment confirmed")
        )
    }
}

data class CreatePaymentOrderRequest(
    val orderId: String,
    val amountInPaise: Long
)

data class PaymentVerificationRequest(
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String
)

data class PaymentVerificationResponse(
    val success: Boolean,
    val message: String
)
```

---

## The Dual-Write Problem: Atomic Payment + Order Update

One of the hardest problems in payment systems: you need to mark the payment as CONFIRMED in your DB AND notify the Order Service atomically. If one succeeds and the other fails, you have data inconsistency.

**Wrong approach**: Call Order Service API synchronously after DB update
```
1. Update payment status = CONFIRMED in DB  ✅
2. POST /api/v1/orders/{id}/confirm  ← Network failure here
→ Payment confirmed, order still PENDING — stuck state
```

**Correct approach**: Transactional Outbox Pattern

```
1. Start DB transaction
2. Update payment status = CONFIRMED
3. Insert event into outbox table: {event: PaymentConfirmed, orderId: xyz}
4. Commit transaction atomically
↓
5. Background job reads outbox table
6. Publishes event to Kafka
7. Marks outbox entry as published
```

```sql
-- Transactional Outbox Table
CREATE TABLE payment_outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100) NOT NULL,  -- "PAYMENT_CONFIRMED", "PAYMENT_FAILED"
    payload         JSONB NOT NULL,
    order_id        VARCHAR(100) NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, PUBLISHED, FAILED
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    published_at    TIMESTAMP WITH TIME ZONE,
    retry_count     INT DEFAULT 0,
    next_retry_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_outbox_pending ON payment_outbox(status, next_retry_at)
    WHERE status = 'PENDING';
```

```kotlin
// src/main/kotlin/com/yourapp/payment/service/PaymentService.kt
@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: PaymentOutboxRepository
) {

    @Transactional  // Both operations happen in ONE database transaction
    fun confirmPayment(razorpayOrderId: String, razorpayPaymentId: String, userId: String) {
        val payment = paymentRepository.findByGatewayOrderId(razorpayOrderId)
            ?: throw PaymentNotFoundException("Payment not found: $razorpayOrderId")

        // Idempotency: if already confirmed, do nothing
        if (payment.status == PaymentStatus.PAYMENT_CONFIRMED) {
            return
        }

        // 1. Update payment record
        payment.status = PaymentStatus.PAYMENT_CONFIRMED
        payment.gatewayPaymentId = razorpayPaymentId
        payment.confirmedAt = java.time.Instant.now()
        paymentRepository.save(payment)

        // 2. Write event to outbox (same transaction — atomic!)
        outboxRepository.save(PaymentOutbox(
            eventType = "PAYMENT_CONFIRMED",
            orderId = payment.orderId,
            payload = mapOf(
                "orderId" to payment.orderId,
                "paymentId" to payment.id.toString(),
                "amount" to payment.amount,
                "confirmedAt" to payment.confirmedAt.toString()
            )
        ))

        // Transaction commits — both writes are durably committed together
        // No half-states possible
    }
}
```

---

## Refund Flow

### Refund Architecture

```
User requests refund
    │
    ▼
[Your backend] validates refund eligibility
    │
    ├── Within refund window? (e.g., 7 days)
    ├── Order in refundable state?
    └── Amount ≤ original payment amount?
    │
    ▼ (eligible)
POST Razorpay Refund API
    │
    ▼
Razorpay initiates refund
    │ (takes 5-7 business days for card, instant for UPI)
    ▼
Razorpay sends refund.processed webhook
    │
    ▼
Update refund status = COMPLETED
Wallet credit (if refund to wallet)
Notify user
```

```kotlin
// src/main/kotlin/com/yourapp/payment/service/RefundService.kt
@Service
class RefundService(
    private val razorpayClient: RazorpayClient,
    private val refundRepository: RefundRepository,
    private val paymentRepository: PaymentRepository
) {

    private val log = LoggerFactory.getLogger(RefundService::class.java)

    @Transactional
    fun initiateRefund(
        orderId: String,
        refundAmountInPaise: Long,
        reason: RefundReason,
        requestedBy: String
    ): RefundResult {
        val payment = paymentRepository.findByOrderId(orderId)
            ?: throw PaymentNotFoundException("No payment found for order: $orderId")

        // Validation
        require(payment.status == PaymentStatus.PAYMENT_CONFIRMED) {
            "Can only refund confirmed payments, current status: ${payment.status}"
        }
        require(refundAmountInPaise <= payment.amount) {
            "Refund amount ($refundAmountInPaise) exceeds payment amount (${payment.amount})"
        }
        require(refundAmountInPaise > 0) { "Refund amount must be positive" }

        // Check for duplicate refund (idempotency)
        val existingRefund = refundRepository.findByOrderIdAndStatus(
            orderId, RefundStatus.REFUND_COMPLETED
        )
        if (existingRefund != null && existingRefund.amount == refundAmountInPaise) {
            log.warn("Duplicate refund request detected - orderId={}", orderId)
            return RefundResult(refundId = existingRefund.id.toString(), status = "already_refunded")
        }

        // Create refund record BEFORE calling Razorpay
        // If Razorpay call succeeds but DB write fails, we have a problem
        // Better: create DB record first, then call Razorpay
        val refund = refundRepository.save(Refund(
            orderId = orderId,
            paymentId = payment.gatewayPaymentId!!,
            amount = refundAmountInPaise,
            status = RefundStatus.REFUND_INITIATED,
            reason = reason,
            requestedBy = requestedBy
        ))

        // Call Razorpay Refunds API
        return try {
            val refundRequest = org.json.JSONObject().apply {
                put("amount", refundAmountInPaise)
                put("speed", "normal")  // "normal" (5-7 days) or "optimum" (instant, higher cost)
                put("notes", org.json.JSONObject(mapOf(
                    "order_id" to orderId,
                    "reason" to reason.name,
                    "initiated_by" to requestedBy
                )))
                // Idempotency for refund API call
                put("receipt", "refund_${refund.id}")
            }

            val razorpayRefund = razorpayClient.refunds.create(refundRequest)
            val gatewayRefundId = razorpayRefund.get<String>("id")

            refund.gatewayRefundId = gatewayRefundId
            refund.status = RefundStatus.REFUND_PROCESSING
            refundRepository.save(refund)

            log.info("Refund initiated - orderId={} refundId={} gatewayRefundId={}",
                orderId, refund.id, gatewayRefundId)

            RefundResult(refundId = refund.id.toString(), status = "processing")
        } catch (e: com.razorpay.RazorpayException) {
            refund.status = RefundStatus.REFUND_FAILED
            refund.failureReason = e.message
            refundRepository.save(refund)

            log.error("Razorpay refund initiation failed - orderId={} error={}", orderId, e.message)
            throw RefundInitiationException("Refund failed: ${e.message}", e)
        }
    }
}
```

---

## Daily Reconciliation: Gateway vs Your DB

### Why Reconciliation is Mandatory

Even with perfect webhook handling and signature verification, discrepancies happen:
- Webhook delivery failed and all retries exhausted
- Bug in your webhook handler marked payment failed when it succeeded
- Database connection issue during payment confirmation

Reconciliation is your safety net — a daily job that compares your DB with Razorpay's settlement reports.

```kotlin
// src/main/kotlin/com/yourapp/payment/job/ReconciliationJob.kt
@Component
@Slf4j
class ReconciliationJob(
    private val razorpayPaymentService: RazorpayPaymentService,
    private val paymentRepository: PaymentRepository,
    private val alertService: AlertService
) {

    /**
     * Runs daily at 2 AM — reconciles yesterday's payments
     */
    @Scheduled(cron = "0 0 2 * * *")
    fun reconcileYesterdayPayments() {
        val yesterday = java.time.LocalDate.now().minusDays(1)
        val startOfDay = yesterday.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
        val endOfDay = yesterday.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)

        log.info("Starting reconciliation for {}", yesterday)

        // Get all payments from our DB that should be confirmed
        val ourConfirmedPayments = paymentRepository.findByStatusAndCreatedAtBetween(
            PaymentStatus.PAYMENT_CONFIRMED, startOfDay, endOfDay
        )

        var discrepancies = 0

        ourConfirmedPayments.forEach { payment ->
            val gatewayPaymentId = payment.gatewayPaymentId ?: return@forEach

            // Fetch payment from Razorpay
            val razorpayPayment = razorpayPaymentService.fetchPayment(gatewayPaymentId)
            val gatewayStatus = razorpayPayment.getString("status")
            val gatewayAmount = razorpayPayment.getLong("amount")

            // Check for discrepancies
            when {
                gatewayStatus != "captured" -> {
                    log.error("RECONCILIATION DISCREPANCY: Our DB says CONFIRMED but Razorpay says {} - paymentId={}",
                        gatewayStatus, gatewayPaymentId)
                    alertService.sendCriticalAlert(
                        "Payment status mismatch: $gatewayPaymentId — DB: CONFIRMED, Gateway: $gatewayStatus"
                    )
                    discrepancies++
                }
                gatewayAmount != payment.amount -> {
                    log.error("RECONCILIATION DISCREPANCY: Amount mismatch - paymentId={} ourAmount={} gatewayAmount={}",
                        gatewayPaymentId, payment.amount, gatewayAmount)
                    discrepancies++
                }
            }
        }

        log.info("Reconciliation complete - checked={} discrepancies={}",
            ourConfirmedPayments.size, discrepancies)

        if (discrepancies > 0) {
            alertService.sendCriticalAlert(
                "Daily reconciliation found $discrepancies discrepancies for $yesterday. Manual review required."
            )
        }
    }
}
```

---

## PCI DSS Compliance: Why You Never Handle Raw Card Data

PCI DSS (Payment Card Industry Data Security Standard) applies to any system that touches raw card data (card number, CVV, expiry). Compliance requires:
- Quarterly security scans
- Annual audits
- Network segmentation
- Encryption at rest and in transit
- Regular penetration testing

This costs **₹50 lakh+ per year** for a startup and requires dedicated security staff.

**The correct approach**: Use a PCI-compliant payment gateway's hosted checkout.

```
✅ User enters card data in Razorpay's hosted UI (NOT your app's input fields)
   ↓
   Razorpay tokenizes the card
   ↓
   Your backend only receives a token (e.g., "tok_xxxxxxxxxx")
   ↓
   You use the token for charges — you NEVER see the actual card number

Your infrastructure is now "out of scope" for PCI DSS (or minimum scope)
```

> [!CAUTION]
> **NEVER build your own card input form** that sends raw card data to your backend. Even if you immediately forward to Razorpay, if raw card data ever touches your server, you're now in PCI scope. Use Razorpay's `<iframe>` or their mobile SDK which tokenizes card data on the client side before it ever leaves the user's device.

---

## Chargeback and Dispute Handling

A chargeback is when a user disputes a charge with their bank. The bank reverses the transaction and demands evidence from you. You have typically 7-14 days to respond.

```kotlin
// Evidence you need to collect and store at order time (for chargebacks later):
data class OrderEvidence(
    val orderId: String,
    val userId: String,
    val deviceFingerprint: String,    // Identify the device
    val ipAddress: String,            // IP at time of order
    val deliveryAddress: String,      // Confirmed delivery address
    val deliveryPhotoUrl: String?,    // Photo taken at delivery time
    val otp: String?,                 // Delivery OTP (proves recipient received it)
    val deliveryTimestamp: Instant,
    val deliveryAgentId: String,
    val paymentMethod: String         // UPI/card (UPI disputes are lower risk)
)
```

---

## Production Payment Service Schema

```sql
-- Payments table
CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            VARCHAR(100) NOT NULL,
    user_id             VARCHAR(100) NOT NULL,
    amount              BIGINT NOT NULL,           -- Amount in paise
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    gateway             VARCHAR(30) NOT NULL DEFAULT 'RAZORPAY',
    gateway_order_id    VARCHAR(100),              -- Razorpay order_id
    gateway_payment_id  VARCHAR(100),              -- Razorpay payment_id
    payment_method      VARCHAR(30),               -- upi, card, netbanking
    failure_reason      TEXT,
    initiated_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    confirmed_at        TIMESTAMP WITH TIME ZONE,
    failed_at           TIMESTAMP WITH TIME ZONE,
    idempotency_key     VARCHAR(255),
    metadata            JSONB DEFAULT '{}'
);

CREATE UNIQUE INDEX idx_payments_gateway_order ON payments(gateway_order_id)
    WHERE gateway_order_id IS NOT NULL;
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_initiated_at ON payments(initiated_at);

-- Refunds table  
CREATE TABLE refunds (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id          UUID NOT NULL REFERENCES payments(id),
    order_id            VARCHAR(100) NOT NULL,
    amount              BIGINT NOT NULL,           -- Refund amount in paise
    status              VARCHAR(30) NOT NULL DEFAULT 'REFUND_INITIATED',
    reason              VARCHAR(100),
    gateway_refund_id   VARCHAR(100),
    requested_by        VARCHAR(100),              -- User ID or 'SYSTEM' or 'ADMIN'
    failure_reason      TEXT,
    initiated_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at        TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX idx_refunds_order_id ON refunds(order_id);
CREATE INDEX idx_refunds_status ON refunds(status);
```

---

## Key Takeaways

1. **Webhooks are more authoritative than synchronous API responses** — build your system around webhooks
2. **HMAC signature verification is non-negotiable** — reject any unverified webhook immediately
3. **Idempotency keys on EVERY payment API call** — the internet is unreliable, retries will happen
4. **Transactional outbox pattern** for atomic payment + order update (not synchronous inter-service calls)
5. **All amounts in paise (smallest unit)** — never use floating point for money
6. **Daily reconciliation job** — your safety net against webhook failures and bugs
7. **PCI DSS**: Use gateway's hosted UI — never let raw card data touch your servers
8. **Payment state machine** — every state transition must be explicit and logged
9. **Return 500 on webhook processing failure** — so Razorpay retries it
10. **Store evidence at order time** — you'll need it for chargebacks you can't predict

> [!NOTE]
> Stripe built their entire reputation on developer-friendly payments with rock-solid idempotency. Their idempotency implementation handles distributed failures so elegantly that their API is used as the industry reference design. Everything in this chapter mirrors Stripe's principles applied to the Indian payment ecosystem (Razorpay/Cashfree + UPI).
