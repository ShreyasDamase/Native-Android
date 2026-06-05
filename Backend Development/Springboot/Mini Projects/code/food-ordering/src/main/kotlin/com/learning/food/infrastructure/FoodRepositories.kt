package com.learning.food.infrastructure

import com.learning.food.domain.AppUser
import com.learning.food.domain.CartItem
import com.learning.food.domain.FoodOrder
import com.learning.food.domain.MenuItem
import com.learning.food.domain.Restaurant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<AppUser, UUID> {
    fun findByEmail(email: String): Optional<AppUser>
}

interface RestaurantRepository : JpaRepository<Restaurant, UUID>

interface MenuItemRepository : JpaRepository<MenuItem, UUID> {
    fun findByRestaurantIdAndActiveTrue(restaurantId: UUID): List<MenuItem>
}

interface CartItemRepository : JpaRepository<CartItem, UUID> {
    fun findByUserId(userId: UUID): List<CartItem>
    fun deleteByUserId(userId: UUID)
}

interface FoodOrderRepository : JpaRepository<FoodOrder, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): Optional<FoodOrder>
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<FoodOrder>
}
