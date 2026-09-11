package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import com.example.model.StampPosition
import com.example.ui.components.StampOverlayView
import com.example.ui.theme.Amber400
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Red500
import com.example.ui.theme.ShutterInner
import com.example.ui.theme.ShutterRing
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.CameraUiEvent
import com.example.viewmodel.CameraViewModel
import java.io.File

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val stampConfig by viewModel.stampConfig.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val lensFacing by viewModel.lensFacing.collectAsState()
    val flashMode by viewModel.flashMode.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val latestPhoto by viewModel.latestPhoto.collectAsState()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    // Quick note edit dialog state
    var showNoteDialog by remember { mutableStateOf(false) }
    var editedNoteText by remember { mutableStateOf(stampConfig.customNote) }

    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fine || coarse || hasLocationPermission
        if (hasLocationPermission) {
            viewModel.onLocationPermissionGranted()
            viewModel.startSensors()
            viewModel.refreshLocation()
        }
    }

    // Storage Access Framework folder picker to select custom folder
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Some OS environments or content providers don't need persistable flag
            }
            val doc = DocumentFile.fromTreeUri(context, uri)
            val displayName = doc?.name ?: uri.lastPathSegment ?: "Custom Folder"
            viewModel.setCustomFolderTree(uri, displayName)
            Toast.makeText(context, "Save location set to: $displayName", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo picker for stamping existing gallery images
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.processPickedImage(it) }
    }

    // Lifecycle sensor control
    DisposableEffect(lifecycleOwner) {
        viewModel.startSensors()
        onDispose {
            viewModel.stopSensors()
        }
    }

    // Automatically request permissions on launch if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Collect UI events (e.g. photo saved)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is CameraUiEvent.PhotoSaved -> {
                    val locationLabel = if (!stampConfig.customFolderDisplayName.isNullOrBlank()) {
                        "Saved to ${stampConfig.customFolderDisplayName}"
                    } else {
                        "Saved to Pictures/${stampConfig.customFolderName}"
                    }
                    Toast.makeText(context, "GPS Photo Stamped! ($locationLabel)", Toast.LENGTH_SHORT).show()
                }
                is CameraUiEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("camera_screen")
    ) {
        // Camera Viewfinder or Permission Prompt
        if (hasCameraPermission) {
            // Re-bind camera whenever lensFacing changes
            key(lensFacing) {
                AndroidView(
                    factory = { ctx ->
                        val view = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        previewView = view

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(view.surfaceProvider)
                                }

                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setFlashMode(flashMode)
                                    .build()
                                imageCapture = capture

                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        view
                    },
                    update = {
                        imageCapture?.flashMode = flashMode
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Simulated Viewfinder & Permission Request Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF020617))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera & Location Access",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To embed real-time GPS coordinates, altitude, compass, and mini-map markers onto your photos, please grant permissions.",
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                        modifier = Modifier.testTag("grant_permissions_button")
                    ) {
                        Text("Grant Camera & GPS Permissions", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Controls Header Overlay (Transparent)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Main Top Bar: Flash on left, Action Icons on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash Mode Toggle
                IconButton(
                    onClick = { viewModel.cycleFlash() },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0x88000000), CircleShape)
                        .testTag("flash_toggle_button")
                ) {
                    val icon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    }
                    Icon(imageVector = icon, contentDescription = "Flash Mode", tint = Color.White)
                }

                // Action controls row (Save Folder, Verify on Map, Switch Camera, Settings)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Choose Save Folder Button
                    IconButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0x88000000), CircleShape)
                            .border(1.dp, if (stampConfig.customFolderTreeUri != null) Cyan400 else Color.Transparent, CircleShape)
                            .testTag("choose_save_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Choose Save Folder",
                            tint = if (stampConfig.customFolderTreeUri != null) Cyan400 else Color.White
                        )
                    }

                    // Verify Location on Google Maps
                    IconButton(
                        onClick = {
                            try {
                                val mapUri = Uri.parse("geo:${locationData.latitude},${locationData.longitude}?q=${locationData.latitude},${locationData.longitude}(GPS+Camera+Location)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${locationData.latitude},${locationData.longitude}"))
                                    context.startActivity(webIntent)
                                }
                            } catch (e: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${locationData.latitude},${locationData.longitude}"))
                                context.startActivity(webIntent)
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0x88000000), CircleShape)
                            .testTag("verify_google_maps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Verify on Google Maps",
                            tint = Cyan400
                        )
                    }

                    // Switch Lens (Front/Back)
                    IconButton(
                        onClick = { viewModel.toggleCamera() },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0x88000000), CircleShape)
                            .testTag("switch_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White
                        )
                    }

                    // Settings Button (Prominent & Always Visible)
                    Surface(
                        onClick = onNavigateToSettings,
                        shape = CircleShape,
                        color = Slate800,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Cyan400),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "App Settings",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Secondary Info Bar: GPS Status Pill and Save Folder Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Location Status Pill
                val isManual = stampConfig.useManualLocation
                val isLive = locationData.isLiveFix && !isManual
                val statusColor = when {
                    isManual -> Amber400
                    isLive -> Emerald500
                    else -> Cyan400
                }
                val statusText = when {
                    isManual -> "MOCK GPS"
                    isLive -> "LIVE GPS"
                    else -> "ACQUIRING..."
                }

                Surface(
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable {
                            if (!hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                viewModel.refreshLocation()
                                Toast.makeText(context, "Acquiring live GPS satellite fix...", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("gps_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLive) Icons.Default.MyLocation else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (locationData.hasRealFix || locationData.latitude != 0.0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${locationData.latitude.toString().take(6)}, ${locationData.longitude.toString().take(6)}",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Save Location Pill
                val folderLabel = stampConfig.customFolderDisplayName ?: stampConfig.customFolderName.ifBlank { "Pictures" }
                Surface(
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (stampConfig.customFolderTreeUri != null) Cyan400 else Slate700),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { folderPickerLauncher.launch(null) }
                        .testTag("save_folder_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save: $folderLabel",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                // Map View Style Quick Switch Pill (Standard / Satellite / 3D / Street View)
                if (stampConfig.showMiniMap) {
                    Surface(
                        color = Color(0x99000000),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Cyan400.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .clickable { viewModel.cycleMapStyle() }
                            .testTag("camera_map_style_quick_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Switch Map Style",
                                tint = Cyan400,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stampConfig.mapStyle.badge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live Stamp HUD Overlay (Top or Bottom based on user settings)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (stampConfig.stampPosition == StampPosition.TOP) 72.dp else 0.dp,
                    bottom = if (stampConfig.stampPosition == StampPosition.BOTTOM) 112.dp else 0.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
            contentAlignment = if (stampConfig.stampPosition == StampPosition.TOP) Alignment.TopCenter else Alignment.BottomCenter
        ) {
            StampOverlayView(
                location = locationData,
                config = stampConfig,
                onEditNoteClick = {
                    editedNoteText = stampConfig.customNote
                    showNoteDialog = true
                },
                onCycleMapStyle = {
                    viewModel.cycleMapStyle()
                }
            )
        }

        // Bottom Camera Action Controls (Shutter, Gallery, Pick Photo)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            color = Color(0xCC070B14)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Thumbnail Shortcut
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate800)
                        .border(2.dp, Cyan400.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToGallery() }
                        .testTag("gallery_thumbnail_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (latestPhoto != null) {
                        AsyncImage(
                            model = File(latestPhoto!!.filePath),
                            contentDescription = "Latest Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Primary Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(ShutterRing)
                        .clickable(enabled = !isCapturing) {
                            imageCapture?.let { capture ->
                                viewModel.capturePhoto(capture)
                            } ?: run {
                                // If camera not bound (e.g. simulator), open picker or prompt
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                        .padding(4.dp)
                        .testTag("shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ShutterInner)
                            .border(3.dp, Slate950, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = Cyan500,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Capture Photo",
                                tint = Slate950,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Import Photo from Gallery & Stamp Button
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Slate800, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .testTag("import_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Stamp Existing Photo",
                        tint = Cyan400,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Quick Note Edit Dialog
        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                title = { Text("Customize Photo Note", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "This note will be stamped directly onto every photo you take.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editedNoteText,
                            onValueChange = { editedNoteText = it },
                            label = { Text("Custom Note / Project / Tag") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_input_field")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateQuickNote(editedNoteText)
                            showNoteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text("Save Note")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
