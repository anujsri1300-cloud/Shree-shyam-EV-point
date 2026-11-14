package com.example.util

import android.location.Location
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormattingUtils {
    fun formatInr(amount: Double): String {
        return String.format(Locale("en", "IN"), "₹%.2f", amount)
    }

    fun formatUnits(units: Double): String {
        return String.format(Locale.US, "%.2f kWh", units)
    }

    fun formatTimestamp(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }

    fun distanceKm(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(fromLat, fromLng, toLat, toLng, results)
        return results[0] / 1000.0
    }
}
