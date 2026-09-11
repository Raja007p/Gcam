package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import android.media.ExifInterface
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CapturedPhoto
import com.example.data.LocationSensorManager
import com.example.data.PreferencesRepository
import com.example.model.LocationData
import com.example.model.StampConfig
import com.example.stamp.StampBitmapGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class CameraUiEvent {
    data class PhotoSaved(val photo: CapturedPhoto) : CameraUiEvent()
    data class Error(val message: String) : CameraUiEvent()
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val photoDao = AppDatabase.getInstance(application).photoDao()
    val locationSensorManager = LocationSensorManager(application, viewModelScope)

    val stampConfig: StateFlow<StampConfig> = prefsRepo.stampConfig
    val locationData: StateFlow<LocationData> = locationSensorManager.locationData

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_OFF)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _latestPhoto = MutableStateFlow<CapturedPhoto?>(null)
    val latestPhoto: StateFlow<CapturedPhoto?> = _latestPhoto.asStateFlow()

    private val _uiEvents = MutableSharedFlow<CameraUiEvent>()
    val uiEvents: SharedFlow<CameraUiEvent> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            stampConfig.collect { config ->
                locationSensorManager.updateConfig(config)
            }
        }
        viewModelScope.launch {
            photoDao.getAllPhotos().collect { photos ->
                _latestPhoto.value = photos.firstOrNull()
            }
        }
    }

    fun startSensors() {
        locationSensorManager.startListening()
    }

    fun stopSensors() {
        locationSensorManager.stopListening()
    }

    fun onLocationPermissionGranted() {
        locationSensorManager.onPermissionGranted()
    }

    fun refreshLocation() {
        locationSensorManager.requestImmediateFix()
    }

    fun setCustomFolderTree(uri: Uri, displayName: String) {
        val updated = stampConfig.value.copy(
            customFolderTreeUri = uri.toString(),
            customFolderDisplayName = displayName,
            customFolderName = displayName
        )
        prefsRepo.updateConfig(updated)
    }

    fun clearCustomFolderTree() {
        val updated = stampConfig.value.copy(
            customFolderTreeUri = null,
            customFolderDisplayName = null,
            customFolderName = "GPSMapCamera"
        )
        prefsRepo.updateConfig(updated)
    }

    fun toggleCamera() {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun cycleFlash() {
        _flashMode.value = when (_flashMode.value) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
    }

    fun updateConfig(newConfig: StampConfig) {
        prefsRepo.updateConfig(newConfig)
    }

    fun updateQuickNote(note: String) {
        val updated = stampConfig.value.copy(customNote = note)
        prefsRepo.updateConfig(updated)
    }

    fun capturePhoto(imageCapture: ImageCapture) {
        if (_isCapturing.value) return
        _isCapturing.value = true

        val context = getApplication<Application>()
        val outputDir = getOutputDirectory(context)
        val tempFile = File(outputDir, "TEMP_${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val savedPhoto = stampAndSaveFile(tempFile, context)
                            tempFile.delete()
                            _isCapturing.value = false
                            _uiEvents.emit(CameraUiEvent.PhotoSaved(savedPhoto))
                        } catch (e: Exception) {
                            Log.e("CameraViewModel", "Failed to stamp photo", e)
                            _isCapturing.value = false
                            _uiEvents.emit(CameraUiEvent.Error("Failed to stamp photo: ${e.localizedMessage}"))
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraViewModel", "Capture failed", exception)
                    _isCapturing.value = false
                    viewModelScope.launch {
                        _uiEvents.emit(CameraUiEvent.Error("Capture failed: ${exception.localizedMessage}"))
                    }
                }
            }
        )
    }

    fun processPickedImage(uri: Uri) {
        _isCapturing.value = true
        val context = getApplication<Application>()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    _isCapturing.value = false
                    _uiEvents.emit(CameraUiEvent.Error("Could not decode selected image"))
                    return@launch
                }

                val stampedBitmap = StampBitmapGenerator.applyStampToBitmap(
                    originalBitmap,
                    locationData.value,
                    stampConfig.value
                )

                val outputDir = getOutputDirectory(context)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val finalFile = File(outputDir, "GPS_STAMP_${timeStamp}.jpg")

                FileOutputStream(finalFile).use { out ->
                    stampedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }

                val currentConfig = stampConfig.value
                if (!currentConfig.customFolderTreeUri.isNullOrBlank()) {
                    try {
                        val treeUri = Uri.parse(currentConfig.customFolderTreeUri)
                        val dirDoc = DocumentFile.fromTreeUri(context, treeUri)
                        if (dirDoc != null && dirDoc.canWrite()) {
                            val safFile = dirDoc.createFile("image/jpeg", "GPS_STAMP_${timeStamp}.jpg")
                            if (safFile != null) {
                                context.contentResolver.openOutputStream(safFile.uri)?.use { out ->
                                    stampedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CameraViewModel", "Failed writing imported photo to SAF tree", e)
                    }
                }

                if (currentConfig.saveToGallery) {
                    savePhotoToMediaStore(
                        context = context,
                        bitmap = stampedBitmap,
                        fileName = "GPS_STAMP_${timeStamp}.jpg",
                        folderName = currentConfig.customFolderName,
                        location = locationData.value
                    )
                }

                stampedBitmap.recycle()
                originalBitmap.recycle()

                val capturedPhoto = CapturedPhoto(
                    filePath = finalFile.absolutePath,
                    timestamp = System.currentTimeMillis(),
                    latitude = locationData.value.latitude,
                    longitude = locationData.value.longitude,
                    altitudeMeters = locationData.value.altitudeMeters,
                    addressTitle = locationData.value.title,
                    addressLine = locationData.value.addressLine,
                    note = stampConfig.value.customNote,
                    compassDegrees = locationData.value.compassDegrees
                )

                photoDao.insertPhoto(capturedPhoto)
                _latestPhoto.value = capturedPhoto
                _isCapturing.value = false
                _uiEvents.emit(CameraUiEvent.PhotoSaved(capturedPhoto))
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error processing picked image", e)
                _isCapturing.value = false
                _uiEvents.emit(CameraUiEvent.Error("Failed to stamp imported image: ${e.localizedMessage}"))
            }
        }
    }

    private suspend fun stampAndSaveFile(tempFile: File, context: Context): CapturedPhoto =
        withContext(Dispatchers.IO) {
            // Load bitmap with correct orientation from Exif
            val exif = ExifInterface(tempFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rawBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            val rotatedBitmap = rotateBitmap(rawBitmap, orientation)

            // Stamp bitmap
            val currentLocation = locationData.value
            val currentConfig = stampConfig.value

            val stampedBitmap = StampBitmapGenerator.applyStampToBitmap(
                rotatedBitmap,
                currentLocation,
                currentConfig
            )

            // Save stamped photo permanently to app storage (custom folder)
            val outputDir = getOutputDirectory(context, currentConfig.customFolderName)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "GPS_${timeStamp}.jpg"
            val stampedFile = File(outputDir, fileName)

            FileOutputStream(stampedFile).use { out ->
                stampedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            // If user selected a custom save folder via Android SAF folder picker, write to it
            if (!currentConfig.customFolderTreeUri.isNullOrBlank()) {
                try {
                    val treeUri = Uri.parse(currentConfig.customFolderTreeUri)
                    val dirDoc = DocumentFile.fromTreeUri(context, treeUri)
                    if (dirDoc != null && dirDoc.canWrite()) {
                        val safFile = dirDoc.createFile("image/jpeg", fileName)
                        if (safFile != null) {
                            context.contentResolver.openOutputStream(safFile.uri)?.use { out ->
                                stampedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                            }
                            Log.d("CameraViewModel", "Saved to user SAF directory: ${safFile.uri}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CameraViewModel", "Failed to write to custom SAF directory: ${e.message}", e)
                }
            }

            // Also export directly to user device's Gallery (DCIM/Pictures) so it appears immediately in the system Gallery app
            if (currentConfig.saveToGallery) {
                savePhotoToMediaStore(
                    context = context,
                    bitmap = stampedBitmap,
                    fileName = fileName,
                    folderName = currentConfig.customFolderName,
                    location = currentLocation
                )
            }

            // Broadcast to Android media scanner so file explorer and gallery pick up the new photo
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(stampedFile.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )
            } catch (e: Exception) {
                Log.w("CameraViewModel", "Media scanner scan failed: ${e.message}")
            }

            stampedBitmap.recycle()
            if (rotatedBitmap != rawBitmap) {
                rotatedBitmap.recycle()
            }
            rawBitmap.recycle()

            val photoRecord = CapturedPhoto(
                filePath = stampedFile.absolutePath,
                timestamp = System.currentTimeMillis(),
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                altitudeMeters = currentLocation.altitudeMeters,
                addressTitle = currentLocation.title,
                addressLine = currentLocation.addressLine,
                note = currentConfig.customNote,
                compassDegrees = currentLocation.compassDegrees
            )

            photoDao.insertPhoto(photoRecord)
            photoRecord
        }

    private fun savePhotoToMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        folderName: String,
        location: LocationData
    ) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.LATITUDE, location.latitude)
                put(MediaStore.Images.Media.LONGITUDE, location.longitude)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = "${Environment.DIRECTORY_PICTURES}/${folderName.ifBlank { "GPSMapCamera" }}"
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Error saving to MediaStore gallery: ${e.message}", e)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun getOutputDirectory(context: Context, folderName: String = "GPSMapCamera"): File {
        val sanitizedFolder = folderName.trim().ifEmpty { "GPSMapCamera" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        
        // Priority 1: Public external Pictures directory
        val publicPics = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (publicPics != null) {
            val customDir = File(publicPics, sanitizedFolder)
            if (customDir.exists() || customDir.mkdirs()) {
                return customDir
            }
        }

        // Priority 2: App external files dir
        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
            File(it, sanitizedFolder).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }
}
