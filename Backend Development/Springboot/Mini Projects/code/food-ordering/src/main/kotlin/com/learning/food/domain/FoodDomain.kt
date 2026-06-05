package com.learning.food.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class UserRole { CUSTOMER, RESTAURANT_OWNER, ADMIN }
enum class OrderStatus { CREATED, PAYMENT_CONFIRMED, ACCEPTED_BY_RESTAURANT, PREPARING, READY_FOR_PICKUP, PICKED_UP, DELIVERED, CANCELLED }

@Entity
@Table(name = "app_users")
class AppUser(
    @Id val id: UUID = UUID.randomUUID(),
    val email: String,
    val passwordHash: String,
    @Enumerated(EnumType.STRING) val role: UserRole = UserRole.CUSTOMER
)

@Entity
@Table(name = "restaurants")
class Restaurant(
    @Id val id: UUID = UUID.randomUUID(),
    val name: String,
    var open: Boolean = true
)

@Entity
@Table(name = "menu_items")
class MenuItem(
    @Id val id: UUID = UUID.randomUUID(),
    val restaurantId: UUID,
    val name: String,
    val priceCents: Long,
    val active: Boolean = true
)

@Entity
@Table(name = "cart_items")
class CartItem(
    @Id val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val restaurantId: UUID,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "menu_item_id") val menuItem: MenuItem,
    var quantity: Int
)

@Entity
@Table(name = "food_orders")
class FoodOrder(
    @Id val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val restaurantId: UUID,
    val idempotencyKey: String,
    @Enumerated(EnumType.STRING) var status: OrderStatus = OrderStatus.CREATED,
    val subtotalCents: Long,
    val deliveryFeeCents: Long,
    val totalCents: Long,
    val createdAt: Instant = Instant.now(),
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<FoodOrderItem> = mutableListOf()
) {
    fun moveTo(next: OrderStatus) {
        val allowed = when (status) {
            OrderStatus.CREATED -> setOf(OrderStatus.PAYMENT_CONFIRMED, OrderStatus.CANCELLED)
            OrderStatus.PAYMENT_CONFIRMED -> setOf(OrderStatus.ACCEPTED_BY_RESTAURANT, OrderStatus.CANCELLED)
            OrderStatus.ACCEPTED_BY_RESTAURANT -> setOf(OrderStatus.PREPARING)
            OrderStatus.PREPARING -> setOf(OrderStatus.READY_FOR_PICKUP)
            OrderStatus.READY_FOR_PICKUP -> setOf(OrderStatus.PICKED_UP)
            OrderStatus.PICKED_UP -> setOf(OrderStatus.DELIVERED)
            OrderStatus.DELIVERED, OrderStatus.CANCELLED -> emptySet()
        }
        check(next in allowed) { "Cannot move order from $status to $next" }
        status = next
    }
}

@Entity
@Table(name = "food_order_items")
class FoodOrderItem(
    @Id val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") val order: FoodOrder,
    val menuItemId: UUID,
    val name: String,
    val quantity: Int,
    val priceCents: Long
)
