package com.learning.parking.api

import com.learning.parking.application.CreateTicketCommand
import com.learning.parking.application.ParkingService
import com.learning.parking.domain.VehicleType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateTicketRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Za-z0-9-]{4,20}$")
    val vehicleNumber: String,
    val vehicleType: VehicleType
)

@RestController
@RequestMapping("/api/v1/parking")
class ParkingController(private val service: ParkingService) {
    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTicket(@Valid @RequestBody request: CreateTicketRequest) =
        service.createTicket(CreateTicketCommand(request.vehicleNumber, request.vehicleType))

    @PostMapping("/tickets/{ticketId}/quote")
    fun quote(@PathVariable ticketId: UUID) = service.quote(ticketId)

    @PostMapping("/tickets/{ticketId}/checkout")
    fun checkout(@PathVariable ticketId: UUID) = service.checkout(ticketId)

    @GetMapping("/tickets/active")
    fun activeTickets() = service.activeTickets()

    @GetMapping("/spots/available")
    fun availableSpots() = service.availableSpots()
}
