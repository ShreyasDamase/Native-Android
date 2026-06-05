package com.learning.booking.infrastructure

import com.learning.booking.domain.Booking
import com.learning.booking.domain.Event
import com.learning.booking.domain.Seat
import com.learning.booking.domain.SeatHold
import com.learning.booking.domain.SeatStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.time.Instant
import java.util.Optional
import java.util.UUID

interface EventRepository : JpaRepository<Event, UUID>

interface SeatRepository : JpaRepository<Seat, UUID> {
    fun findByEventIdOrderBySeatCode(eventId: UUID): List<Seat>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByIdIn(ids: Collection<UUID>): List<Seat>
}

interface SeatHoldRepository : JpaRepository<SeatHold, UUID> {
    fun findByStatusAndExpiresAtBefore(status: com.learning.booking.domain.HoldStatus, now: Instant): List<SeatHold>
}

interface BookingRepository : JpaRepository<Booking, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): Optional<Booking>
}
