package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val speedKmh: Float = 0f,
    val bearingDegrees: Float = 0f,
    val compassDegrees: Float = 0f,
    val compassDirection: String = "N",
    val title: String = "Acquiring GPS...",
    val addressLine: String = "Locating via device GPS satellites...",
    val plusCode: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Int = 25,
    val weatherCondition: String = "Sunny",
    val humidityPercent: Int = 42,
    val pressureHpa: Int = 1014,
    val isLiveFix: Boolean = false,
    val hasRealFix: Boolean = false
) {
    fun getFormattedDateTime(pattern: String = "EEEE, dd MMMM yyyy HH:mm:ss Z"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }

    fun getLatitudeDms(): String {
        if (!hasRealFix && latitude == 0.0) return "Acquiring Lat..."
        return toDms(latitude, isLatitude = true)
    }

    fun getLongitudeDms(): String {
        if (!hasRealFix && longitude == 0.0) return "Acquiring Lng..."
        return toDms(longitude, isLatitude = false)
    }

    fun getCoordinatesFormatted(format: CoordinateFormat): String {
        if (!hasRealFix && latitude == 0.0 && longitude == 0.0) {
            return "Acquiring Live GPS..."
        }
        return when (format) {
            CoordinateFormat.DMS -> "${getLatitudeDms()}  ${getLongitudeDms()}"
            CoordinateFormat.DECIMAL -> {
                val latCard = if (latitude >= 0) "N" else "S"
                val lngCard = if (longitude >= 0) "E" else "W"
                String.format(Locale.US, "%.5f° %s, %.5f° %s", abs(latitude), latCard, abs(longitude), lngCard)
            }
        }
    }

    fun getAltitudeFormatted(unit: AltitudeUnit): String {
        return when (unit) {
            AltitudeUnit.METERS -> "${altitudeMeters.toInt()} m"
            AltitudeUnit.FEET -> "${(altitudeMeters * 3.28084).toInt()} ft"
        }
    }

    companion object {
        fun toDms(decimal: Double, isLatitude: Boolean): String {
            val direction = if (isLatitude) {
                if (decimal >= 0) "N" else "S"
            } else {
                if (decimal >= 0) "E" else "W"
            }
            val absVal = abs(decimal)
            val degrees = absVal.toInt()
            val minutesDecimal = (absVal - degrees) * 60.0
            val minutes = minutesDecimal.toInt()
            val seconds = (minutesDecimal - minutes) * 60.0
            return String.format(Locale.US, "%d° %02d' %04.2f\" %s", degrees, minutes, seconds, direction)
        }

        fun degreesToDirection(degrees: Float): String {
            val normalized = ((degrees % 360) + 360) % 360
            val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
            val index = ((normalized + 11.25f) / 22.5f).toInt() % 16
            return directions[index]
        }
    }
}
