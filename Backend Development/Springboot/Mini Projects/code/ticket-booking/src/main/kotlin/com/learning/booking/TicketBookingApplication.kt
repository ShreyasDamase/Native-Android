package com.learning.booking

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TicketBookingApplication

fun main(args: Array<String>) {
    runApplication<TicketBookingApplication>(*args)
}
