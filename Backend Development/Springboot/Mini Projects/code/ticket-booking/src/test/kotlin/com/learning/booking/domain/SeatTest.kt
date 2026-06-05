package com.learning.booking.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class SeatTest {
    @Test
    fun `holding and booking seat changes status`() {
        val seat = Seat(eventId = UUID.randomUUID(), seatCode = "A1", category = "GOLD", priceCents = 45000)
        seat.hold()
        assertThat(seat.status).isEqualTo(SeatStatus.HELD)
        seat.book()
        assertThat(seat.status).isEqualTo(SeatStatus.BOOKED)
    }
}
