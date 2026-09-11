package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AltitudeUnit
import com.example.model.CoordinateFormat
import com.example.model.MapStyle
import com.example.model.StampBgStyle
import com.example.model.StampConfig
import com.example.model.StampPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gps_camera_prefs", Context.MODE_PRIVATE)

    private val _stampConfig = MutableStateFlow(loadConfig())
    val stampConfig: StateFlow<StampConfig> = _stampConfig.asStateFlow()

    private fun loadConfig(): StampConfig {
        return StampConfig(
            showMiniMap = prefs.getBoolean("show_mini_map", true),
            mapStyle = MapStyle.valueOf(prefs.getString("map_style", MapStyle.NORMAL_STREET.name) ?: MapStyle.NORMAL_STREET.name),
            showCoordinates = prefs.getBoolean("show_coordinates", true),
            coordinateFormat = CoordinateFormat.valueOf(prefs.getString("coord_format", CoordinateFormat.DMS.name) ?: CoordinateFormat.DMS.name),
            showAltitude = prefs.getBoolean("show_altitude", true),
            altitudeUnit = AltitudeUnit.valueOf(prefs.getString("alt_unit", AltitudeUnit.METERS.name) ?: AltitudeUnit.METERS.name),
            showAddress = prefs.getBoolean("show_address", true),
            showDateTime = prefs.getBoolean("show_datetime", true),
            showCompass = prefs.getBoolean("show_compass", true),
            showNote = prefs.getBoolean("show_note", true),
            customNote = prefs.getString("custom_note", "GPS Live Field Capture") ?: "GPS Live Field Capture",
            showWeather = prefs.getBoolean("show_weather", true),
            showBadge = prefs.getBoolean("show_badge", true),
            badgeText = prefs.getString("badge_text", "GPS Verified") ?: "GPS Verified",
            stampPosition = StampPosition.valueOf(prefs.getString("stamp_pos", StampPosition.BOTTOM.name) ?: StampPosition.BOTTOM.name),
            bgStyle = StampBgStyle.valueOf(prefs.getString("bg_style", StampBgStyle.TRANSLUCENT_DARK.name) ?: StampBgStyle.TRANSLUCENT_DARK.name),
            customFolderName = prefs.getString("custom_folder_name", "GPSMapCamera") ?: "GPSMapCamera",
            saveToGallery = prefs.getBoolean("save_to_gallery", true),
            useManualLocation = prefs.getBoolean("use_manual_loc", false),
            manualLatitude = prefs.getFloat("manual_lat", 35.434008f).toDouble(),
            manualLongitude = prefs.getFloat("manual_lng", -118.730612f).toDouble(),
            manualAltitude = prefs.getFloat("manual_alt", 605.0f).toDouble(),
            manualTitle = prefs.getString("manual_title", "California, United States") ?: "California, United States",
            manualAddress = prefs.getString("manual_address", "Breckenridge Road, Kern County, California, United States") ?: "Breckenridge Road, Kern County, California, United States"
        )
    }

    fun updateConfig(config: StampConfig) {
        prefs.edit().apply {
            putBoolean("show_mini_map", config.showMiniMap)
            putString("map_style", config.mapStyle.name)
            putBoolean("show_coordinates", config.showCoordinates)
            putString("coord_format", config.coordinateFormat.name)
            putBoolean("show_altitude", config.showAltitude)
            putString("alt_unit", config.altitudeUnit.name)
            putBoolean("show_address", config.showAddress)
            putBoolean("show_datetime", config.showDateTime)
            putBoolean("show_compass", config.showCompass)
            putBoolean("show_note", config.showNote)
            putString("custom_note", config.customNote)
            putBoolean("show_weather", config.showWeather)
            putBoolean("show_badge", config.showBadge)
            putString("badge_text", config.badgeText)
            putString("stamp_pos", config.stampPosition.name)
            putString("bg_style", config.bgStyle.name)
            putString("custom_folder_name", config.customFolderName)
            putBoolean("save_to_gallery", config.saveToGallery)
            putBoolean("use_manual_loc", config.useManualLocation)
            putFloat("manual_lat", config.manualLatitude.toFloat())
            putFloat("manual_lng", config.manualLongitude.toFloat())
            putFloat("manual_alt", config.manualAltitude.toFloat())
            putString("manual_title", config.manualTitle)
            putString("manual_address", config.manualAddress)
            apply()
        }
        _stampConfig.value = config
    }
}
