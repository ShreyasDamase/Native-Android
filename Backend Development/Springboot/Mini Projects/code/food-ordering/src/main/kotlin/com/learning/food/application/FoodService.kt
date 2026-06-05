package com.learning.food.application

import com.learning.food.common.ConflictException
import com.learning.food.common.NotFoundException
import com.learning.food.common.UnauthorizedException
import com.learning.food.domain.AppUser
import com.learning.food.domain.CartItem
import com.learning.food.domain.FoodOrder
import com.learning.food.domain.FoodOrderItem
import com.learning.food.domain.OrderStatus
import com.learning.food.infrastructure.CartItemRepository
import com.learning.food.infrastructure.FoodOrderRepository
import com.learning.food.infrastructure.MenuItemRepository
import com.learning.food.infrastructure.RestaurantRepository
import com.learning.food.infrastructure.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class UserView(val userId: UUID, val email: String, val token: String)
data class CartView(val userId: UUID, val restaurantId: UUID?, val items: List<CartLine>, val subtotalCents: Long)
data class CartLine(val menuItemId: UUID, val name: String, val quantity: Int, val priceCents: Long)
data class OrderView(val orderId: UUID, val status: OrderStatus, val totalCents: Long, val items: List<CartLine>)

@Service
class FoodService(
    private val users: UserRepository,
    private val restaurants: RestaurantRepository,
    private val menuItems: MenuItemRepository,
    private val cartItems: CartItemRepository,
    private val orders: FoodOrderRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    fun register(email: String, password: String): UserView {
        if (users.findByEmail(email).isPresent) throw ConflictException("Email already registered")
        val user = users.save(AppUser(email = email.lowercase(), passwordHash = passwordEncoder.encode(password)))
        return user.toView()
    }

    @Transactional(readOnly = true)
    fun login(email: String, password: String): UserView {
        val user = users.findByEmail(email.lowercase()).orElseThrow { UnauthorizedException("Invalid credentials") }
        if (!passwordEncoder.matches(password, user.passwordHash)) throw UnauthorizedException("Invalid credentials")
        return user.toView()
    }

    @Transactional(readOnly = true)
    fun restaurants() = restaurants.findAll().map { mapOf("restaurantId" to it.id, "name" to it.name, "open" to it.open) }

    @Transactional(readOnly = true)
    fun menu(restaurantId: UUID) = menuItems.findByRestaurantIdAndActiveTrue(restaurantId)
        .map { mapOf("menuItemId" to it.id, "name" to it.name, "priceCents" to it.priceCents) }

    @Transactional
    fun addCartItem(userId: UUID, menuItemId: UUID, quantity: Int): CartView {
        val item = menuItems.findById(menuItemId).orElseThrow { NotFoundException("Menu item not found") }
        if (!item.active) throw ConflictException("Menu item is inactive")

        val existingCart = cartItems.findByUserId(userId)
        if (existingCart.isNotEmpty() && existingCart.first().restaurantId != item.restaurantId) {
            throw ConflictException("Cart can contain items from only one restaurant")
        }

        val existing = existingCart.firstOrNull { it.menuItem.id == menuItemId }
        if (existing == null) {
            cartItems.save(CartItem(userId = userId, restaurantId = item.restaurantId, menuItem = item, quantity = quantity))
        } else {
            existing.quantity += quantity
        }
        return cart(userId)
    }

    @Transactional(readOnly = true)
    fun cart(userId: UUID): CartView {
        val lines = cartItems.findByUserId(userId)
        val cartLines = lines.map { CartLine(it.menuItem.id, it.menuItem.name, it.quantity, it.menuItem.priceCents) }
        return CartView(userId, lines.firstOrNull()?.restaurantId, cartLines, cartLines.sumOf { it.priceCents * it.quantity })
    }

    @Transactional(readOnly = true)
    fun quote(userId: UUID): Map<String, Long> {
        val subtotal = cart(userId).subtotalCents
        if (subtotal == 0L) throw ConflictException("Cart is empty")
        val deliveryFee = 4_000L
        return mapOf("subtotalCents" to subtotal, "deliveryFeeCents" to deliveryFee, "totalCents" to subtotal + deliveryFee)
    }

    @Transactional
    fun createOrder(userId: UUID, idempotencyKey: String): OrderView {
        val existing = orders.findByIdempotencyKey(idempotencyKey)
        if (existing.isPresent) return existing.get().toView()
        val cart = cartItems.findByUserId(userId)
        if (cart.isEmpty()) throw ConflictException("Cart is empty")
        val restaurant = restaurants.findById(cart.first().restaurantId).orElseThrow { NotFoundException("Restaurant not found") }
        if (!restaurant.open) throw ConflictException("Restaurant is closed")

        val subtotal = cart.sumOf { it.menuItem.priceCents * it.quantity }
        val order = FoodOrder(
            userId = userId,
            restaurantId = restaurant.id,
            idempotencyKey = idempotencyKey,
            subtotalCents = subtotal,
            deliveryFeeCents = 4_000,
            totalCents = subtotal + 4_000
        )
        cart.forEach {
            order.items.add(FoodOrderItem(order = order, menuItemId = it.menuItem.id, name = it.menuItem.name, quantity = it.quantity, priceCents = it.menuItem.priceCents))
        }
        val saved = orders.save(order)
        cartItems.deleteByUserId(userId)
        return saved.toView()
    }

    @Transactional
    fun updateOrderStatus(orderId: UUID, next: OrderStatus): OrderView {
        val order = orders.findById(orderId).orElseThrow { NotFoundException("Order not found") }
        order.moveTo(next)
        return order.toView()
    }

    @Transactional(readOnly = true)
    fun userOrders(userId: UUID) = orders.findByUserIdOrderByCreatedAtDesc(userId).map { it.toView() }
}

fun AppUser.toView() = UserView(id, email, "demo-token-$id")

fun FoodOrder.toView() = OrderView(
    orderId = id,
    status = status,
    totalCents = totalCents,
    items = items.map { CartLine(it.menuItemId, it.name, it.quantity, it.priceCents) }
)
