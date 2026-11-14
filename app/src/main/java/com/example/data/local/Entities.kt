package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "station_config")
data class StationConfigEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "Shree Shyam Charging Station",
    val ratePerUnitInr: Double = 14.0,
    val upiId: String = "shreeshyam.ev@upi",
    val ownerPhone: String = "+91 98290 00000",
    val ownerPin: String = "1234",
    val maxUnitsPerSession: Double = 50.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "station_locations")
data class StationLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val totalBays: Int,
    val availableBays: Int,
    val chargerType: String,
    val isPrimary: Boolean = false,
    val isActive: Boolean = true
)

@Entity(tableName = "recharge_transactions")
data class RechargeTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val receiptCode: String,
    val stationId: Int,
    val stationName: String,
    val vehicleNumber: String,
    val customerName: String,
    val ratePerUnitSnapshot: Double,
    val unitsPurchased: Double,
    val amountInr: Double,
    val paymentMethod: String,
    val cashTenderedInr: Double? = null,
    val changeReturnedInr: Double? = null,
    val upiReference: String? = null,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)
