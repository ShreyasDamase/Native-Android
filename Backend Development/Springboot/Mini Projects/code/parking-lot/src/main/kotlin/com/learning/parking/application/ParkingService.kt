package com.learning.parking.application

import com.learning.parking.common.ConflictException
import com.learning.parking.common.NotFoundException
import com.learning.parking.domain.ParkingFeeCalculator
import com.learning.parking.domain.ParkingTicket
import com.learning.parking.domain.SpotStatus
import com.learning.parking.domain.SpotType
import com.learning.parking.domain.TicketStatus
import com.learning.parking.domain.VehicleType
import com.learning.parking.infrastructure.ParkingSpotRepository
import com.learning.parking.infrastructure.ParkingTicketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class CreateTicketCommand(val vehicleNumber: String, val vehicleType: VehicleType)
data class TicketView(
    val ticketId: UUID,
    val vehicleNumber: String,
    val vehicleType: VehicleType,
    val spotCode: String,
    val status: TicketStatus,
    val entryTime: Instant,
    val exitTime: Instant?,
    val feeCents: Long?
)

@Service
class ParkingService(
    private val spots: ParkingSpotRepository,
    private val tickets: ParkingTicketRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    private val calculator = ParkingFeeCalculator()

    @Transactional
    fun createTicket(command: CreateTicketCommand): TicketView {
        val activeStatuses = listOf(TicketStatus.ACTIVE, TicketStatus.PAYMENT_PENDING)
        if (tickets.existsByVehicleNumberAndStatusIn(command.vehicleNumber, activeStatuses)) {
            throw ConflictException("Vehicle already has an active parking ticket")
        }

        val spot = spots.findFirstByTypeInAndStatusOrderByCode(compatibleSpots(command.vehicleType), SpotStatus.AVAILABLE)
            .orElseThrow { ConflictException("No compatible parking spot is available") }

        spot.occupy()
        val ticket = ParkingTicket(
            vehicleNumber = command.vehicleNumber.uppercase(),
            vehicleType = command.vehicleType,
            spot = spot,
            entryTime = Instant.now(clock)
        )
        return tickets.save(ticket).toView()
    }

    @Transactional(readOnly = true)
    fun quote(ticketId: UUID): Map<String, Any> {
        val ticket = tickets.findById(ticketId).orElseThrow { NotFoundException("Ticket not found") }
        val fee = ticket.quote(Instant.now(clock), calculator)
        return mapOf("ticketId" to ticket.id, "status" to ticket.status, "feeCents" to fee)
    }

    @Transactional
    fun checkout(ticketId: UUID): TicketView {
        val ticket = tickets.findById(ticketId).orElseThrow { NotFoundException("Ticket not found") }
        ticket.checkout(Instant.now(clock), calculator)
        return ticket.toView()
    }

    @Transactional(readOnly = true)
    fun activeTickets(): List<TicketView> = tickets.findByStatus(TicketStatus.ACTIVE).map { it.toView() }

    @Transactional(readOnly = true)
    fun availableSpots() = spots.findByStatus(SpotStatus.AVAILABLE).map { mapOf("id" to it.id, "code" to it.code, "type" to it.type) }

    private fun compatibleSpots(vehicleType: VehicleType): List<SpotType> = when (vehicleType) {
        VehicleType.TWO_WHEELER -> listOf(SpotType.BIKE)
        VehicleType.CAR -> listOf(SpotType.COMPACT, SpotType.LARGE)
        VehicleType.SUV -> listOf(SpotType.LARGE)
        VehicleType.EV -> listOf(SpotType.EV_CHARGING, SpotType.COMPACT, SpotType.LARGE)
    }
}

fun ParkingTicket.toView() = TicketView(
    ticketId = id,
    vehicleNumber = vehicleNumber,
    vehicleType = vehicleType,
    spotCode = spot.code,
    status = status,
    entryTime = entryTime,
    exitTime = exitTime,
    feeCents = feeCents
)
