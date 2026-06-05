package com.learning.food.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class FoodOrderTest {
    @Test
    fun `order moves through valid state`() {
        val order = FoodOrder(userId = UUID.randomUUID(), restaurantId = UUID.randomUUID(), idempotencyKey = "k1", subtotalCents = 1000, deliveryFeeCents = 100, totalCents = 1100)
        order.moveTo(OrderStatus.PAYMENT_CONFIRMED)
        assertThat(order.status).isEqualTo(OrderStatus.PAYMENT_CONFIRMED)
    }

    @Test
    fun `order rejects invalid transition`() {
        val order = FoodOrder(userId = UUID.randomUUID(), restaurantId = UUID.randomUUID(), idempotencyKey = "k2", subtotalCents = 1000, deliveryFeeCents = 100, totalCents = 1100)
        assertThatThrownBy { order.moveTo(OrderStatus.DELIVERED) }.isInstanceOf(IllegalStateException::class.java)
    }
}
