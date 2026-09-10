package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CapturedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val photoDao = AppDatabase.getInstance(application).photoDao()

    val photos: StateFlow<List<CapturedPhoto>> = photoDao.getAllPhotos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deletePhoto(photo: CapturedPhoto) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(photo.filePath)
                if (file.exists()) {
                    file.delete()
                }
                photoDao.deletePhoto(photo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getShareUri(context: Context, photo: CapturedPhoto): Uri? {
        return try {
            val file = File(photo.filePath)
            if (!file.exists()) return null
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sharePhoto(context: Context, photo: CapturedPhoto) {
        val uri = getShareUri(context, photo) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Captured with GPS Map Camera:\n📍 ${photo.addressLine}\n🌐 Lat: ${photo.latitude}, Long: ${photo.longitude}\n⛰️ Alt: ${photo.altitudeMeters.toInt()}m")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share GPS Stamped Photo")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
