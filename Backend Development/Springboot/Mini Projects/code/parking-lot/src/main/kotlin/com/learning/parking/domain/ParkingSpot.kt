package com.learning.parking.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID

@Entity
@Table(name = "parking_spots")
class ParkingSpot(
    @Id
    val id: UUID = UUID.randomUUID(),
    val code: String,
    @Enumerated(EnumType.STRING)
    val type: SpotType,
    @Enumerated(EnumType.STRING)
    var status: SpotStatus = SpotStatus.AVAILABLE,
    @Version
    var version: Long = 0
) {
    fun occupy() {
        check(status == SpotStatus.AVAILABLE) { "Spot $code is not available" }
        status = SpotStatus.OCCUPIED
    }

    fun release() {
        status = SpotStatus.AVAILABLE
    }
}
