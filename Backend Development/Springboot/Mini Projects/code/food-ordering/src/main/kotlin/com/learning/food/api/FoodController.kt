package com.learning.food.api

import com.learning.food.application.FoodService
import com.learning.food.domain.OrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class AuthRequest(@field:Email val email: String, @field:Size(min = 6) val password: String)
data class AddCartItemRequest(val userId: UUID, val menuItemId: UUID, @field:Min(1) val quantity: Int)
data class UserRequest(val userId: UUID)
data class UpdateOrderStatusRequest(val status: OrderStatus)

@RestController
@RequestMapping("/api/v1")
class FoodController(private val service: FoodService) {
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: AuthRequest) = service.register(request.email, request.password)

    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody request: AuthRequest) = service.login(request.email, request.password)

    @GetMapping("/restaurants")
    fun restaurants() = service.restaurants()

    @GetMapping("/restaurants/{restaurantId}/menu")
    fun menu(@PathVariable restaurantId: UUID) = service.menu(restaurantId)

    @PostMapping("/cart/items")
    fun addCartItem(@Valid @RequestBody request: AddCartItemRequest) =
        service.addCartItem(request.userId, request.menuItemId, request.quantity)

    @GetMapping("/cart/{userId}")
    fun cart(@PathVariable userId: UUID) = service.cart(userId)

    @PostMapping("/checkout/quote")
    fun quote(@RequestBody request: UserRequest) = service.quote(request.userId)

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: UserRequest
    ) = service.createOrder(request.userId, idempotencyKey)

    @PatchMapping("/orders/{orderId}/status")
    fun updateStatus(@PathVariable orderId: UUID, @RequestBody request: UpdateOrderStatusRequest) =
        service.updateOrderStatus(orderId, request.status)

    @GetMapping("/users/{userId}/orders")
    fun userOrders(@PathVariable userId: UUID) = service.userOrders(userId)
}
