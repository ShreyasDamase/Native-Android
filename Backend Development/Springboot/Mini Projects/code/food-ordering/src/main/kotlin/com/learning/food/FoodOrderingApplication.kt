package com.learning.food

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FoodOrderingApplication

fun main(args: Array<String>) {
    runApplication<FoodOrderingApplication>(*args)
}
