package com.learning.parking.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.learning.parking.domain.ParkingSpot
import com.learning.parking.domain.SpotType
import com.learning.parking.infrastructure.ParkingSpotRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class ParkingControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val spots: ParkingSpotRepository
) {
    @BeforeEach
    fun seedSpot() {
        spots.deleteAll()
        spots.save(ParkingSpot(code = "T-C-001", type = SpotType.COMPACT))
    }

    @Test
    fun `create ticket assigns a spot`() {
        mockMvc.post("/api/v1/parking/tickets") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTicketRequest("MH12AB1234", com.learning.parking.domain.VehicleType.CAR))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.spotCode") { exists() }
            jsonPath("$.status") { value("ACTIVE") }
        }
    }
}
