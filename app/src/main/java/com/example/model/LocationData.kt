package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class LocationData(
    val latitude: Double = 35.434008,
    val longitude: Double = -118.730612,
    val altitudeMeters: Double = 605.0,
    val accuracyMeters: Float = 4.2f,
    val speedKmh: Float = 0f,
    val bearingDegrees: Float = 220f,
    val compassDegrees: Float = 220f,
    val compassDirection: String = "SW",
    val title: String = "California, United States",
    val addressLine: String = "Breckenridge Road, Kern County, California, United States",
    val plusCode: String = "84CQ+47",
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Int = 25,
    val weatherCondition: String = "Sunny",
    val humidityPercent: Int = 42,
    val pressureHpa: Int = 1014,
    val isLiveFix: Boolean = false
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
        return toDms(latitude, isLatitude = true)
    }

    fun getLongitudeDms(): String {
        return toDms(longitude, isLatitude = false)
    }

    fun getCoordinatesFormatted(format: CoordinateFormat): String {
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
