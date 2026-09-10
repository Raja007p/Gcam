package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import android.media.ExifInterface
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

    fun refreshLocation() {
        locationSensorManager.requestImmediateFix()
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

            // Save stamped photo permanently
            val outputDir = getOutputDirectory(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val stampedFile = File(outputDir, "GPS_STAMP_${timeStamp}.jpg")

            FileOutputStream(stampedFile).use { out ->
                stampedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
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

    private fun getOutputDirectory(context: Context): File {
        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
            File(it, "GPSMapCamera").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }
}
