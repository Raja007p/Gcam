package com.example.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

data class PermissionStatus(
    val isCameraGranted: Boolean = false,
    val isLocationGranted: Boolean = false,
    val isLocationServiceEnabled: Boolean = false,
    val isCameraPermanentlyDenied: Boolean = false,
    val isLocationPermanentlyDenied: Boolean = false
) {
    val arePermissionsGranted: Boolean
        get() = isCameraGranted && isLocationGranted

    val isFullyReady: Boolean
        get() = isCameraGranted && isLocationGranted && isLocationServiceEnabled

    val hasAnyPermanentDenial: Boolean
        get() = isCameraPermanentlyDenied || isLocationPermanentlyDenied
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

object PermissionManager {

    private const val PREFS_NAME = "gps_camera_permissions"
    private const val KEY_REQUESTED_CAMERA = "has_requested_camera"
    private const val KEY_REQUESTED_LOCATION = "has_requested_location"

    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationPermissionGranted(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    fun checkStatus(context: Context, activity: Activity?): PermissionStatus {
        val cameraGranted = isCameraPermissionGranted(context)
        val locationGranted = isLocationPermissionGranted(context)
        val locationServiceEnabled = isLocationServiceEnabled(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasRequestedCamera = prefs.getBoolean(KEY_REQUESTED_CAMERA, false)
        val hasRequestedLocation = prefs.getBoolean(KEY_REQUESTED_LOCATION, false)

        val cameraPermanentDenied = if (activity != null && hasRequestedCamera && !cameraGranted) {
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        } else false

        val locationPermanentDenied = if (activity != null && hasRequestedLocation && !locationGranted) {
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } else false

        return PermissionStatus(
            isCameraGranted = cameraGranted,
            isLocationGranted = locationGranted,
            isLocationServiceEnabled = locationServiceEnabled,
            isCameraPermanentlyDenied = cameraPermanentDenied,
            isLocationPermanentlyDenied = locationPermanentDenied
        )
    }

    fun markCameraRequested(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REQUESTED_CAMERA, true)
            .apply()
    }

    fun markLocationRequested(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REQUESTED_LOCATION, true)
            .apply()
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openLocationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
