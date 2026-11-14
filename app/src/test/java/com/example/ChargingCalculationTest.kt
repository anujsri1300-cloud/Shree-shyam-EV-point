package com.example

import com.example.data.repository.ChargingRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ChargingCalculationTest {

    @Test
    fun calculateUnitsForAmount_respectsOwnerPerUnitRate() {
        val units = ChargingRepository.calculateUnitsForAmount(
            amountInr = 280.0,
            ratePerUnit = 14.0
        )
        assertEquals(20.0, units, 0.001)
    }

    @Test
    fun calculateAmountForUnits_respectsOwnerPerUnitRate() {
        val amount = ChargingRepository.calculateAmountForUnits(
            units = 7.5,
            ratePerUnit = 16.0
        )
        assertEquals(120.0, amount, 0.001)
    }

    @Test
    fun calculateUnitsForAmount_handlesZeroRateSafely() {
        assertEquals(0.0, ChargingRepository.calculateUnitsForAmount(100.0, 0.0), 0.001)
        assertEquals(0.0, ChargingRepository.calculateAmountForUnits(10.0, 0.0), 0.001)
    }
}
