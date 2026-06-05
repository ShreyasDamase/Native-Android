package com.learning.booking.application

import com.learning.booking.common.ConflictException
import com.learning.booking.common.NotFoundException
import com.learning.booking.domain.Booking
import com.learning.booking.domain.HoldStatus
import com.learning.booking.domain.SeatHold
import com.learning.booking.domain.SeatHoldItem
import com.learning.booking.domain.SeatStatus
import com.learning.booking.infrastructure.BookingRepository
import com.learning.booking.infrastructure.EventRepository
import com.learning.booking.infrastructure.SeatHoldRepository
import com.learning.booking.infrastructure.SeatRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class SeatView(val seatId: UUID, val seatCode: String, val category: String, val priceCents: Long, val status: SeatStatus)
data class HoldView(val holdId: UUID, val eventId: UUID, val seatIds: List<UUID>, val expiresAt: Instant, val totalCents: Long)
data class BookingView(val bookingId: UUID, val holdId: UUID, val status: String, val totalCents: Long)

@Service
class BookingService(
    private val events: EventRepository,
    private val seats: SeatRepository,
    private val holds: SeatHoldRepository,
    private val bookings: BookingRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional(readOnly = true)
    fun listEvents() = events.findAll().map { mapOf("eventId" to it.id, "title" to it.title, "startsAt" to it.startsAt) }

    @Transactional(readOnly = true)
    fun seatsFor(eventId: UUID): List<SeatView> = seats.findByEventIdOrderBySeatCode(eventId)
        .map { SeatView(it.id, it.seatCode, it.category, it.priceCents, it.status) }

    @Transactional
    fun createHold(eventId: UUID, seatIds: List<UUID>): HoldView {
        if (!events.existsById(eventId)) throw NotFoundException("Event not found")
        val selectedSeats = seats.findByIdIn(seatIds).sortedBy { it.seatCode }
        if (selectedSeats.size != seatIds.toSet().size) throw NotFoundException("One or more seats were not found")
        if (selectedSeats.any { it.eventId != eventId }) throw ConflictException("All seats must belong to the same event")
        if (selectedSeats.any { it.status != SeatStatus.AVAILABLE }) throw ConflictException("One or more seats are not available")

        selectedSeats.forEach { it.hold() }
        val now = Instant.now(clock)
        val hold = SeatHold(eventId = eventId, expiresAt = now.plus(Duration.ofMinutes(5)), createdAt = now)
        selectedSeats.forEach { hold.items.add(SeatHoldItem(hold = hold, seat = it)) }
        holds.save(hold)
        return HoldView(hold.id, eventId, selectedSeats.map { it.id }, hold.expiresAt, selectedSeats.sumOf { it.priceCents })
    }

    @Transactional
    fun confirmBooking(holdId: UUID, idempotencyKey: String): BookingView {
        val existing = bookings.findByIdempotencyKey(idempotencyKey)
        if (existing.isPresent) return existing.get().toView()

        val hold = holds.findById(holdId).orElseThrow { NotFoundException("Hold not found") }
        if (!hold.isActive(Instant.now(clock))) throw ConflictException("Hold is expired or not active")

        val seatsInHold = hold.items.map { it.seat }
        seatsInHold.forEach { it.book() }
        hold.confirm()

        val booking = bookings.save(
            Booking(holdId = hold.id, idempotencyKey = idempotencyKey, totalCents = seatsInHold.sumOf { it.priceCents })
        )
        return booking.toView()
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun releaseExpiredHolds() {
        val expired = holds.findByStatusAndExpiresAtBefore(HoldStatus.ACTIVE, Instant.now(clock))
        expired.forEach { hold ->
            hold.status = HoldStatus.EXPIRED
            hold.items.forEach { it.seat.release() }
        }
    }
}

fun Booking.toView() = BookingView(id, holdId, status.name, totalCents)
