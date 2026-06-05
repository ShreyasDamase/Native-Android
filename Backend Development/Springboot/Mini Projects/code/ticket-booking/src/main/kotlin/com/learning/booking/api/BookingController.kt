package com.learning.booking.api

import com.learning.booking.application.BookingService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateHoldRequest(val eventId: UUID, @field:NotEmpty val seatIds: List<UUID>)
data class CreateBookingRequest(val holdId: UUID)

@RestController
@RequestMapping("/api/v1")
class BookingController(private val service: BookingService) {
    @GetMapping("/events")
    fun events() = service.listEvents()

    @GetMapping("/events/{eventId}/seats")
    fun seats(@PathVariable eventId: UUID) = service.seatsFor(eventId)

    @PostMapping("/seat-holds")
    @ResponseStatus(HttpStatus.CREATED)
    fun createHold(@Valid @RequestBody request: CreateHoldRequest) = service.createHold(request.eventId, request.seatIds)

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBooking(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: CreateBookingRequest
    ) = service.confirmBooking(request.holdId, idempotencyKey)
}
