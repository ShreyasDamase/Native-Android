package com.learning.parking.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class ParkingFeeCalculatorTest {
    private val calculator = ParkingFeeCalculator()

    @Test
    fun `car first hour costs 3000 cents`() {
        assertThat(calculator.calculate(VehicleType.CAR, Duration.ofMinutes(20))).isEqualTo(3_000)
    }

    @Test
    fun `car extra hour adds half base rate`() {
        assertThat(calculator.calculate(VehicleType.CAR, Duration.ofMinutes(90))).isEqualTo(4_500)
    }
}
