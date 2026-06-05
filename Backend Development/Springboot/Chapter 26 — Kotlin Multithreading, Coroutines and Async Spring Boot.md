# Chapter 26 — Kotlin Multithreading, Coroutines and Async Spring Boot

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

Research baseline:

- Spring Framework official Kotlin coroutine reference says Spring supports Kotlin coroutines in controllers, WebFlux functional routes, reactive transactions and context propagation.
- Spring Boot official Kotlin reference says Spring Boot manages coroutine dependency versions through the Kotlin Coroutines BOM.
- Kotlin official coroutine documentation explains that coroutines are concurrent computations that still run on real threads on the JVM.

This chapter teaches the concurrency ideas you need for Spring Boot backends written in Kotlin.

The big question:

```text
When should I use normal threads, Spring @Async, coroutines, WebFlux, queues, schedulers, locks or database transactions?
```

Short answer:

```text
Spring MVC + JPA backend:
  Use normal blocking code first.
  Use @Async for background work.
  Use thread pools carefully.
  Use database locks/transactions for correctness.
  Do not add coroutines everywhere.

Spring WebFlux + R2DBC backend:
  Coroutines are very useful.
  Use suspend functions, Flow and reactive transaction APIs.
  Avoid blocking JDBC/JPA calls on event-loop threads.
```

---

## 26.1 What Is Multithreading?

A thread is an operating-system execution path.

If your Spring Boot server handles 100 requests at the same time, it needs concurrency. In classic Spring MVC, concurrency usually means many request threads.

Example:

```text
Request 1 -> Tomcat thread 1 -> Controller -> Service -> JPA -> PostgreSQL
Request 2 -> Tomcat thread 2 -> Controller -> Service -> JPA -> PostgreSQL
Request 3 -> Tomcat thread 3 -> Controller -> Service -> JPA -> PostgreSQL
```

Each request gets a thread. If the request is waiting for PostgreSQL, that thread is blocked.

Blocked does not mean broken. It means the thread is waiting.

For most normal CRUD apps, this is perfectly fine.

---

## 26.2 Why Backend Developers Need Threading Concepts

You need threading knowledge because backend bugs often appear only under load.

Examples:

- Two users book the same seat.
- Two delivery partners accept the same order.
- Inventory goes below zero.
- Wallet transfer double-debits the sender.
- `@Async` method silently loses security/request context.
- A scheduled job runs twice and sends duplicate notifications.
- A coroutine calls blocking JDBC code on a reactive event-loop thread and slows the whole app.

Concurrency is not about making every function async. It is about protecting shared state while doing useful work in parallel.

---

## 26.3 Parallelism vs Concurrency

Concurrency:

```text
Multiple tasks are in progress during the same time period.
```

Parallelism:

```text
Multiple tasks are literally running at the same instant on different CPU cores.
```

Backend example:

```text
Concurrency:
  500 HTTP requests are open.
  Some wait for DB, some wait for Redis, some compute response JSON.

Parallelism:
  8 CPU cores are simultaneously running code.
```

Important lesson:

```text
Concurrency is about structure.
Parallelism is about hardware execution.
```

Coroutines help structure concurrency. They do not remove the need for threads.

---

## 26.4 Blocking vs Non-Blocking

Blocking code:

```kotlin
val user = userRepository.findById(userId).orElseThrow()
```

With JPA/JDBC, the thread waits while the database call is running.

Non-blocking code:

```kotlin
val user = userClient.get()
    .uri("/users/{id}", userId)
    .retrieve()
    .awaitBody<UserResponse>()
```

With WebClient + WebFlux + coroutines, the coroutine can suspend while the network call is waiting. The underlying thread can do other work.

Key rule:

```text
JPA/JDBC = blocking.
R2DBC/WebClient reactive APIs = non-blocking.
```

Do not call blocking JPA repositories from reactive event-loop threads.

---

## 26.5 Spring MVC Thread Model

Most Spring Boot apps use Spring MVC with embedded Tomcat.

Default mental model:

```text
HTTP request
  -> Tomcat worker thread
  -> Controller function
  -> Service function
  -> Repository function
  -> Database
  -> Response
```

Example:

```kotlin
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService
) {
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): OrderResponse {
        return orderService.createOrder(request)
    }
}
```

Service:

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository
) {
    @Transactional
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        val order = Order.create(request.userId, request.items)
        val saved = orderRepository.save(order)
        return saved.toResponse()
    }
}
```

This is blocking, thread-per-request style.

For a normal Spring MVC + PostgreSQL + JPA app, this is the default and easiest correct model.

---

## 26.6 Thread Safety

Thread safety means code behaves correctly when multiple threads use it at the same time.

Bad shared mutable state:

```kotlin
@Service
class BadCounterService {
    private var counter = 0

    fun next(): Int {
        counter += 1
        return counter
    }
}
```

Why this is unsafe:

```text
Thread A reads counter = 10
Thread B reads counter = 10
Thread A writes 11
Thread B writes 11
```

Expected result:

```text
12
```

Actual result:

```text
11
```

Better with `AtomicInteger`:

```kotlin
import java.util.concurrent.atomic.AtomicInteger

@Service
class SafeCounterService {
    private val counter = AtomicInteger(0)

    fun next(): Int {
        return counter.incrementAndGet()
    }
}
```

But in real backends, do not use in-memory counters for important business state.

For important state, use:

- PostgreSQL transaction.
- Unique constraint.
- Row lock.
- Optimistic locking.
- Redis atomic command.
- Message queue with idempotent consumer.

---

## 26.7 Why Singleton Spring Beans Must Avoid Mutable State

Most Spring beans are singletons.

That means this service object is shared across requests:

```kotlin
@Service
class CheckoutService {
    private var currentUserId: UUID? = null
}
```

This is dangerous.

Two requests can overwrite each other:

```text
Request A sets currentUserId = user-A
Request B sets currentUserId = user-B
Request A continues and accidentally uses user-B
```

Correct pattern:

```kotlin
@Service
class CheckoutService {
    fun checkout(userId: UUID, request: CheckoutRequest): CheckoutResponse {
        // Keep request data local to this function.
    }
}
```

Rule:

```text
Spring singleton beans should be stateless.
State belongs in local variables, database rows, cache entries or message payloads.
```

---

## 26.8 `synchronized` in Kotlin

Kotlin can use JVM synchronization.

Example:

```kotlin
class InventoryCounter {
    private var available = 10

    @Synchronized
    fun reserveOne(): Boolean {
        if (available <= 0) return false
        available -= 1
        return true
    }
}
```

This protects one object inside one JVM.

But in backend systems, this is usually not enough.

Why:

```text
Instance 1 has lock A.
Instance 2 has lock B.
Both connect to the same database.
Both can still modify the same product inventory.
```

Use `synchronized` only for local process state.

For real business state, use database or distributed coordination.

---

## 26.9 Database Transactions Are Also a Concurrency Tool

Backend concurrency is often solved in the database.

Example: prevent overselling inventory.

Bad:

```kotlin
@Transactional
fun reserve(productId: UUID, qty: Int) {
    val inventory = inventoryRepository.findByProductId(productId)
    if (inventory.available < qty) {
        throw ConflictException("Not enough stock")
    }
    inventory.available -= qty
}
```

This can fail under concurrency if two transactions read the same value before either writes.

Better with pessimistic lock:

```kotlin
interface InventoryRepository : JpaRepository<InventoryItem, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.productId = :productId")
    fun findByProductIdForUpdate(productId: UUID): InventoryItem?
}
```

Service:

```kotlin
@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository
) {
    @Transactional
    fun reserve(productId: UUID, qty: Int) {
        val inventory = inventoryRepository.findByProductIdForUpdate(productId)
            ?: throw NotFoundException("Inventory not found")

        if (inventory.available < qty) {
            throw ConflictException("Not enough stock")
        }

        inventory.available -= qty
        inventory.reserved += qty
    }
}
```

What happens:

```text
Transaction A locks inventory row.
Transaction B waits.
Transaction A updates and commits.
Transaction B reads updated value.
```

This is exactly the kind of idea used in parking spot assignment, ticket booking and wallet transfer.

---

## 26.10 Optimistic Locking

Optimistic locking assumes conflicts are rare.

Entity:

```kotlin
@Entity
class ProductInventory(
    @Id
    val id: UUID,

    val productId: UUID,

    var available: Int,

    var reserved: Int,

    @Version
    var version: Long = 0
)
```

Flow:

```text
Thread A reads version 5.
Thread B reads version 5.
Thread A updates row -> version becomes 6.
Thread B tries update with old version 5 -> fails.
```

Service with retry:

```kotlin
@Service
class OptimisticInventoryService(
    private val inventoryRepository: InventoryRepository
) {
    fun reserveWithRetry(productId: UUID, qty: Int) {
        repeat(3) {
            try {
                reserveOnce(productId, qty)
                return
            } catch (ex: ObjectOptimisticLockingFailureException) {
                // Retry because another request updated the row first.
            }
        }
        throw ConflictException("Inventory is busy. Please retry.")
    }

    @Transactional
    fun reserveOnce(productId: UUID, qty: Int) {
        val inventory = inventoryRepository.findByProductId(productId)
            ?: throw NotFoundException("Inventory not found")

        if (inventory.available < qty) {
            throw ConflictException("Not enough stock")
        }

        inventory.available -= qty
        inventory.reserved += qty
    }
}
```

Use optimistic locking when:

- Conflicts are uncommon.
- You want higher throughput.
- Retrying is acceptable.

Use pessimistic locking when:

- Conflicts are common.
- The operation must serialize access.
- Example: same seat, same parking spot, same wallet balance.

---

## 26.11 Spring `@Async`

`@Async` runs a method on a separate thread pool.

Use it for background work that does not need to block the HTTP response.

Examples:

- Send email.
- Send push notification.
- Generate report.
- Upload transformed image.
- Call slow analytics provider.

Enable async:

```kotlin
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean("applicationTaskExecutor")
    fun applicationTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 8
        executor.maxPoolSize = 32
        executor.queueCapacity = 500
        executor.setThreadNamePrefix("app-async-")
        executor.initialize()
        return executor
    }
}
```

Async service:

```kotlin
@Service
class NotificationService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async("applicationTaskExecutor")
    fun sendOrderConfirmation(orderId: UUID) {
        logger.info("Sending order confirmation for order={}", orderId)
        // send email or push notification
    }
}
```

Call from order service:

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val notificationService: NotificationService
) {
    @Transactional
    fun createOrder(command: CreateOrderCommand): OrderResponse {
        val order = orderRepository.save(Order.create(command))

        notificationService.sendOrderConfirmation(order.id)

        return order.toResponse()
    }
}
```

Important warning:

```text
The async method may run before or after the database transaction commits.
```

Better pattern:

```kotlin
@Component
class OrderEvents(
    private val notificationService: NotificationService
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderCreated(event: OrderCreatedEvent) {
        notificationService.sendOrderConfirmation(event.orderId)
    }
}
```

This ensures notification starts only after commit.

---

## 26.12 Common `@Async` Mistakes

Mistake 1: calling async method inside same class.

```kotlin
@Service
class BadService {
    fun create() {
        sendEmailAsync()
    }

    @Async
    fun sendEmailAsync() {
        // This will not be async when called from same class.
    }
}
```

Why:

```text
Spring @Async works through proxy interception.
Self-invocation bypasses the proxy.
```

Correct:

```kotlin
@Service
class OrderService(
    private val emailService: EmailService
) {
    fun create() {
        emailService.sendEmailAsync()
    }
}

@Service
class EmailService {
    @Async
    fun sendEmailAsync() {}
}
```

Mistake 2: assuming request context exists.

```kotlin
@Async
fun sendEmail() {
    // SecurityContext, MDC, request attributes may not automatically exist here.
}
```

Mistake 3: doing critical business state in async method without idempotency.

Bad:

```kotlin
@Async
fun debitWallet(orderId: UUID) {
    // dangerous if retried or executed twice
}
```

Better:

```text
Use transaction + idempotency key + ledger table.
```

---

## 26.13 Kotlin Coroutines: What They Are

A coroutine is a suspendable computation.

Simple example:

```kotlin
suspend fun fetchUser(userId: UUID): UserResponse {
    delay(100)
    return UserResponse(userId, "Shreyas")
}
```

`suspend` means:

```text
This function can pause without blocking its current thread.
```

But important:

```text
A coroutine is not a thread.
Coroutines run on threads.
Many coroutines can share fewer threads.
```

Kotlin official docs explain that concurrent coroutine code on the JVM still runs on operating-system threads.

---

## 26.14 Coroutine Mental Model

Normal blocking function:

```kotlin
fun loadProfile(userId: UUID): Profile {
    val user = userRepository.findById(userId).orElseThrow()
    val orders = orderRepository.findByUserId(userId)
    return Profile(user, orders)
}
```

The thread waits during DB calls.

Suspending function:

```kotlin
suspend fun loadProfile(userId: UUID): Profile {
    val user = userClient.getUser(userId)
    val orders = orderClient.getOrders(userId)
    return Profile(user, orders)
}
```

If `userClient` and `orderClient` use non-blocking APIs, the coroutine can suspend without occupying a thread while waiting.

---

## 26.15 Can Coroutines Be Used in Spring Boot?

Yes.

But the right usage depends on your stack.

### Good Fit

Coroutines are a good fit when using:

- Spring WebFlux.
- WebClient.
- R2DBC.
- Reactive Redis.
- Reactive MongoDB.
- Kotlin `Flow`.
- `suspend fun` controllers.

Example WebFlux coroutine controller:

```kotlin
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val productService: ProductService
) {
    @GetMapping("/{id}")
    suspend fun getProduct(@PathVariable id: UUID): ProductResponse {
        return productService.getProduct(id)
    }
}
```

Service:

```kotlin
@Service
class ProductService(
    private val productRepository: ProductRepository
) {
    suspend fun getProduct(id: UUID): ProductResponse {
        val product = productRepository.findById(id)
            ?: throw NotFoundException("Product not found")
        return product.toResponse()
    }
}
```

Repository with R2DBC-style coroutine support:

```kotlin
interface ProductRepository : CoroutineCrudRepository<Product, UUID>
```

### Not Automatically Useful

Coroutines are not automatically useful when using:

- Spring MVC.
- JPA.
- JDBC.
- Blocking Redis client.
- Blocking HTTP client.

This code is suspicious:

```kotlin
suspend fun getOrder(id: UUID): OrderResponse {
    val order = orderRepository.findById(id).orElseThrow()
    return order.toResponse()
}
```

Why suspicious:

```text
The function is suspend, but the repository call is still blocking.
You did not make the database non-blocking.
You only changed the function signature.
```

---

## 26.16 Spring MVC + Coroutines

Spring has coroutine support, but you should not treat `suspend` as magic.

In classic MVC with JPA:

```kotlin
@GetMapping("/{id}")
suspend fun getOrder(@PathVariable id: UUID): OrderResponse {
    return orderService.getOrder(id)
}
```

If `orderService.getOrder` calls JPA, the actual database work is still blocking.

Better beginner rule:

```text
If your app is Spring MVC + JPA, use normal functions first.
Learn transactions, locks, async executors and queues before adding coroutines.
```

Use coroutines in Spring MVC only when you have a specific reason, such as:

- Calling several non-blocking remote APIs.
- Working with coroutine libraries.
- Preparing to move a module to WebFlux.

---

## 26.17 WebFlux + Coroutines

WebFlux is Spring's reactive web stack.

Without coroutines, WebFlux often uses `Mono` and `Flux`:

```kotlin
fun getProduct(id: UUID): Mono<ProductResponse>
fun listProducts(): Flux<ProductResponse>
```

With coroutines:

```kotlin
suspend fun getProduct(id: UUID): ProductResponse
fun listProducts(): Flow<ProductResponse>
```

Mental translation:

```text
Mono<T>     -> suspend fun returns T
Mono<Void>  -> suspend fun returns Unit
Flux<T>     -> Flow<T>
```

Example:

```kotlin
@RestController
class CatalogController(
    private val catalogService: CatalogService
) {
    @GetMapping("/api/v1/catalog/{id}")
    suspend fun get(@PathVariable id: UUID): ProductResponse {
        return catalogService.get(id)
    }

    @GetMapping("/api/v1/catalog")
    fun list(): Flow<ProductResponse> {
        return catalogService.list()
    }
}
```

Service:

```kotlin
@Service
class CatalogService(
    private val repository: ProductCoroutineRepository
) {
    suspend fun get(id: UUID): ProductResponse {
        val product = repository.findById(id)
            ?: throw NotFoundException("Product not found")
        return product.toResponse()
    }

    fun list(): Flow<ProductResponse> {
        return repository.findAll().map { it.toResponse() }
    }
}
```

---

## 26.18 Coroutine Dependencies in Spring Boot

For coroutine support:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}
```

Spring Boot manages coroutine versions, so you usually do not need to manually specify the coroutine version.

For R2DBC:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
}
```

For classic JPA:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
}
```

Do not mix these casually.

```text
JPA is blocking.
R2DBC is reactive/non-blocking.
```

---

## 26.19 Structured Concurrency

Bad coroutine style:

```kotlin
fun createOrder(command: CreateOrderCommand) {
    GlobalScope.launch {
        sendEmail(command.userId)
    }
}
```

Why bad:

- No lifecycle ownership.
- Hard to cancel.
- Hard to test.
- Exceptions can disappear.
- Work may continue after request/application shutdown.

Better:

```kotlin
suspend fun buildCheckoutPage(userId: UUID): CheckoutPage =
    coroutineScope {
        val cart = async { cartClient.getCart(userId) }
        val address = async { addressClient.getDefaultAddress(userId) }
        val offers = async { offerClient.getOffers(userId) }

        CheckoutPage(
            cart = cart.await(),
            address = address.await(),
            offers = offers.await()
        )
    }
```

Explanation:

```text
coroutineScope creates a parent scope.
async starts child coroutines.
await waits for result.
If one child fails, the scope cancels the other children.
```

Use this for parallel remote calls.

Do not use this to make blocking JPA calls faster unless you intentionally run blocking work on `Dispatchers.IO` and understand the tradeoff.

---

## 26.20 Calling Multiple Remote APIs Concurrently

Sequential:

```kotlin
suspend fun homepage(userId: UUID): HomePageResponse {
    val profile = profileClient.getProfile(userId)
    val orders = orderClient.getRecentOrders(userId)
    val recommendations = recommendationClient.getRecommendations(userId)

    return HomePageResponse(profile, orders, recommendations)
}
```

If each call takes 200 ms:

```text
Total roughly 600 ms.
```

Concurrent:

```kotlin
suspend fun homepage(userId: UUID): HomePageResponse =
    coroutineScope {
        val profile = async { profileClient.getProfile(userId) }
        val orders = async { orderClient.getRecentOrders(userId) }
        val recommendations = async { recommendationClient.getRecommendations(userId) }

        HomePageResponse(
            profile = profile.await(),
            orders = orders.await(),
            recommendations = recommendations.await()
        )
    }
```

If each call takes 200 ms:

```text
Total roughly 200 ms plus overhead.
```

This is a great coroutine use case.

---

## 26.21 Coroutine Dispatchers

Dispatcher decides which thread or thread pool runs coroutine work.

Common dispatchers:

```text
Dispatchers.Default -> CPU-heavy work
Dispatchers.IO      -> blocking IO work
Dispatchers.Main    -> UI thread, mostly Android/Desktop
```

Backend example:

```kotlin
suspend fun resizeImage(bytes: ByteArray): ByteArray =
    withContext(Dispatchers.Default) {
        imageResizer.resize(bytes)
    }
```

Blocking call bridge:

```kotlin
suspend fun loadFromBlockingClient(id: UUID): ExternalUser =
    withContext(Dispatchers.IO) {
        blockingExternalClient.loadUser(id)
    }
```

Important:

```text
withContext(Dispatchers.IO) does not make blocking code non-blocking.
It moves blocking work to a thread pool designed for blocking IO.
```

This can be useful, but it is not the same as using a truly non-blocking client.

---

## 26.22 Coroutines and Transactions

Classic Spring transaction:

```kotlin
@Transactional
fun createOrder(command: CreateOrderCommand): Order {
    val order = orderRepository.save(Order.create(command))
    inventoryService.reserve(order.id)
    return order
}
```

This is the standard Spring MVC + JPA model.

For WebFlux/R2DBC coroutines, official Spring docs show programmatic reactive transactions.

Pattern:

```kotlin
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

@Service
class ReactiveOrderService(
    private val tx: TransactionalOperator,
    private val orderRepository: OrderCoroutineRepository,
    private val inventoryRepository: InventoryCoroutineRepository
) {
    suspend fun createOrder(command: CreateOrderCommand): OrderResponse {
        return tx.executeAndAwait {
            val order = orderRepository.save(Order.create(command))
            inventoryRepository.reserve(order.id)
            order.toResponse()
        } ?: error("Transaction returned no result")
    }
}
```

Important rule:

```text
Spring coroutine transactions are strongest with reactive transaction management.
For MVC + JPA, prefer normal @Transactional blocking service methods.
```

---

## 26.23 Kotlin Flow in Spring

`Flow<T>` is Kotlin's asynchronous stream type.

Use it when you return many values over time.

Example:

```kotlin
@GetMapping("/api/v1/orders/stream")
fun streamOrders(): Flow<OrderResponse> {
    return orderService.streamRecentOrders()
}
```

Service:

```kotlin
fun streamRecentOrders(): Flow<OrderResponse> {
    return orderRepository.findRecentOrders()
        .map { it.toResponse() }
}
```

Good use cases:

- Streaming large result sets.
- Server-sent events.
- Reactive data pipelines.
- Processing messages.

For normal paginated REST APIs, a simple `List<T>` is often easier.

---

## 26.24 Thread Pools in Spring Boot

Thread pool controls how many tasks can run.

Example:

```kotlin
@Configuration
class ExecutorConfig {
    @Bean("reportExecutor")
    fun reportExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 4
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("report-")
        executor.initialize()
        return executor
    }
}
```

Meaning:

```text
corePoolSize  -> normal number of worker threads
maxPoolSize   -> maximum threads during pressure
queueCapacity -> tasks waiting before rejection
```

Do not set huge pools blindly.

Bad:

```kotlin
executor.maxPoolSize = 1000
```

Why:

- Too many threads increase memory usage.
- Context switching increases.
- Database connection pool becomes bottleneck.
- External providers may rate-limit you.

Thread pool sizing must match downstream capacity.

Example:

```text
DB connection pool = 20
Async workers doing DB work = 200
```

This is bad. 200 workers will fight for 20 connections.

---

## 26.25 Scheduled Jobs

Spring scheduled job:

```kotlin
@Component
class ExpiredHoldReleaseJob(
    private val bookingService: BookingService
) {
    @Scheduled(fixedDelay = 60_000)
    fun releaseExpiredHolds() {
        bookingService.releaseExpiredHolds()
    }
}
```

Enable scheduling:

```kotlin
@SpringBootApplication
@EnableScheduling
class BookingApplication
```

Important production warning:

```text
If you run 3 app instances, the scheduled job may run on all 3 instances.
```

Solutions:

- Make job idempotent.
- Use DB locks.
- Use ShedLock.
- Use Kubernetes CronJob.
- Use queue consumer with single partition.

Idempotent job example:

```kotlin
@Transactional
fun expireOldHolds(now: Instant) {
    val holds = holdRepository.findActiveExpiredHolds(now)
    holds.forEach { hold ->
        if (hold.status == HoldStatus.ACTIVE) {
            hold.expire()
        }
    }
}
```

If it runs twice, second run should do nothing harmful.

---

## 26.26 Race Conditions

Race condition means behavior depends on timing.

Ticket booking race:

```text
User A checks seat A1 -> available
User B checks seat A1 -> available
User A books A1
User B books A1
```

Correctness tools:

- Unique constraint.
- Pessimistic lock.
- Optimistic lock.
- Atomic update.
- Idempotency key.

Atomic SQL pattern:

```sql
update seats
set status = 'HELD'
where id = :seatId
  and status = 'AVAILABLE';
```

Then check affected row count:

```text
1 row updated -> success
0 rows updated -> someone else got it
```

This is often better than doing read-then-write.

---

## 26.27 Deadlocks

Deadlock means two transactions wait for each other forever until the database kills one.

Example:

```text
Transaction A locks wallet 1, then wants wallet 2.
Transaction B locks wallet 2, then wants wallet 1.
```

Solution: always lock in deterministic order.

```kotlin
@Transactional
fun transfer(fromWalletId: UUID, toWalletId: UUID, amount: BigDecimal) {
    val orderedIds = listOf(fromWalletId, toWalletId).sorted()

    val first = walletRepository.findByIdForUpdate(orderedIds[0])
    val second = walletRepository.findByIdForUpdate(orderedIds[1])

    val from = if (first.id == fromWalletId) first else second
    val to = if (second.id == toWalletId) second else first

    from.debit(amount)
    to.credit(amount)
}
```

Rule:

```text
When locking multiple rows, lock them in the same order everywhere.
```

---

## 26.28 Idempotency and Concurrency

Idempotency means retrying the same operation does not create duplicate side effects.

Payment example:

```http
POST /api/v1/payments
Idempotency-Key: user-123-order-456-payment
```

Table:

```sql
create table idempotency_keys (
  id uuid primary key,
  idempotency_key varchar(120) not null unique,
  request_hash varchar(120) not null,
  response_json jsonb,
  status varchar(30) not null,
  created_at timestamptz not null
);
```

Service pattern:

```kotlin
@Transactional
fun createPayment(command: CreatePaymentCommand): PaymentResponse {
    val existing = idempotencyRepository.findByKey(command.idempotencyKey)
    if (existing != null) {
        return existing.response
    }

    val payment = paymentRepository.save(Payment.create(command))
    val response = payment.toResponse()

    idempotencyRepository.save(
        IdempotencyRecord(
            key = command.idempotencyKey,
            requestHash = command.hash(),
            response = response
        )
    )

    return response
}
```

Use idempotency for:

- Payments.
- Wallet transfer.
- Order creation.
- Booking confirmation.
- Webhook handling.
- Message consumers.

---

## 26.29 Coroutines vs `@Async`

Use `@Async` when:

- You are in Spring MVC.
- You want fire-and-forget background work.
- You have a bounded thread pool.
- You do not need structured concurrency in the request.

Use coroutines when:

- You are using WebFlux/R2DBC.
- You call multiple non-blocking remote APIs.
- You need `suspend fun` APIs.
- You process asynchronous streams with `Flow`.

Comparison:

| Concept | Best For | Thread Usage | Spring Fit |
|---|---|---|---|
| Normal MVC | CRUD/JPA apps | One thread per request | Excellent |
| `@Async` | Background tasks | Separate executor thread | Excellent |
| Coroutines | Non-blocking async code | Many coroutines on fewer threads | Best with WebFlux/R2DBC |
| WebFlux `Mono`/`Flux` | Reactive pipelines | Event-loop model | Excellent but harder |
| Queue workers | Reliable async business workflows | Consumer threads | Production-grade |

---

## 26.30 Should You Use Coroutines in Your Spring Boot Projects?

For your current learning path:

```text
Step 1:
  Learn normal Spring MVC + JPA deeply.

Step 2:
  Learn transactions, locks, idempotency and @Async.

Step 3:
  Learn WebClient and external API calls.

Step 4:
  Learn coroutines for concurrent remote calls.

Step 5:
  Learn WebFlux + R2DBC only when you intentionally want non-blocking architecture.
```

Recommended beginner decision:

```text
Parking lot app:
  Normal MVC + JPA + pessimistic locks.

Ticket booking app:
  Normal MVC + JPA + locks + idempotency.

Food ordering app:
  Normal MVC + JPA + @Async/event listener for notifications.

Wallet ledger app:
  Normal MVC + JPA + transactions + row locks.

External aggregator app:
  Coroutines are useful for parallel API calls.
```

---

## 26.31 Real Example: Parallel Quote Aggregation With Coroutines

Imagine a checkout quote needs:

- Cart service.
- Address service.
- Coupon service.
- Delivery fee service.

Coroutine version:

```kotlin
@Service
class CheckoutQuoteService(
    private val cartClient: CartClient,
    private val addressClient: AddressClient,
    private val couponClient: CouponClient,
    private val deliveryClient: DeliveryClient
) {
    suspend fun quote(userId: UUID, couponCode: String?): CheckoutQuote =
        coroutineScope {
            val cartDeferred = async { cartClient.getCart(userId) }
            val addressDeferred = async { addressClient.getDefaultAddress(userId) }
            val couponDeferred = async { couponCode?.let { couponClient.validate(it, userId) } }

            val cart = cartDeferred.await()
            val address = addressDeferred.await()
            val coupon = couponDeferred.await()

            val deliveryFee = deliveryClient.calculateFee(
                restaurantId = cart.restaurantId,
                addressId = address.id
            )

            CheckoutQuote(
                subtotal = cart.subtotal,
                discount = coupon?.discount ?: Money.zero("INR"),
                deliveryFee = deliveryFee,
                total = cart.subtotal - (coupon?.discount ?: Money.zero("INR")) + deliveryFee
            )
        }
}
```

Why this is good:

```text
Cart, address and coupon are independent.
They can be fetched concurrently.
Delivery fee depends on cart and address, so it runs after those are available.
```

---

## 26.32 Real Example: Do Not Use Coroutines for Wallet Transfer

Bad idea:

```kotlin
suspend fun transfer(from: UUID, to: UUID, amount: BigDecimal) = coroutineScope {
    val debit = async { walletRepository.debit(from, amount) }
    val credit = async { walletRepository.credit(to, amount) }

    debit.await()
    credit.await()
}
```

Why bad:

```text
Debit and credit are one atomic business transaction.
They should not be independent concurrent tasks.
```

Correct:

```kotlin
@Transactional
fun transfer(from: UUID, to: UUID, amount: BigDecimal) {
    val fromWallet = walletRepository.findByIdForUpdate(from)
    val toWallet = walletRepository.findByIdForUpdate(to)

    fromWallet.debit(amount)
    toWallet.credit(amount)

    ledgerRepository.saveBalancedTransfer(fromWallet, toWallet, amount)
}
```

Concurrency lesson:

```text
Use coroutines to parallelize independent waiting.
Use transactions to protect one atomic business invariant.
```

---

## 26.33 Real Example: `@Async` After Commit

Order notification:

```kotlin
data class OrderCreatedEvent(val orderId: UUID, val userId: UUID)
```

Order service:

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val publisher: ApplicationEventPublisher
) {
    @Transactional
    fun createOrder(command: CreateOrderCommand): OrderResponse {
        val order = orderRepository.save(Order.create(command))
        publisher.publishEvent(OrderCreatedEvent(order.id, order.userId))
        return order.toResponse()
    }
}
```

Listener:

```kotlin
@Component
class OrderNotificationListener(
    private val notificationService: NotificationService
) {
    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderCreated(event: OrderCreatedEvent) {
        notificationService.sendOrderCreated(event.orderId, event.userId)
    }
}
```

This gives you:

```text
Transaction safety from AFTER_COMMIT.
Non-blocking HTTP response from @Async.
Clear separation of order creation and notification.
```

---

## 26.34 Testing Concurrent Code

Test race conditions explicitly.

Example:

```kotlin
@Test
fun `same seat cannot be booked twice`() {
    val seatId = seedAvailableSeat()
    val executor = Executors.newFixedThreadPool(2)
    val start = CountDownLatch(1)

    val tasks = (1..2).map {
        executor.submit<Boolean> {
            start.await()
            try {
                bookingService.holdSeat(seatId)
                true
            } catch (ex: ConflictException) {
                false
            }
        }
    }

    start.countDown()

    val results = tasks.map { it.get() }

    assertThat(results.count { it }).isEqualTo(1)
    assertThat(results.count { !it }).isEqualTo(1)
}
```

What this test does:

```text
Creates two threads.
Releases both at the same time.
Both try to hold the same seat.
Only one should succeed.
```

Concurrency tests are not always perfectly deterministic, but they catch many design mistakes.

---

## 26.35 Practical Backend Decision Matrix

Use this:

| Problem | Best Tool |
|---|---|
| Normal CRUD API | Spring MVC + JPA |
| Send email after order | `@Async` + after-commit event |
| Reserve inventory | DB transaction + row lock |
| Prevent duplicate payment | Idempotency key + unique constraint |
| Handle payment webhook | Idempotent consumer |
| Run cleanup every minute | `@Scheduled` + idempotent job |
| Fetch 4 remote APIs together | Coroutines + WebClient |
| Stream many events | Flow/WebFlux or message queue |
| CPU-heavy work | Bounded executor or `Dispatchers.Default` |
| Blocking IO bridge | `Dispatchers.IO` or dedicated executor |
| Reliable async workflow | Kafka/RabbitMQ + outbox pattern |

---

## 26.36 What To Remember

1. Threads are real OS execution units.
2. Coroutines are suspendable computations that run on threads.
3. `suspend` does not magically make blocking JPA non-blocking.
4. Spring MVC + JPA should usually use normal blocking functions.
5. WebFlux + R2DBC is where coroutines become very powerful.
6. `@Async` is useful for background work in Spring MVC.
7. Protect business state with transactions, locks, constraints and idempotency.
8. Do not store request state in singleton Spring beans.
9. Parallelize independent waiting, not one atomic business operation.
10. For production async workflows, queues and outbox patterns are more reliable than fire-and-forget.

Concurrency is not a decoration you add to code. It is a correctness and capacity design decision.
