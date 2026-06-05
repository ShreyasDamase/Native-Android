package com.learning.parking.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.ceil

@Entity
@Table(name = "parking_tickets")
class ParkingTicket(
    @Id
    val id: UUID = UUID.randomUUID(),
    val vehicleNumber: String,
    @Enumerated(EnumType.STRING)
    val vehicleType: VehicleType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id")
    val spot: ParkingSpot,
    @Enumerated(EnumType.STRING)
    var status: TicketStatus = TicketStatus.ACTIVE,
    val entryTime: Instant = Instant.now(),
    var exitTime: Instant? = null,
    var feeCents: Long? = null,
    @Version
    var version: Long = 0
) {
    fun quote(now: Instant, calculator: ParkingFeeCalculator): Long {
        return calculator.calculate(vehicleType, Duration.between(entryTime, now))
    }

    fun checkout(now: Instant, calculator: ParkingFeeCalculator): Long {
        if (status == TicketStatus.PAID) return feeCents ?: 0
        val fee = quote(now, calculator)
        status = TicketStatus.PAID
        exitTime = now
        feeCents = fee
        spot.release()
        return fee
    }
}

class ParkingFeeCalculator {
    fun calculate(vehicleType: VehicleType, parkedFor: Duration): Long {
        val hours = ceil(parkedFor.toMinutes().coerceAtLeast(1) / 60.0).toLong().coerceAtLeast(1)
        val firstHour = when (vehicleType) {
            VehicleType.TWO_WHEELER -> 1_000
            VehicleType.CAR -> 3_000
            VehicleType.SUV -> 4_000
            VehicleType.EV -> 3_500
        }
        val extraHour = firstHour / 2
        return firstHour + ((hours - 1) * extraHour)
    }
}
