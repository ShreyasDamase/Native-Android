package com.learning.parking.infrastructure

import com.learning.parking.domain.ParkingSpot
import com.learning.parking.domain.ParkingTicket
import com.learning.parking.domain.SpotStatus
import com.learning.parking.domain.SpotType
import com.learning.parking.domain.TicketStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.Optional
import java.util.UUID

interface ParkingSpotRepository : JpaRepository<ParkingSpot, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findFirstByTypeInAndStatusOrderByCode(types: Collection<SpotType>, status: SpotStatus): Optional<ParkingSpot>

    fun findByStatus(status: SpotStatus): List<ParkingSpot>
}

interface ParkingTicketRepository : JpaRepository<ParkingTicket, UUID> {
    fun existsByVehicleNumberAndStatusIn(vehicleNumber: String, statuses: Collection<TicketStatus>): Boolean
    fun findByStatus(status: TicketStatus): List<ParkingTicket>
}
