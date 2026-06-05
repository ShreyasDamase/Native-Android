package com.learning.booking.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

enum class SeatStatus { AVAILABLE, HELD, BOOKED }
enum class HoldStatus { ACTIVE, EXPIRED, CONFIRMED, CANCELLED }
enum class BookingStatus { CONFIRMED, CANCELLED }

@Entity
@Table(name = "events")
class Event(
    @Id val id: UUID = UUID.randomUUID(),
    val title: String,
    val startsAt: Instant
)

@Entity
@Table(name = "seats")
class Seat(
    @Id val id: UUID = UUID.randomUUID(),
    val eventId: UUID,
    val seatCode: String,
    val category: String,
    val priceCents: Long,
    @Enumerated(EnumType.STRING) var status: SeatStatus = SeatStatus.AVAILABLE,
    @Version var version: Long = 0
) {
    fun hold() {
        check(status == SeatStatus.AVAILABLE) { "Seat $seatCode is not available" }
        status = SeatStatus.HELD
    }

    fun book() {
        check(status == SeatStatus.HELD) { "Seat $seatCode must be held before booking" }
        status = SeatStatus.BOOKED
    }

    fun release() {
        if (status == SeatStatus.HELD) status = SeatStatus.AVAILABLE
    }
}

@Entity
@Table(name = "seat_holds")
class SeatHold(
    @Id val id: UUID = UUID.randomUUID(),
    val eventId: UUID,
    @Enumerated(EnumType.STRING) var status: HoldStatus = HoldStatus.ACTIVE,
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
    @OneToMany(mappedBy = "hold", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<SeatHoldItem> = mutableListOf()
) {
    fun isActive(now: Instant) = status == HoldStatus.ACTIVE && expiresAt.isAfter(now)
    fun confirm() {
        status = HoldStatus.CONFIRMED
    }
}

@Entity
@Table(name = "seat_hold_items")
class SeatHoldItem(
    @Id val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "hold_id") val hold: SeatHold,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seat_id") val seat: Seat
)

@Entity
@Table(name = "bookings")
class Booking(
    @Id val id: UUID = UUID.randomUUID(),
    val holdId: UUID,
    val idempotencyKey: String,
    @Enumerated(EnumType.STRING) val status: BookingStatus = BookingStatus.CONFIRMED,
    val totalCents: Long,
    val createdAt: Instant = Instant.now()
)
