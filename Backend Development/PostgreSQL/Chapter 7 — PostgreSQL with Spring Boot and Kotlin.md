# Chapter 7 — PostgreSQL with Spring Boot and Kotlin

### _JPA, repositories, transactions and production configuration_

---

## 7.1 Dependencies

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
}
```

---

## 7.2 Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:app}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 2000

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  flyway:
    enabled: true
```

---

## 7.3 Entity Example

```kotlin
@Entity
@Table(name = "orders")
class Order(
    @Id
    val id: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus,

    @Column(name = "total_amount_cents", nullable = false)
    val totalAmountCents: Long,

    @Version
    var version: Long? = null
)
```

Use:

- `@Enumerated(EnumType.STRING)`, not ordinal.
- `@Version` for optimistic locking.
- UUID ids for business entities.

---

## 7.4 Repository Example

```kotlin
interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByUserIdAndIdempotencyKey(userId: UUID, idempotencyKey: String): Order?

    @Query("""
        select o from Order o
        where o.userId = :userId
        order by o.createdAt desc
    """)
    fun findRecent(userId: UUID, pageable: Pageable): Page<Order>
}
```

---

## 7.5 Transaction Example

```kotlin
@Service
class PlaceOrderUseCase(
    private val orderRepository: OrderRepository
) {
    @Transactional
    fun place(command: PlaceOrderCommand): OrderResponse {
        orderRepository.findByUserIdAndIdempotencyKey(command.userId, command.idempotencyKey)
            ?.let { return OrderResponse.from(it) }

        val order = Order.place(command)
        return OrderResponse.from(orderRepository.save(order))
    }
}
```

Keep transactions at use-case/service level, not controller level.

