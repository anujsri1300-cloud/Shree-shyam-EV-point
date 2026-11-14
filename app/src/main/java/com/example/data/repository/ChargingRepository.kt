package com.example.data.repository

import android.net.Uri
import com.example.data.local.RechargeTransactionEntity
import com.example.data.local.StationConfigDao
import com.example.data.local.StationConfigEntity
import com.example.data.local.StationLocationDao
import com.example.data.local.StationLocationEntity
import com.example.data.local.TransactionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import kotlin.random.Random

class ChargingRepository(
    private val configDao: StationConfigDao,
    private val locationDao: StationLocationDao,
    private val transactionDao: TransactionDao
) {
    val stationConfig: Flow<StationConfigEntity> = configDao.observeConfig()
        .map { it ?: StationConfigEntity() }

    val stationLocations: Flow<List<StationLocationEntity>> = locationDao.observeAllLocations()

    val transactions: Flow<List<RechargeTransactionEntity>> = transactionDao.observeAllTransactions()

    suspend fun ensureSeeded() {
        if (configDao.getConfigOnce() == null) {
            configDao.upsertConfig(StationConfigEntity())
        }
        if (locationDao.getAllOnce().isEmpty()) {
            locationDao.insertLocation(
                StationLocationEntity(
                    name = "Station 1 — Khatu Shyamji Main Hub",
                    address = "Ringas–Khatu Road, Near Toran Dwar, Sikar, Rajasthan 332602",
                    latitude = 27.3673,
                    longitude = 75.4027,
                    totalBays = 6,
                    availableBays = 4,
                    chargerType = "DC Fast 30kW + AC Type-2",
                    isPrimary = true,
                    isActive = true
                )
            )
            locationDao.insertLocation(
                StationLocationEntity(
                    name = "Station 2 — Jaipur Highway Plaza",
                    address = "NH-52 Sikar Road Bypass, Chomu, Jaipur, Rajasthan 303702",
                    latitude = 27.1636,
                    longitude = 75.7222,
                    totalBays = 4,
                    availableBays = 3,
                    chargerType = "DC Fast 60kW + Bharat AC001",
                    isPrimary = false,
                    isActive = true
                )
            )
        }
    }

    suspend fun updateOwnerRate(ratePerUnitInr: Double) {
        val current = configDao.getConfigOnce() ?: StationConfigEntity()
        val sanitized = ratePerUnitInr.coerceIn(1.0, 999.0)
        configDao.upsertConfig(
            current.copy(
                ratePerUnitInr = roundTwoDecimals(sanitized),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateOwnerSettings(
        businessName: String,
        ratePerUnitInr: Double,
        upiId: String,
        ownerPhone: String,
        ownerPin: String,
        maxUnitsPerSession: Double
    ) {
        val current = configDao.getConfigOnce() ?: StationConfigEntity()
        configDao.upsertConfig(
            current.copy(
                businessName = businessName.trim().ifBlank { "Shree Shyam Charging Station" },
                ratePerUnitInr = roundTwoDecimals(ratePerUnitInr.coerceIn(1.0, 999.0)),
                upiId = upiId.trim().ifBlank { current.upiId },
                ownerPhone = ownerPhone.trim().ifBlank { current.ownerPhone },
                ownerPin = ownerPin.trim().takeIf { it.length >= 4 } ?: current.ownerPin,
                maxUnitsPerSession = maxUnitsPerSession.coerceIn(1.0, 500.0),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveLocation(location: StationLocationEntity) {
        if (location.id == 0) {
            locationDao.insertLocation(location)
        } else {
            locationDao.updateLocation(location)
        }
    }

    suspend fun deleteLocation(id: Int) {
        locationDao.deleteLocationById(id)
    }

    suspend fun recordRecharge(
        station: StationLocationEntity,
        vehicleNumber: String,
        customerName: String,
        ratePerUnit: Double,
        units: Double,
        amountInr: Double,
        paymentMethod: String,
        cashTenderedInr: Double? = null,
        upiReference: String? = null,
        initialStatus: String = "CHARGING"
    ): RechargeTransactionEntity {
        val code = "SSCS-${Random.nextInt(1000, 9999)}"
        val roundedAmount = roundTwoDecimals(amountInr)
        val change = if (paymentMethod == "CASH" && cashTenderedInr != null) {
            roundTwoDecimals((cashTenderedInr - roundedAmount).coerceAtLeast(0.0))
        } else null

        val entity = RechargeTransactionEntity(
            receiptCode = code,
            stationId = station.id,
            stationName = station.name,
            vehicleNumber = vehicleNumber.trim().uppercase().ifBlank { "WALK-IN EV" },
            customerName = customerName.trim().ifBlank { "Guest Driver" },
            ratePerUnitSnapshot = roundTwoDecimals(ratePerUnit),
            unitsPurchased = roundTwoDecimals(units),
            amountInr = roundedAmount,
            paymentMethod = paymentMethod,
            cashTenderedInr = cashTenderedInr?.let { roundTwoDecimals(it) },
            changeReturnedInr = change,
            upiReference = upiReference?.trim()?.takeIf { it.isNotEmpty() },
            status = initialStatus,
            createdAt = System.currentTimeMillis()
        )
        val insertedId = transactionDao.insertTransaction(entity)
        return entity.copy(id = insertedId.toInt())
    }

    suspend fun markTransactionStatus(id: Int, status: String) {
        transactionDao.updateStatus(id, status)
    }

    suspend fun deleteTransaction(id: Int) {
        transactionDao.deleteById(id)
    }

    companion object {
        fun calculateUnitsForAmount(amountInr: Double, ratePerUnit: Double): Double {
            if (ratePerUnit <= 0.0 || amountInr <= 0.0) return 0.0
            return roundTwoDecimals(amountInr / ratePerUnit)
        }

        fun calculateAmountForUnits(units: Double, ratePerUnit: Double): Double {
            if (ratePerUnit <= 0.0 || units <= 0.0) return 0.0
            return roundTwoDecimals(units * ratePerUnit)
        }

        fun roundTwoDecimals(value: Double): Double {
            return (value * 100.0).roundToInt() / 100.0
        }

        fun buildUpiDeepLink(
            upiId: String,
            payeeName: String,
            amountInr: Double,
            note: String
        ): String {
            val formattedAmount = String.format(java.util.Locale.US, "%.2f", amountInr)
            return Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", payeeName)
                .appendQueryParameter("am", formattedAmount)
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("tn", note)
                .build()
                .toString()
        }
    }
}
