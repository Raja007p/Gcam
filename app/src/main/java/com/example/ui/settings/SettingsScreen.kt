package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterHdr
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AltitudeUnit
import com.example.model.CoordinateFormat
import com.example.model.MapStyle
import com.example.model.StampBgStyle
import com.example.model.StampConfig
import com.example.model.StampPosition
import com.example.ui.components.StampOverlayView
import com.example.ui.theme.Amber400
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.CameraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CameraViewModel,
    onNavigateBack: () -> Unit
) {
    val stampConfig by viewModel.stampConfig.collectAsState()
    val locationData by viewModel.locationData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Stamp Customization",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900)
            )
        },
        containerColor = Slate950
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LIVE STAMP PREVIEW",
                    color = Cyan400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                StampOverlayView(
                    location = locationData,
                    config = stampConfig,
                    modifier = Modifier.border(1.dp, Slate800, RoundedCornerShape(8.dp))
                )
            }

            // Section 1: Mini Map & Location Pin
            item {
                SettingsSectionCard(title = "Mini Map & Pin Marker", icon = Icons.Default.Map) {
                    SettingToggleRow(
                        title = "Show Mini Map",
                        subtitle = "Display Google-style map thumbnail with red location pin",
                        checked = stampConfig.showMiniMap,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(showMiniMap = it)) },
                        testTag = "toggle_mini_map"
                    )

                    if (stampConfig.showMiniMap) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Map Theme Style", color = Slate400, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MapStyle.values().forEach { style ->
                                FilterChip(
                                    selected = stampConfig.mapStyle == style,
                                    onClick = { viewModel.updateConfig(stampConfig.copy(mapStyle = style)) },
                                    label = { Text(style.label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Cyan500,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Coordinates & Formats
            item {
                SettingsSectionCard(title = "Coordinates & Position", icon = Icons.Default.LocationOn) {
                    SettingToggleRow(
                        title = "Show Latitude & Longitude",
                        subtitle = "Pin down the exact geographic coordinates",
                        checked = stampConfig.showCoordinates,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(showCoordinates = it)) },
                        testTag = "toggle_coordinates"
                    )

                    if (stampConfig.showCoordinates) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Coordinate Display Format", color = Slate400, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoordinateFormat.values().forEach { format ->
                                FilterChip(
                                    selected = stampConfig.coordinateFormat == format,
                                    onClick = { viewModel.updateConfig(stampConfig.copy(coordinateFormat = format)) },
                                    label = { Text(format.label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Cyan500,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Altitude
            item {
                SettingsSectionCard(title = "Altitude & Elevation", icon = Icons.Default.FilterHdr) {
                    SettingToggleRow(
                        title = "Show Altitude",
                        subtitle = "Elevation above sea level",
                        checked = stampConfig.showAltitude,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(showAltitude = it)) },
                        testTag = "toggle_altitude"
                    )

                    if (stampConfig.showAltitude) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Altitude Unit", color = Slate400, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AltitudeUnit.values().forEach { unit ->
                                FilterChip(
                                    selected = stampConfig.altitudeUnit == unit,
                                    onClick = { viewModel.updateConfig(stampConfig.copy(altitudeUnit = unit)) },
                                    label = { Text(unit.label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Cyan500,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Address, Date & Compass
            item {
                SettingsSectionCard(title = "Geographic & Sensor Details", icon = Icons.Default.Explore) {
                    SettingToggleRow(
                        title = "Show Address & Place Title",
                        subtitle = "City, state, country, and street location",
                        checked = stampConfig.showAddress,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(showAddress = it)) },
                        testTag = "toggle_address"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    SettingToggleRow(
                        title = "Show Date & Time",
                        subtitle = "Exact capture timestamp with timezone",
                        checked = stampConfig.showDateTime,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(showDateTime = it)) },
                        testTag = "toggle_datetime"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    SettingToggleRow(
                        title = "Show Live Compass Heading",
                        subtitle = "Sensor-driven degrees and direction (e.g. 220° SW)",
                        checked = stampConfig.showCompass,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(showCompass = it)) },
                        testTag = "toggle_compass"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    SettingToggleRow(
                        title = "Show Weather & Temperature",
                        subtitle = "Live temperature (°C) and conditions",
                        checked = stampConfig.showWeather,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(showWeather = it)) },
                        testTag = "toggle_weather"
                    )
                }
            }

            // Section 5: Custom Notes & Badge
            item {
                SettingsSectionCard(title = "Notes & Category Badge", icon = Icons.Default.Note) {
                    SettingToggleRow(
                        title = "Show Custom Note",
                        subtitle = "Project tag, inspector note, or caption",
                        checked = stampConfig.showNote,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(showNote = it)) },
                        testTag = "toggle_note"
                    )

                    if (stampConfig.showNote) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = stampConfig.customNote,
                            onValueChange = { viewModel.updateConfig(stampConfig.copy(customNote = it)) },
                            label = { Text("Default Note Text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_note_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    SettingToggleRow(
                        title = "Show Category Badge",
                        subtitle = "Header banner above mini-map",
                        checked = stampConfig.showBadge,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(showBadge = it)) },
                        testTag = "toggle_badge"
                    )

                    if (stampConfig.showBadge) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = stampConfig.badgeText,
                            onValueChange = { viewModel.updateConfig(stampConfig.copy(badgeText = it)) },
                            label = { Text("Badge Label (e.g. GPS VERIFIED, POLICE, FIELD SURVEY)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_badge_input"),
                            singleLine = true
                        )
                    }
                }
            }

            // Section 6: Visual Layout & Styling
            item {
                SettingsSectionCard(title = "Stamp Layout & Theme", icon = Icons.Default.Palette) {
                    Text("Stamp Position on Photo", color = Slate400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StampPosition.values().forEach { pos ->
                            FilterChip(
                                selected = stampConfig.stampPosition == pos,
                                onClick = { viewModel.updateConfig(stampConfig.copy(stampPosition = pos)) },
                                label = { Text(pos.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan500,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Stamp Card Background Style", color = Slate400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StampBgStyle.values().forEach { style ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateConfig(stampConfig.copy(bgStyle = style)) },
                                color = if (stampConfig.bgStyle == style) Slate800 else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (stampConfig.bgStyle == style) Cyan400 else Slate800
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = style.label,
                                        color = if (stampConfig.bgStyle == style) Color.White else Slate400,
                                        fontWeight = if (stampConfig.bgStyle == style) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (stampConfig.bgStyle == style) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Cyan400
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 7: Location Override / Test Presets (Ideal for emulators & testing!)
            item {
                SettingsSectionCard(title = "Location Simulation & Presets", icon = Icons.Default.Place) {
                    SettingToggleRow(
                        title = "Custom Location Override",
                        subtitle = "Test any location worldwide (great for emulators or testing)",
                        checked = stampConfig.useManualLocation,
                        onCheckedChange = { viewModel.updateConfig(stampConfig.copy(useManualLocation = it)) },
                        testTag = "toggle_manual_location"
                    )

                    if (stampConfig.useManualLocation) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Worldwide Presets", color = Slate400, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        val presets = listOf(
                            LocationPreset("California", 35.434008, -118.730612, 605.0, "California, United States", "Breckenridge Road, Kern County, California, United States"),
                            LocationPreset("New York", 40.758896, -73.985130, 24.0, "New York, United States", "50th Avenue & Broadway, New York, NY 10019, United States"),
                            LocationPreset("Sydney", -33.868820, 151.209296, 58.0, "New South Wales, Australia", "Bruce Cameron Dr, Moruya, NSW 2537, Australia"),
                            LocationPreset("Tokyo", 35.689487, 139.691706, 44.0, "Tokyo, Japan", "Shinjuku City, Tokyo 160-0022, Japan"),
                            LocationPreset("London", 51.507351, -0.127758, 35.0, "London, United Kingdom", "Westminster, London SW1A 0AA, United Kingdom"),
                            LocationPreset("Mt. Everest", 27.988121, 86.924975, 8848.0, "Solukhumbu, Nepal", "Mount Everest Summit, Khumbu Region, Nepal")
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(presets) { preset ->
                                FilterChip(
                                    selected = stampConfig.manualTitle == preset.title,
                                    onClick = {
                                        viewModel.updateConfig(
                                            stampConfig.copy(
                                                manualLatitude = preset.lat,
                                                manualLongitude = preset.lng,
                                                manualAltitude = preset.alt,
                                                manualTitle = preset.title,
                                                manualAddress = preset.address
                                            )
                                        )
                                    },
                                    label = { Text(preset.name, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Amber400,
                                        selectedLabelColor = Slate950
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = stampConfig.manualTitle,
                            onValueChange = { viewModel.updateConfig(stampConfig.copy(manualTitle = it)) },
                            label = { Text("Area / City Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = stampConfig.manualAddress,
                            onValueChange = { viewModel.updateConfig(stampConfig.copy(manualAddress = it)) },
                            label = { Text("Full Street Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = stampConfig.manualLatitude.toString(),
                                onValueChange = {
                                    it.toDoubleOrNull()?.let { lat ->
                                        viewModel.updateConfig(stampConfig.copy(manualLatitude = lat))
                                    }
                                },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = stampConfig.manualLongitude.toString(),
                                onValueChange = {
                                    it.toDoubleOrNull()?.let { lng ->
                                        viewModel.updateConfig(stampConfig.copy(manualLongitude = lng))
                                    }
                                },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = stampConfig.manualAltitude.toInt().toString(),
                                onValueChange = {
                                    it.toDoubleOrNull()?.let { alt ->
                                        viewModel.updateConfig(stampConfig.copy(manualAltitude = alt))
                                    }
                                },
                                label = { Text("Alt (m)") },
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private data class LocationPreset(
    val name: String,
    val lat: Double,
    val lng: Double,
    val alt: Double,
    val title: String,
    val address: String
)

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Slate900,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Slate400,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Cyan500,
                uncheckedThumbColor = Slate400,
                uncheckedTrackColor = Slate800
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
