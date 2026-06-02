# P0-9 — Observability: Logs vs Metrics vs Traces, Grafana, Prometheus, OpenTelemetry

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Production Truth**: The difference between a 2-minute outage and a 2-hour outage is observability. When Zepto's delivery system goes down at 2am, the on-call engineer has exactly one tool: their observability stack. Without it, they're blind.

---

## The Core Problem: You Can't Debug What You Can't See

Every production system fails. Disks fill up. Queries degrade. Memory leaks accumulate over 6 hours. A third-party API starts returning 503s. The question is never **if** — it's **how fast you find it and how fast you fix it**.

Observability is the property of a system that allows you to infer its internal state from its external outputs. It's not a tool — it's an architectural property you bake into your system from day one.

The industry has converged on **three pillars** that together answer every diagnostic question you'll ever have in production.

---

## The Three Pillars of Observability

### Pillar 1: Logs — "What happened?"

Logs are discrete events. Something happened at a point in time and you recorded it. A request came in. A DB query ran. An exception was thrown. A payment webhook was received.

**Logs answer**: What happened? What was the state at the time? What error occurred?

### Pillar 2: Metrics — "How is the system behaving over time?"

Metrics are numerical measurements aggregated over time. Request rate, error rate, p99 latency, JVM heap used, DB connection pool active connections. They are stored as time-series data.

**Metrics answer**: Is the system healthy right now? Is it degrading? When did latency spike?

### Pillar 3: Traces — "How did this specific request flow through the system?"

A trace follows a single request across multiple services. In a microservices architecture, one user action might touch 6 services. A trace connects all the spans (units of work) in that journey with a single trace ID.

**Traces answer**: Which service is the bottleneck? Why is this specific user experiencing slow response? Where exactly did the error originate in the call chain?

> [!IMPORTANT]
> You need ALL THREE. Metrics alert you that something is wrong. Logs tell you what happened. Traces tell you where. Trying to do production debugging with only one or two pillars is like trying to navigate with one eye closed.

---

## Pillar 1: Structured Logging in Spring Boot

### Why Plain Text Logs Are a Production Crime

```
# BAD — plain text log
2024-01-15 14:23:45 ERROR PaymentService - Payment failed for order 12345
```

When you have 500 pods running and 10 million log lines per hour, you cannot grep this effectively. You cannot aggregate it. You cannot alert on it. You cannot do any meaningful analysis.

```json
// GOOD — structured JSON log
{
  "timestamp": "2024-01-15T14:23:45.123Z",
  "level": "ERROR",
  "service": "payment-service",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "userId": "usr_9K3m2",
  "orderId": "ord_7x9Pk2",
  "paymentGateway": "razorpay",
  "errorCode": "PAYMENT_CAPTURE_FAILED",
  "errorMessage": "Gateway timeout after 30s",
  "message": "Payment capture failed",
  "environment": "production",
  "version": "2.4.1"
}
```

Every field is queryable. In Kibana or Grafana Loki, you can instantly filter `errorCode=PAYMENT_CAPTURE_FAILED AND paymentGateway=razorpay` and see exactly how many users were affected.

### Logback Configuration for JSON Logging

Add the dependency:

```kotlin
// build.gradle.kts
dependencies {
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    // Spring Boot already includes logback-classic transitively
}
```

Create `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- Console appender with JSON encoding for production -->
    <springProfile name="production,staging">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>spanId</includeMdcKeyName>
                <includeMdcKeyName>userId</includeMdcKeyName>
                <includeMdcKeyName>orderId</includeMdcKeyName>
                <includeMdcKeyName>requestId</includeMdcKeyName>
                <customFields>{"service":"payment-service","environment":"production"}</customFields>
                <fieldNames>
                    <timestamp>timestamp</timestamp>
                    <message>message</message>
                    <logger>logger</logger>
                    <thread>thread</thread>
                    <levelValue>[ignore]</levelValue>
                </fieldNames>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>

        <!-- Suppress noisy framework logs -->
        <logger name="org.hibernate.SQL" level="WARN"/>
        <logger name="org.springframework.web" level="WARN"/>
        <logger name="com.zaxxer.hikari" level="WARN"/>
    </springProfile>

    <!-- Human-readable for local development -->
    <springProfile name="local,default">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{traceId}] - %msg%n</pattern>
            </encoder>
        </appender>

        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>

        <logger name="org.hibernate.SQL" level="DEBUG"/>
        <logger name="org.hibernate.type.descriptor.sql" level="TRACE"/>
    </springProfile>

</configuration>
```

### Log Levels Discipline — The Production Reality

| Level | Use Case | Production Volume |
|-------|----------|------------------|
| `ERROR` | Something failed that requires immediate attention. Customer impact. | Should be ZERO under normal operation |
| `WARN` | Something unexpected happened but the system recovered. Degraded path taken. | Low — alert if sustained |
| `INFO` | Key business events: order created, payment confirmed, user registered | Moderate — every important state transition |
| `DEBUG` | Detailed operational data: SQL queries, HTTP request bodies | OFF in production |
| `TRACE` | Extremely granular: loop iterations, byte-level data | NEVER in production |

> [!WARNING]
> A common rookie mistake is logging at `DEBUG` in production because "we might need it". This creates millions of useless log lines that hide real errors AND costs you money in log ingestion (Datadog, Elastic charge per GB). Stripe's engineering team has a rule: if a log line doesn't help you diagnose a real production issue, delete it.

```kotlin
// src/main/kotlin/com/yourapp/common/logging/LoggingExtensions.kt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Kotlin extension for inline logger creation — no more companion object boilerplate
inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

// Usage in any class:
// private val log = logger()
```

---

## The Correlation ID Pattern with MDC

### Why Correlation IDs Are Non-Negotiable

When a user reports "my order didn't go through at 3:47pm", you need to find ALL log lines from that single request across ALL your services. Without a correlation ID, you're searching for a needle in a haystack of 10 million log lines.

The pattern: every inbound HTTP request gets a unique ID. This ID is propagated through every downstream call, included in every log line, and returned to the client in the response header.

### MDC (Mapped Diagnostic Context) Implementation

MDC is a thread-local map in SLF4J that automatically includes key-value pairs in every log line emitted on that thread.

```kotlin
// src/main/kotlin/com/yourapp/common/filter/CorrelationIdFilter.kt
package com.yourapp.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(1) // Run first before any other filter
class CorrelationIdFilter : OncePerRequestFilter() {

    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val REQUEST_ID_MDC_KEY = "requestId"
        const val USER_ID_MDC_KEY = "userId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Honor upstream correlation ID (from API gateway, mobile client) or generate new one
        val correlationId = request.getHeader(CORRELATION_ID_HEADER)
            ?: UUID.randomUUID().toString().replace("-", "")

        try {
            // Set in MDC — now EVERY log line on this thread will include requestId automatically
            MDC.put(REQUEST_ID_MDC_KEY, correlationId)

            // Echo it back in the response so mobile clients can report it for support tickets
            response.setHeader(CORRELATION_ID_HEADER, correlationId)

            filterChain.doFilter(request, response)
        } finally {
            // CRITICAL: Always clear MDC after request. Servlet containers use thread pools.
            // Forgetting this means the NEXT request on this thread gets the PREVIOUS request's MDC values.
            MDC.clear()
        }
    }
}
```

> [!CAUTION]
> **Thread pool contamination** is a real production bug. If you use `MDC.put()` but forget `MDC.clear()` in a finally block, thread pool threads will carry over MDC values from previous requests. In a Tomcat thread pool of 200 threads, this means request B might log with request A's userId. This causes completely wrong attribution in your logs and can mislead your debugging for hours.

```kotlin
// src/main/kotlin/com/yourapp/common/filter/UserContextFilter.kt
package com.yourapp.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(2) // Run after auth filter which validates and sets userId in SecurityContext
class UserContextFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Extract userId from JWT principal set by Spring Security
        val userId = extractUserIdFromSecurityContext()

        if (userId != null) {
            MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, userId)
        }

        filterChain.doFilter(request, response)
        // Note: CorrelationIdFilter.MDC.clear() handles cleanup since it wraps this filter
    }

    private fun extractUserIdFromSecurityContext(): String? {
        return try {
            val auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().authentication
            auth?.name
        } catch (e: Exception) {
            null
        }
    }
}
```

### Using MDC in Business Logic

```kotlin
// src/main/kotlin/com/yourapp/payment/service/PaymentService.kt
package com.yourapp.payment.service

import org.slf4j.MDC
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val razorpayClient: RazorpayClient,
    private val paymentRepository: PaymentRepository
) {

    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    fun capturePayment(orderId: String, paymentId: String, amount: Long): PaymentResult {
        // Add order-specific context to MDC for this logical operation
        MDC.put("orderId", orderId)
        MDC.put("paymentId", paymentId)
        MDC.put("amount", amount.toString())

        try {
            log.info("Initiating payment capture") // Log line will automatically include all MDC fields

            val result = razorpayClient.capture(paymentId, amount)

            log.info("Payment capture successful - gatewayReference={}", result.gatewayRef)

            return result
        } catch (e: RazorpayException) {
            // ERROR log with full context - MDC fields included automatically
            log.error("Payment capture failed - errorCode={} gatewayMessage={}",
                e.errorCode, e.message, e)
            throw PaymentCaptureException("Payment capture failed", e)
        } finally {
            // Clean up order-specific keys (keep request-level keys set by filter)
            MDC.remove("orderId")
            MDC.remove("paymentId")
            MDC.remove("amount")
        }
    }
}
```

---

## Pillar 2: Metrics with Micrometer and Prometheus

### Micrometer — The Metrics Facade

Micrometer is to metrics what SLF4J is to logging — a vendor-neutral facade. You write `counter.increment()` once, and Micrometer can ship it to Prometheus, Datadog, CloudWatch, InfluxDB, or any other backend just by changing a dependency. This is genius API design that prevents vendor lock-in.

Spring Boot Actuator auto-configures Micrometer and exposes dozens of metrics out of the box with zero code.

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // OpenTelemetry integration (for traces — covered later)
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
}
```

```yaml
# application.yml — Actuator and Prometheus configuration
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
        # NEVER expose: env, configprops, beans, mappings in production (they leak secrets and internals)
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized  # Don't expose DB status to unauthenticated requests
      show-components: when-authorized
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${APP_ENV:local}
    distribution:
      percentiles-histogram:
        http.server.requests: true  # Enable histogram for p50/p95/p99 HTTP latency
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      slo:
        http.server.requests: 100ms, 500ms, 1000ms  # Service Level Objectives
  prometheus:
    metrics:
      export:
        enabled: true
        step: 10s  # How often metrics are computed

# Restrict /actuator to internal network in production
# In production, use Spring Security to restrict /actuator/prometheus to Prometheus's IP range
```

> [!WARNING]
> **NEVER** expose `/actuator/env` or `/actuator/configprops` publicly. These endpoints return ALL environment variables, which includes your database passwords, JWT secrets, API keys, and every sensitive configuration value. This is how companies get breached. Restrict actuator endpoints to internal network only (use `management.server.port=9090` with a separate internal port that's not exposed by your load balancer).

### Auto-Exposed Metrics (Free with Zero Code)

Spring Boot + Micrometer automatically exposes:

| Metric | Type | What It Tells You |
|--------|------|-------------------|
| `jvm.memory.used` | Gauge | JVM heap/non-heap used |
| `jvm.gc.pause` | Timer | GC pause duration and frequency |
| `jvm.threads.live` | Gauge | Active thread count |
| `hikaricp.connections.active` | Gauge | Active DB connections |
| `hikaricp.connections.pending` | Gauge | Threads waiting for a DB connection |
| `hikaricp.connections.timeout.total` | Counter | Times a thread couldn't get a DB connection |
| `http.server.requests` | Timer | HTTP request count, latency by endpoint |
| `spring.data.repository.invocations` | Timer | JPA repository call count and latency |
| `process.cpu.usage` | Gauge | JVM process CPU utilization |
| `system.cpu.usage` | Gauge | System-level CPU utilization |

### Custom Business Metrics

Auto-exposed infrastructure metrics are great, but you also need **business metrics** that tell you whether your product is actually working.

```kotlin
// src/main/kotlin/com/yourapp/payment/metrics/PaymentMetrics.kt
package com.yourapp.payment.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class PaymentMetrics(private val meterRegistry: MeterRegistry) {

    // Counter — monotonically increasing. Use for: orders placed, payments failed, users registered
    private val paymentsInitiated: Counter = Counter.builder("payments.initiated.total")
        .description("Total number of payment initiations")
        .tag("gateway", "razorpay")
        .register(meterRegistry)

    private val paymentsSucceeded: Counter = Counter.builder("payments.succeeded.total")
        .description("Total successful payments")
        .tag("gateway", "razorpay")
        .register(meterRegistry)

    private val paymentsFailed: Counter = Counter.builder("payments.failed.total")
        .description("Total failed payments")
        .tag("gateway", "razorpay")
        .register(meterRegistry)

    // Timer — measures duration AND counts. Use for: external API calls, DB queries, business operations
    private val paymentCaptureTimer: Timer = Timer.builder("payments.capture.duration")
        .description("Time taken to capture payment from gateway")
        .tag("gateway", "razorpay")
        .publishPercentiles(0.5, 0.95, 0.99) // p50, p95, p99
        .publishPercentileHistogram(true)
        .register(meterRegistry)

    // Gauge — current value. Use for: queue depth, active sessions, wallet balance of top users
    // Gauges are registered differently — they hold a weak reference to the measured object
    private val activePaymentSessions = java.util.concurrent.atomic.AtomicLong(0)

    init {
        io.micrometer.core.instrument.Gauge.builder("payments.sessions.active", activePaymentSessions) { it.get().toDouble() }
            .description("Currently active payment sessions")
            .register(meterRegistry)
    }

    fun recordPaymentInitiated() = paymentsInitiated.increment()

    fun recordPaymentSucceeded() = paymentsSucceeded.increment()

    fun recordPaymentFailed(reason: String) {
        // Dynamic tags allow filtering failed payments by failure reason in Grafana
        Counter.builder("payments.failed.total")
            .tag("gateway", "razorpay")
            .tag("reason", reason) // e.g., "timeout", "insufficient_funds", "card_declined"
            .register(meterRegistry)
            .increment()
    }

    fun <T> timePaymentCapture(operation: () -> T): T {
        return paymentCaptureTimer.recordCallable(operation)!!
    }

    fun incrementActiveSessions() = activePaymentSessions.incrementAndGet()
    fun decrementActiveSessions() = activePaymentSessions.decrementAndGet()
}
```

```kotlin
// Usage in PaymentService
@Service
class PaymentService(
    private val paymentMetrics: PaymentMetrics,
    private val razorpayClient: RazorpayClient
) {
    fun capturePayment(orderId: String, paymentId: String, amount: Long): PaymentResult {
        paymentMetrics.recordPaymentInitiated()
        paymentMetrics.incrementActiveSessions()

        return try {
            // Timer wraps the actual capture call — measures real gateway latency
            val result = paymentMetrics.timePaymentCapture {
                razorpayClient.capture(paymentId, amount)
            }
            paymentMetrics.recordPaymentSucceeded()
            result
        } catch (e: GatewayTimeoutException) {
            paymentMetrics.recordPaymentFailed("gateway_timeout")
            throw e
        } catch (e: InsufficientFundsException) {
            paymentMetrics.recordPaymentFailed("insufficient_funds")
            throw e
        } finally {
            paymentMetrics.decrementActiveSessions()
        }
    }
}
```

### Metric Types — When to Use What

```
Counter   → Always going up. Total events. Never use for values that can decrease.
           Example: total_orders_placed, total_payments_failed, total_api_calls

Gauge     → Current snapshot. Can go up or down.
           Example: active_connections, queue_depth, memory_used, temperature

Timer     → Duration + count combined. Automatically computes p50/p95/p99.
           Example: http_request_duration, db_query_duration, external_api_call_duration

DistributionSummary → Like Timer but for non-time quantities.
           Example: payment_amount_distribution, order_item_count, response_body_size
```

---

## Prometheus: Pull-Based Metrics Collection

### Architecture: How Prometheus Works

Prometheus is fundamentally different from push-based monitoring (like StatsD/Graphite). Prometheus **pulls** metrics from your services on a schedule. Your Spring Boot app exposes `/actuator/prometheus` — Prometheus scrapes it every 15 seconds.

```
[Spring Boot App] ← scrape every 15s ← [Prometheus Server]
                    /actuator/prometheus                     ↓
                                                    [Time Series DB]
                                                             ↓
                                                        [Grafana]
                                                    [AlertManager]
```

**Why pull-based?**
- Prometheus controls the scrape rate — it can't be overwhelmed by a misconfigured app pushing too fast
- You can tell which apps are down (they stop responding to scrapes)
- Service discovery: Prometheus can auto-discover new pods in Kubernetes

### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s      # How often to scrape
  evaluation_interval: 15s  # How often to evaluate alerting rules
  
  # Labels added to all metrics from this Prometheus instance
  external_labels:
    cluster: 'production-ap-south-1'
    region: 'india'

# Alerting configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

# Load alerting rules from files
rule_files:
  - "alerts/springboot_alerts.yml"
  - "alerts/database_alerts.yml"
  - "alerts/business_alerts.yml"

# Scrape configurations
scrape_configs:
  # Scrape Prometheus itself
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Scrape Spring Boot applications
  - job_name: 'spring-boot-apps'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s
    static_configs:
      - targets:
          - 'payment-service:8080'
          - 'order-service:8080'
          - 'user-service:8080'
          - 'notification-service:8080'
    # In Kubernetes, use kubernetes_sd_configs instead of static_configs
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance

  # PostgreSQL metrics (requires postgres_exporter sidecar)
  - job_name: 'postgresql'
    static_configs:
      - targets: ['postgres-exporter:9187']

  # Redis metrics (requires redis_exporter sidecar)
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

### PromQL — The Query Language

PromQL is a functional query language for time series data. You must know it to build dashboards and alerts.

```promql
# --- INSTANT VECTORS ---

# All HTTP request samples right now
http_server_requests_seconds_count

# Filter by label
http_server_requests_seconds_count{uri="/api/v1/payments", status="200"}

# --- RANGE VECTORS (with [time window]) ---

# Rate of requests over last 5 minutes (requests per second)
rate(http_server_requests_seconds_count[5m])

# --- KEY QUERIES ---

# 1. Error rate per endpoint (percentage)
(
  rate(http_server_requests_seconds_count{status=~"5.."}[5m])
  /
  rate(http_server_requests_seconds_count[5m])
) * 100

# 2. P99 latency for a specific endpoint
histogram_quantile(
  0.99,
  rate(http_server_requests_seconds_bucket{uri="/api/v1/payments"}[5m])
)

# 3. HikariCP connection pool utilization
hikaricp_connections_active / hikaricp_connections_max

# 4. JVM heap utilization
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# 5. GC time percentage (how much time is spent GC'ing vs serving requests)
rate(jvm_gc_pause_seconds_sum[5m]) / 15

# 6. Payment success rate
rate(payments_succeeded_total[5m])
/
rate(payments_initiated_total[5m])

# 7. Active DB connections pending (queue buildup — early sign of DB saturation)
hikaricp_connections_pending

# 8. CPU usage
process_cpu_usage * 100

# 9. Requests per second by service
sum by (job) (rate(http_server_requests_seconds_count[1m]))
```

### Alerting Rules

```yaml
# alerts/springboot_alerts.yml
groups:
  - name: springboot-slos
    interval: 30s
    rules:

      # Alert when p99 latency exceeds 2 seconds for 5 minutes
      - alert: HighLatencyP99
        expr: |
          histogram_quantile(
            0.99,
            rate(http_server_requests_seconds_bucket[5m])
          ) > 2.0
        for: 5m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "High P99 latency on {{ $labels.job }}"
          description: "P99 latency is {{ $value | humanizeDuration }} on {{ $labels.instance }} for endpoint {{ $labels.uri }}"
          runbook: "https://wiki.yourcompany.com/runbooks/high-latency"

      # Alert when error rate exceeds 1% for 3 minutes
      - alert: HighErrorRate
        expr: |
          (
            rate(http_server_requests_seconds_count{status=~"5.."}[5m])
            /
            rate(http_server_requests_seconds_count[5m])
          ) * 100 > 1
        for: 3m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "High error rate on {{ $labels.job }}"
          description: "Error rate is {{ $value | humanize }}% on {{ $labels.instance }}"

      # Alert when DB connection pool is nearly exhausted (>80% utilized)
      - alert: DatabaseConnectionPoolExhaustion
        expr: |
          hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 2m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "DB connection pool near exhaustion on {{ $labels.job }}"
          description: "{{ $value | humanizePercentage }} of connections used. If this hits 100%, new requests will queue and timeout."
          runbook: "https://wiki.yourcompany.com/runbooks/db-connection-pool"

      # Alert when DB connections are pending (queue has built up)
      - alert: DatabaseConnectionsPending
        expr: hikaricp_connections_pending > 5
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Threads waiting for DB connection on {{ $labels.job }}"
          description: "{{ $value }} threads are waiting for a DB connection. Service is about to become unresponsive."

      # Alert when JVM heap is >85% for 10 minutes (memory leak signal)
      - alert: JVMHeapHighUtilization
        expr: |
          jvm_memory_used_bytes{area="heap"}
          /
          jvm_memory_max_bytes{area="heap"} > 0.85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "JVM heap high on {{ $labels.instance }}"
          description: "JVM heap at {{ $value | humanizePercentage }}. GC pressure increasing."

      # Business alert: payment success rate dropped below 95%
      - alert: PaymentSuccessRateLow
        expr: |
          rate(payments_succeeded_total[5m])
          /
          rate(payments_initiated_total[5m]) < 0.95
        for: 3m
        labels:
          severity: critical
          team: payments
          page: true  # PagerDuty page, not just Slack notification
        annotations:
          summary: "Payment success rate below 95%"
          description: "Payment success rate is {{ $value | humanizePercentage }}. Direct revenue impact."
```

---

## Grafana: Dashboards and Alerting

### Connecting Grafana to Prometheus

In production, Grafana is configured via `datasources.yml` provisioning files (not manual UI clicks — configuration must be code):

```yaml
# grafana/provisioning/datasources/prometheus.yml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false  # Prevent accidental UI changes
    jsonData:
      httpMethod: POST
      prometheusType: Prometheus
      prometheusVersion: 2.40.0
      timeInterval: "15s"  # Match Prometheus scrape interval
```

### Pre-Built Spring Boot Dashboard

Import dashboard ID `4701` from Grafana's public dashboard library for a comprehensive JVM + Spring Boot metrics dashboard. This gives you:
- JVM heap, non-heap, GC activity
- HTTP request rates, error rates, latency percentiles
- Thread pools, class loading
- HikariCP connection pool
- CPU and memory

```yaml
# grafana/provisioning/dashboards/dashboard.yml
apiVersion: 1
providers:
  - name: 'Spring Boot Apps'
    orgId: 1
    folder: 'Spring Boot'
    type: file
    disableDeletion: false
    editable: true
    updateIntervalSeconds: 30
    options:
      path: /etc/grafana/dashboards
```

### Alert Channels: Slack and PagerDuty

```yaml
# grafana/provisioning/alerting/notification-channels.yml
apiVersion: 1
contactPoints:
  - orgId: 1
    name: Slack - Engineering
    receivers:
      - uid: slack-engineering
        type: slack
        settings:
          url: ${SLACK_WEBHOOK_URL}
          channel: '#alerts-production'
          username: 'Grafana Alert'
          icon_emoji: ':rotating_light:'
          title: '{{ template "slack.default.title" . }}'
          text: '{{ template "slack.default.text" . }}'
        disableResolveMessage: false

  - orgId: 1
    name: PagerDuty - Critical
    receivers:
      - uid: pagerduty-critical
        type: pagerduty
        settings:
          integrationKey: ${PAGERDUTY_INTEGRATION_KEY}
          severity: critical
          class: production
          component: payment-service
```

---

## Pillar 3: Distributed Tracing with OpenTelemetry

### Why Tracing Is Essential in Microservices

Consider a Zepto order flow:
1. Mobile app → **API Gateway** → **Order Service** (creates order)
2. **Order Service** → **Inventory Service** (reserves items)
3. **Order Service** → **Payment Service** (charges wallet)
4. **Payment Service** → **Razorpay** (external call)
5. **Order Service** → **Notification Service** (sends push notification)

If the user's checkout takes 8 seconds instead of 1 second, which service is slow? Without tracing, you have zero idea. With tracing, you can see a Gantt chart of every service call and exactly where the 7 extra seconds were spent.

### OpenTelemetry — The Industry Standard

OpenTelemetry (OTel) is the merger of OpenCensus and OpenTracing. It's now the CNCF standard and supported by every major cloud provider and APM vendor (Datadog, Honeycomb, Grafana Tempo, Jaeger, Zipkin).

The key concept: **Vendor-neutral instrumentation**. You write OTel code once, and you can send traces to Jaeger today and Datadog tomorrow by changing your exporter configuration — zero code changes.

### Spring Boot 3 + OpenTelemetry Setup

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Micrometer Tracing — the bridge between Spring Boot and OTel
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    
    // OTel SDK
    implementation("io.opentelemetry:opentelemetry-sdk")
    
    // Exporter — sends traces to your backend (Tempo, Jaeger, Datadog, etc.)
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    
    // Auto-instrumentation for HTTP clients
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
}
```

```yaml
# application.yml — OpenTelemetry configuration
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% in development; use 0.1 (10%) in high-traffic production

spring:
  application:
    name: payment-service  # This becomes the service name in traces

# OpenTelemetry exporter configuration
otel:
  exporter:
    otlp:
      endpoint: http://otel-collector:4318  # OTLP HTTP endpoint of your OTel Collector
      protocol: http/protobuf
  resource:
    attributes:
      service.name: ${spring.application.name}
      service.version: ${APP_VERSION:unknown}
      deployment.environment: ${APP_ENV:local}
```

> [!NOTE]
> In production at high traffic (Uber-scale), sampling 100% of traces is expensive. Use **adaptive sampling**: sample 100% of traces with errors, 100% of slow traces (>1s), and 1-5% of normal traces. This gives you full visibility into problems without the storage cost of tracing every single request.

### Adding Custom Spans

Spring Boot auto-instruments HTTP requests and Spring Data repositories. But you often need custom spans for important business operations:

```kotlin
// src/main/kotlin/com/yourapp/payment/service/PaymentService.kt
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val observationRegistry: ObservationRegistry,
    private val razorpayClient: RazorpayClient
) {

    fun capturePayment(orderId: String, paymentId: String, amount: Long): PaymentResult {
        // Create a custom span — this shows up as a distinct segment in your trace timeline
        return Observation.createNotStarted("payment.capture", observationRegistry)
            .lowCardinalityKeyValue("gateway", "razorpay")
            .highCardinalityKeyValue("orderId", orderId)     // High cardinality = many unique values
            .highCardinalityKeyValue("paymentId", paymentId)
            .observe {
                razorpayClient.capture(paymentId, amount)
            }
    }
}
```

> [!IMPORTANT]
> **Low cardinality vs high cardinality tags** is critical for Prometheus performance. Low cardinality means few unique values (e.g., `status=success/failure`, `gateway=razorpay/cashfree`). High cardinality means many unique values (e.g., `orderId=ord_abc123`). **NEVER use high cardinality values as Prometheus metric tags** — this creates millions of unique time series and will crash Prometheus. High cardinality values belong in traces (Tempo/Jaeger/Datadog APM) or in log fields. Uber famously had a Prometheus meltdown caused by careless use of `user_id` as a metric label.

### Propagating Trace Context Across HTTP Calls

When your Order Service calls Payment Service, the trace ID must be propagated in HTTP headers so Tempo/Jaeger can stitch together the full trace:

```kotlin
// Spring Boot auto-configures this when micrometer-tracing-bridge-otel is on classpath
// But for RestTemplate / WebClient, you need to ensure the interceptors are registered

// src/main/kotlin/com/yourapp/common/config/HttpClientConfig.kt
@Configuration
class HttpClientConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        // builder auto-configures OTel trace propagation interceptors
        return builder
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(30))
            .build()
    }

    @Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        // WebClient.Builder auto-configures OTel propagation when micrometer-tracing is on classpath
        return builder
            .baseUrl("https://api.razorpay.com")
            .build()
    }
}
```

The W3C Trace Context standard headers that get propagated:
- `traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`
- `tracestate: (vendor-specific additional trace info)`

---

## Log Aggregation: ELK Stack vs Loki

### Option 1: ELK Stack (Elasticsearch + Logstash + Kibana)

The classic enterprise stack. Extremely powerful full-text search. High operational cost.

```
[App Pods] → [Filebeat/Fluentd] → [Logstash] → [Elasticsearch] → [Kibana]
```

**Pros**: Full-text search, complex aggregations, mature ecosystem, Kibana Lens for analytics
**Cons**: Elasticsearch is expensive to run (memory-hungry), complex operational overhead, licensing costs

### Option 2: Grafana Loki + Promtail (Recommended for Cost-Conscious Teams)

Loki is "Prometheus but for logs". It doesn't index log content (only labels). This makes it dramatically cheaper to run. Query language is LogQL, similar to PromQL.

```
[App Pods] → [Promtail/Alloy] → [Loki] → [Grafana]
```

**Pros**: Cheap storage, integrates perfectly with Grafana (one UI for metrics + logs + traces), horizontally scalable, object storage (S3) backend
**Cons**: No full-text search on log content — only label-based queries

```yaml
# promtail-config.yml — Collects container logs and ships to Loki
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: spring-boot-containers
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
    relabel_configs:
      # Extract container name as a label
      - source_labels: [__meta_docker_container_name]
        target_label: container
      # Extract image name
      - source_labels: [__meta_docker_container_image]
        target_label: image
    pipeline_stages:
      # Parse JSON logs from Spring Boot
      - json:
          expressions:
            level: level
            traceId: traceId
            requestId: requestId
            userId: userId
      # Extract fields as Loki labels (keep cardinality LOW)
      - labels:
          level:
          traceId:
      # Drop DEBUG logs to save storage
      - drop:
          expression: '.*"level":"DEBUG".*'
```

LogQL query examples:

```logql
# All ERROR logs from payment-service
{container="payment-service"} |= "ERROR"

# Find all logs for a specific trace
{container=~".*-service"} | json | traceId="4bf92f3577b34da6a3ce929d0e0e4736"

# Count errors by service over last hour
sum by (container) (count_over_time({container=~".*-service"} |= "ERROR" [1h]))

# Payment failures in last 15 minutes
{container="payment-service"} | json | level="ERROR" | errorCode="PAYMENT_CAPTURE_FAILED" | __error__="" [15m]
```

---

## Complete Docker Compose: Full Observability Stack

```yaml
# docker-compose.observability.yml
version: '3.9'

networks:
  observability:
    driver: bridge
  app:
    driver: bridge

volumes:
  prometheus_data:
  grafana_data:
  loki_data:

services:

  # ─── Prometheus ──────────────────────────────────────────────────────────────
  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: prometheus
    restart: unless-stopped
    networks:
      - observability
      - app
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./prometheus/alerts:/etc/prometheus/alerts:ro
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'  # Keep 15 days of metrics
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'  # Allow POST /prometheus/-/reload for config reload
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:9090/-/healthy"]
      interval: 30s
      timeout: 10s
      retries: 3

  # ─── AlertManager ────────────────────────────────────────────────────────────
  alertmanager:
    image: prom/alertmanager:v0.26.0
    container_name: alertmanager
    restart: unless-stopped
    networks:
      - observability
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'

  # ─── Grafana ─────────────────────────────────────────────────────────────────
  grafana:
    image: grafana/grafana:10.2.2
    container_name: grafana
    restart: unless-stopped
    networks:
      - observability
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - ./grafana/dashboards:/etc/grafana/dashboards:ro
    environment:
      - GF_SECURITY_ADMIN_USER=${GRAFANA_USER:-admin}
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-changeme}
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_SERVER_ROOT_URL=http://grafana:3000
      - GF_INSTALL_PLUGINS=grafana-clock-panel,grafana-piechart-panel
      - GF_FEATURE_TOGGLES_ENABLE=traceToMetrics
    depends_on:
      prometheus:
        condition: service_healthy

  # ─── Loki ────────────────────────────────────────────────────────────────────
  loki:
    image: grafana/loki:2.9.3
    container_name: loki
    restart: unless-stopped
    networks:
      - observability
    ports:
      - "3100:3100"
    volumes:
      - loki_data:/loki
      - ./loki/loki-config.yml:/etc/loki/local-config.yaml:ro
    command: -config.file=/etc/loki/local-config.yaml
    healthcheck:
      test: ["CMD-SHELL", "wget --quiet --tries=1 --spider http://localhost:3100/ready || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  # ─── Promtail (Log Collector) ─────────────────────────────────────────────────
  promtail:
    image: grafana/promtail:2.9.3
    container_name: promtail
    restart: unless-stopped
    networks:
      - observability
      - app
    volumes:
      - ./promtail/promtail-config.yml:/etc/promtail/config.yml:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - /var/log:/var/log:ro
    command: -config.file=/etc/promtail/config.yml
    depends_on:
      loki:
        condition: service_healthy

  # ─── Grafana Tempo (Distributed Tracing Backend) ──────────────────────────────
  tempo:
    image: grafana/tempo:2.3.1
    container_name: tempo
    restart: unless-stopped
    networks:
      - observability
      - app
    ports:
      - "3200:3200"   # Tempo HTTP API
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
    volumes:
      - ./tempo/tempo-config.yml:/etc/tempo.yaml:ro
    command: -config.file=/etc/tempo.yaml

  # ─── OpenTelemetry Collector ──────────────────────────────────────────────────
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.91.0
    container_name: otel-collector
    restart: unless-stopped
    networks:
      - observability
      - app
    ports:
      - "4317:4317"   # OTLP gRPC receiver
      - "4318:4318"   # OTLP HTTP receiver
      - "8888:8888"   # Collector's own metrics
    volumes:
      - ./otel-collector/otel-collector-config.yml:/etc/otel-collector-config.yml:ro
    command: --config=/etc/otel-collector-config.yml
    depends_on:
      - tempo
```

### AlertManager Configuration for Slack Routing

```yaml
# alertmanager/alertmanager.yml
global:
  resolve_timeout: 5m
  slack_api_url: '${SLACK_WEBHOOK_URL}'

route:
  group_by: ['alertname', 'job']
  group_wait: 30s       # Wait 30s to group related alerts before sending
  group_interval: 5m    # Wait 5m before sending notification for same group
  repeat_interval: 4h   # Resend if alert is still firing after 4h
  receiver: 'slack-warnings'

  routes:
    # Critical alerts with page=true go to PagerDuty immediately
    - matchers:
        - severity = critical
        - page = true
      receiver: 'pagerduty-critical'
      repeat_interval: 30m

    # All critical alerts also go to Slack
    - matchers:
        - severity = critical
      receiver: 'slack-critical'
      continue: true  # Don't stop routing — also apply parent routes

    # Payment team alerts
    - matchers:
        - team = payments
      receiver: 'slack-payments-team'

receivers:
  - name: 'slack-warnings'
    slack_configs:
      - channel: '#alerts-warnings'
        title: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
        send_resolved: true

  - name: 'slack-critical'
    slack_configs:
      - channel: '#alerts-critical'
        color: 'danger'
        title: ':rotating_light: CRITICAL: {{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}\nRunbook: {{ .Annotations.runbook }}{{ end }}'

  - name: 'pagerduty-critical'
    pagerduty_configs:
      - routing_key: '${PAGERDUTY_INTEGRATION_KEY}'
        severity: critical
        description: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'

  - name: 'slack-payments-team'
    slack_configs:
      - channel: '#payments-alerts'
        api_url: '${PAYMENTS_SLACK_WEBHOOK_URL}'
```

---

## Spring Boot Actuator: What to Expose vs. Lock Down

### The Minimal Safe Production Exposure

```yaml
# application-production.yml
management:
  server:
    port: 9090  # CRITICAL: Expose actuator on a DIFFERENT port from your app
                # Your load balancer/nginx exposes port 8080 only — never 9090
                # Prometheus scrapes port 9090 from within the private network

  endpoints:
    web:
      exposure:
        # ONLY expose what you need. Everything else is locked.
        include: health, prometheus, info
      base-path: /actuator

  endpoint:
    health:
      show-details: never        # In production, only return UP/DOWN, not DB details
      show-components: never     # Don't reveal which components are failing publicly
      # Exception: your load balancer health check just needs 200 OK from /actuator/health
    info:
      enabled: true
    prometheus:
      enabled: true

  info:
    env:
      enabled: true  # Allow /actuator/info to show app version for deployment verification
    git:
      enabled: true
      mode: simple  # Shows branch, commit id, commit time

# Safe info properties to expose
info:
  app:
    name: ${spring.application.name}
    version: @project.version@
    java-version: @java.version@
```

### Endpoints and Their Risk Level

| Endpoint | Risk | Expose In Production? |
|----------|------|----------------------|
| `/actuator/health` | Low (if details hidden) | ✅ Yes — load balancer needs it |
| `/actuator/prometheus` | Medium (internal metric names) | ✅ Yes — but on internal port only |
| `/actuator/info` | Low | ✅ Yes — for deployment verification |
| `/actuator/metrics` | Medium | ⚠️ Internal only |
| `/actuator/env` | **CRITICAL** | ❌ NEVER — exposes all secrets |
| `/actuator/configprops` | **CRITICAL** | ❌ NEVER — exposes all config |
| `/actuator/beans` | High | ❌ NEVER — reveals full Spring context |
| `/actuator/mappings` | Medium | ❌ NEVER — reveals all API endpoints |
| `/actuator/threaddump` | Medium | ❌ NEVER publicly |
| `/actuator/heapdump` | **CRITICAL** | ❌ NEVER — heap dump can contain passwords stored in strings |
| `/actuator/loggers` | Medium | ⚠️ Internal only — but useful for dynamic log level changes |
| `/actuator/shutdown` | **CRITICAL** | ❌ NEVER — kills your process |

> [!CAUTION]
> The `/actuator/heapdump` endpoint downloads the full JVM heap as a binary file. JVM heaps contain all string objects — including database passwords, JWT tokens, API keys that were ever decoded from environment variables. If an attacker downloads your heap dump, they have all your credentials. Stripe's security team found this during a red team exercise.

---

## Production Runbook: Debugging a Latency Spike

When you get paged for "P99 latency is 8 seconds on /api/v1/orders":

```
1. GRAFANA: Open HTTP latency dashboard
   → Which endpoints are slow?
   → Is it all endpoints or specific ones?

2. PROMETHEUS QUERY:
   histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[2m]))
   → Identify which URI is slow

3. GRAFANA: Check HikariCP dashboard
   → hikaricp_connections_pending > 0? → DB is saturated
   → hikaricp_connections_active / max > 0.8? → Pool near exhaustion

4. GRAFANA: Check JVM GC dashboard
   → rate(jvm_gc_pause_seconds_sum[5m]) high? → GC pressure
   → jvm_memory_used_bytes{area="heap"} near max? → Memory leak

5. LOKI: Search logs for the affected time window
   {container="order-service"} | json | level="ERROR"
   → Any new error patterns?

6. TEMPO: Find a specific slow trace
   → Open a specific trace to see the span breakdown
   → Is it the DB query? External API call? Internal computation?

7. KUBERNETES (if applicable):
   kubectl top pods → CPU/memory per pod
   kubectl describe pod → Any OOMKilled or restart events?
```

---

## Key Takeaways

1. **Structured JSON logging** from day one — never plain text in production
2. **MDC with correlation IDs** — every log line must be correlatable to a request
3. **Separate actuator port** — never expose internal metrics on your public-facing port
4. **Business metrics in addition to infrastructure metrics** — payment success rate is more valuable than CPU usage
5. **OpenTelemetry for traces** — vendor-neutral, won't regret it
6. **High cardinality belongs in traces, not Prometheus labels** — this is the most commonly violated rule
7. **Grafana Loki over ELK** for startups — 10x cheaper, integrates with Grafana you already have
8. **Alert on what matters**: P99 latency, error rate, DB pool exhaustion, payment success rate
9. **Don't alert on symptoms you can't act on** — alert fatigue makes engineers ignore real pages

> [!NOTE]
> Netflix's famous culture: they run "Chaos Monkey" that randomly kills production services. They can do this confidently only because their observability stack tells them immediately when something breaks, and their distributed architecture means no single failure takes down the whole system. Observability is what enables velocity — not caution.
## Book-Aligned Correction

The book's production-readiness path starts with Actuator:

```text
spring-boot-starter-actuator
  -> health, metrics, env/config/beans diagnostics
  -> Micrometer
  -> Prometheus endpoint
  -> Grafana dashboards
  -> tracing/log correlation
```

Actuator is the source of many operational signals. Prometheus and Grafana are consumers/visualizers of those signals.

Protect sensitive endpoints like `/actuator/env`, `/actuator/configprops`, `/actuator/beans`, and `/actuator/conditions`.
