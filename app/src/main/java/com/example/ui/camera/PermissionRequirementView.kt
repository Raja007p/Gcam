package com.example.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.permission.PermissionStatus
import com.example.ui.theme.Amber400
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Red500
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun PermissionRequirementView(
    status: PermissionStatus,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestAll: () -> Unit,
    onTurnOnLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRefreshCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("permission_requirement_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Hero Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0x2200E5FF))
                    .border(2.dp, Cyan400.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (!status.isLocationServiceEnabled && status.arePermissionsGranted) {
                        Icons.Default.LocationOff
                    } else if (status.hasAnyPermanentDenial) {
                        Icons.Default.Settings
                    } else {
                        Icons.Default.PhotoCamera
                    },
                    contentDescription = null,
                    tint = if (!status.isLocationServiceEnabled && status.arePermissionsGranted) Amber400 else Cyan400,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (!status.isLocationServiceEnabled && status.arePermissionsGranted) {
                    "Location Service is Off"
                } else if (!status.arePermissionsGranted) {
                    "Permissions Required"
                } else {
                    "Setup Required"
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "GPS Map Camera requires Camera access to take photos and Location services to embed authentic GPS coordinates, altitude, and maps.",
                color = Slate400,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Camera Access Status Card
            PermissionItemCard(
                icon = Icons.Default.PhotoCamera,
                title = "Camera Permission",
                isReady = status.isCameraGranted,
                isPermanentlyDenied = status.isCameraPermanentlyDenied,
                readyMessage = "Camera access is enabled and ready to capture photos.",
                deniedMessage = "Camera permission is required to preview and capture photos.",
                permanentMessage = "Camera access is permanently denied. Please enable it manually in App Settings.",
                buttonText = if (status.isCameraPermanentlyDenied) "Open App Settings" else "Grant Camera Permission",
                buttonTag = if (status.isCameraPermanentlyDenied) "camera_open_settings_button" else "enable_camera_button",
                onAction = {
                    if (status.isCameraPermanentlyDenied) onOpenAppSettings() else onRequestCamera()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Location Permission Status Card
            PermissionItemCard(
                icon = Icons.Default.LocationOn,
                title = "Location Permission",
                isReady = status.isLocationGranted,
                isPermanentlyDenied = status.isLocationPermanentlyDenied,
                readyMessage = "Location permission is granted for GPS coordinates.",
                deniedMessage = "Location permission is required to acquire GPS coordinates, altitude, and maps.",
                permanentMessage = "Location access is permanently denied. Please enable it manually in App Settings.",
                buttonText = if (status.isLocationPermanentlyDenied) "Open App Settings" else "Grant Location Permission",
                buttonTag = if (status.isLocationPermanentlyDenied) "location_open_settings_button" else "enable_location_button",
                onAction = {
                    if (status.isLocationPermanentlyDenied) onOpenAppSettings() else onRequestLocation()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Device Location Service (GPS Power State) Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("location_service_status_card"),
                shape = RoundedCornerShape(14.dp),
                color = if (!status.isLocationServiceEnabled) Color(0x33B91C1C) else Slate900,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (!status.isLocationServiceEnabled) Amber400 else Emerald500.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (status.isLocationServiceEnabled) Color(0x2210B981) else Color(0x33F59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (status.isLocationServiceEnabled) Icons.Default.GpsFixed else Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = if (status.isLocationServiceEnabled) Emerald500 else Amber400,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Device Location (GPS)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (status.isLocationServiceEnabled) {
                                    "Location service is turned on."
                                } else {
                                    "Location is turned off. Please turn on Location to use this feature."
                                },
                                color = if (status.isLocationServiceEnabled) Slate400 else Amber400,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (status.isLocationServiceEnabled) Color(0x2210B981) else Color(0x33F59E0B),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = if (status.isLocationServiceEnabled) "ON" else "OFF",
                                color = if (status.isLocationServiceEnabled) Emerald500 else Amber400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!status.isLocationServiceEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onTurnOnLocation,
                            colors = ButtonDefaults.buttonColors(containerColor = Amber400),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("turn_on_location_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Slate950,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Turn On Location",
                                color = Slate950,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Collective Action Button
            if (!status.arePermissionsGranted && !status.hasAnyPermanentDenial) {
                Button(
                    onClick = onRequestAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_all_permissions_button")
                ) {
                    Text(
                        text = "Grant All Permissions",
                        color = Slate950,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (status.hasAnyPermanentDenial) {
                Button(
                    onClick = onOpenAppSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("primary_open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Slate950,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open App Settings",
                        color = Slate950,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (!status.isLocationServiceEnabled) {
                Button(
                    onClick = onTurnOnLocation,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("primary_turn_on_location_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = Slate950,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Turn On Location",
                        color = Slate950,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Secondary Refresh Button
            OutlinedButton(
                onClick = onRefreshCheck,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("refresh_permissions_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Check Permissions Again",
                    color = Cyan400,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    isReady: Boolean,
    isPermanentlyDenied: Boolean,
    readyMessage: String,
    deniedMessage: String,
    permanentMessage: String,
    buttonText: String,
    buttonTag: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isReady) Emerald500.copy(alpha = 0.5f) else if (isPermanentlyDenied) Red500.copy(alpha = 0.6f) else Slate700
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isReady) Color(0x2210B981) else if (isPermanentlyDenied) Color(0x33EF4444) else Color(0x2200E5FF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isReady) Emerald500 else if (isPermanentlyDenied) Red500 else Cyan400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isReady) readyMessage else if (isPermanentlyDenied) permanentMessage else deniedMessage,
                        color = if (isReady) Slate400 else if (isPermanentlyDenied) Red500 else Slate400,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isReady) Color(0x2210B981) else if (isPermanentlyDenied) Color(0x33EF4444) else Color(0x33F59E0B),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (isReady) "READY" else if (isPermanentlyDenied) "DENIED" else "REQUIRED",
                        color = if (isReady) Emerald500 else if (isPermanentlyDenied) Red500 else Amber400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!isReady) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPermanentlyDenied) Slate800 else Cyan500
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(buttonTag)
                ) {
                    if (isPermanentlyDenied) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = buttonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Text(text = buttonText, color = Slate950, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
