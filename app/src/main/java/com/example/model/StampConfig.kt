package com.example.model

enum class CoordinateFormat(val label: String) {
    DMS("DMS (35° 26' 2.4\" N)"),
    DECIMAL("Decimal (35.4340° N)")
}

enum class AltitudeUnit(val label: String) {
    METERS("Meters (m)"),
    FEET("Feet (ft)")
}

enum class MapStyle(val label: String) {
    NORMAL_STREET("Street / Roads"),
    SATELLITE("Satellite"),
    TERRAIN("Terrain / Topo")
}

enum class StampPosition(val label: String) {
    BOTTOM("Bottom of Photo"),
    TOP("Top of Photo")
}

enum class StampBgStyle(val label: String) {
    TRANSLUCENT_DARK("Dark Glass (75%)"),
    SOLID_BLACK("Solid Midnight"),
    FROSTED_LIGHT("Frosted Light"),
    MINIMAL_BORDER("Minimal Frame")
}

data class StampConfig(
    // Toggles for metadata
    val showMiniMap: Boolean = true,
    val mapStyle: MapStyle = MapStyle.NORMAL_STREET,
    val showCoordinates: Boolean = true,
    val coordinateFormat: CoordinateFormat = CoordinateFormat.DMS,
    val showAltitude: Boolean = true,
    val altitudeUnit: AltitudeUnit = AltitudeUnit.METERS,
    val showAddress: Boolean = true,
    val showDateTime: Boolean = true,
    val showCompass: Boolean = true,
    val showNote: Boolean = true,
    val customNote: String = "GPS Live Field Capture",
    val showWeather: Boolean = true,
    val showBadge: Boolean = true,
    val badgeText: String = "GPS Verified",

    // Visual Customization
    val stampPosition: StampPosition = StampPosition.BOTTOM,
    val bgStyle: StampBgStyle = StampBgStyle.TRANSLUCENT_DARK,

    // Storage and Gallery Customization
    val customFolderName: String = "GPSMapCamera",
    val customFolderTreeUri: String? = null,
    val customFolderDisplayName: String? = null,
    val saveToGallery: Boolean = true,

    // Manual / Mock Location Test Mode (Great for emulators and indoors)
    val useManualLocation: Boolean = false,
    val manualLatitude: Double = 35.434008,
    val manualLongitude: Double = -118.730612,
    val manualAltitude: Double = 605.0,
    val manualTitle: String = "California, United States",
    val manualAddress: String = "Breckenridge Road, Kern County, California, United States"
)
