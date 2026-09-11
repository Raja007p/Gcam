package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.LocationData
import com.example.model.StampConfig
import com.example.permission.PermissionManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationSensorManager(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val rotationSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private var gpsListener: LocationListener? = null
    private var networkListener: LocationListener? = null
    private var isListening = false

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var currentConfig: StampConfig? = null

    private var isLocationUpdatesActive = false

    fun updateConfig(config: StampConfig) {
        currentConfig = config
        if (config.useManualLocation) {
            _locationData.value = _locationData.value.copy(
                latitude = config.manualLatitude,
                longitude = config.manualLongitude,
                altitudeMeters = config.manualAltitude,
                title = config.manualTitle,
                addressLine = config.manualAddress,
                timestamp = System.currentTimeMillis(),
                isLiveFix = false,
                hasRealFix = true
            )
        } else {
            // Re-check permissions and immediately request a fresh fix if we are listening
            if (isListening && hasLocationPermission()) {
                requestImmediateFix()
                if (!isLocationUpdatesActive) {
                    startLocationUpdates()
                }
            }
        }
    }

    fun onPermissionGranted() {
        if (hasLocationPermission() && PermissionManager.isLocationServiceEnabled(context)) {
            startLocationUpdates()
            requestImmediateFix()
        } else if (!PermissionManager.isLocationServiceEnabled(context)) {
            onLocationServiceDisabled()
        }
    }

    fun onLocationServiceDisabled() {
        stopLocationUpdates()
        if (currentConfig?.useManualLocation != true) {
            _locationData.value = LocationData(
                latitude = 0.0,
                longitude = 0.0,
                altitudeMeters = 0.0,
                accuracyMeters = 0f,
                speedKmh = 0f,
                hasRealFix = false,
                isLiveFix = false,
                title = "Location is turned off",
                addressLine = "Location is turned off. Please turn on Location to use this feature."
            )
        }
    }

    fun startListening() {
        startSensors()
        isListening = true
        if (hasLocationPermission() && PermissionManager.isLocationServiceEnabled(context)) {
            startLocationUpdates()
            requestImmediateFix()
        } else if (!PermissionManager.isLocationServiceEnabled(context)) {
            onLocationServiceDisabled()
        }
    }

    fun stopLocationUpdates() {
        isLocationUpdatesActive = false
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
        gpsListener?.let {
            try {
                locationManager?.removeUpdates(it)
            } catch (e: Exception) {
                Log.w("LocationSensorManager", "Error removing GPS listener", e)
            }
            gpsListener = null
        }
        networkListener?.let {
            try {
                locationManager?.removeUpdates(it)
            } catch (e: Exception) {
                Log.w("LocationSensorManager", "Error removing network listener", e)
            }
            networkListener = null
        }
    }

    fun stopListening() {
        isListening = false
        stopLocationUpdates()
        sensorManager?.unregisterListener(this)
    }

    private fun startSensors() {
        if (rotationSensor != null) {
            sensorManager?.registerListener(
                this,
                rotationSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
            accelerometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            magnetometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return PermissionManager.isLocationPermissionGranted(context)
    }

    private fun isLocationFresh(location: Location): Boolean {
        // Only consider a cached location valid if recorded in the last 60 seconds.
        // Prevents using stale, random, or old locations when GPS was disabled or lost.
        val ageMs = System.currentTimeMillis() - location.time
        return ageMs in 0..60_000L
    }

    @SuppressLint("MissingPermission")
    fun requestImmediateFix() {
        if (!hasLocationPermission() || !PermissionManager.isLocationServiceEnabled(context)) {
            Log.d("LocationSensorManager", "Skipping immediate fix: permission missing or location service off.")
            if (!PermissionManager.isLocationServiceEnabled(context)) {
                onLocationServiceDisabled()
            }
            return
        }

        try {
            // 1. Check lastLocation from Google FusedLocationProviderClient only if strictly fresh
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    if (isLocationFresh(it)) {
                        handleNewLocation(it)
                    } else {
                        Log.d("LocationSensorManager", "Ignored stale cached location (${(System.currentTimeMillis() - it.time) / 1000}s old)")
                    }
                }
            }

            // 2. Request active getCurrentLocation for an instant accurate fix
            val cts = CancellationTokenSource()
            val currentRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(10000)
                .setDurationMillis(10000)
                .build()

            fusedLocationClient.getCurrentLocation(currentRequest, cts.token)
                .addOnSuccessListener { loc ->
                    loc?.let { handleNewLocation(it) }
                }

            // 3. Fallback: check last known locations from standard LocationManager only if strictly fresh
            locationManager?.let { lm ->
                val providers = lm.getProviders(true)
                for (provider in providers) {
                    val last = lm.getLastKnownLocation(provider)
                    if (last != null && isLocationFresh(last)) {
                        handleNewLocation(last)
                        break
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w("LocationSensorManager", "Location permission missing during immediate fix: ${e.message}")
        } catch (e: Exception) {
            Log.e("LocationSensorManager", "Error in requestImmediateFix", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission() || !PermissionManager.isLocationServiceEnabled(context)) {
            Log.d("LocationSensorManager", "Cannot start location updates: permission not granted or location service off.")
            return
        }

        try {
            isLocationUpdatesActive = true

            // Immediately request fresh fix
            requestImmediateFix()

            // Remove existing callback if any to prevent duplicate streams
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
            }

            // Continuous real-time updates via Google Play Services Fused Location Client
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .setMinUpdateDistanceMeters(0f)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { handleNewLocation(it) }
                }
            }
            locationCallback = callback

            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )

            // Direct device hardware GPS Provider listener fallback
            locationManager?.let { lm ->
                gpsListener?.let { try { lm.removeUpdates(it) } catch (_: Exception) {} }
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    val gps = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            handleNewLocation(location)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    gpsListener = gps
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L,
                        0f,
                        gps,
                        Looper.getMainLooper()
                    )
                }

                // Network Provider fallback for rapid indoor / cellular fix
                networkListener?.let { try { lm.removeUpdates(it) } catch (_: Exception) {} }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    val net = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            handleNewLocation(location)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    networkListener = net
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000L,
                        0f,
                        net,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w("LocationSensorManager", "Location permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e("LocationSensorManager", "Error starting location updates", e)
        }
    }

    private fun handleNewLocation(location: Location) {
        if (currentConfig?.useManualLocation == true) {
            return
        }

        val lat = location.latitude
        val lng = location.longitude
        val alt = if (location.hasAltitude()) location.altitude else _locationData.value.altitudeMeters
        val acc = if (location.hasAccuracy()) location.accuracy else _locationData.value.accuracyMeters
        val spd = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val bearing = if (location.hasBearing()) location.bearing else _locationData.value.bearingDegrees

        val interimTitle = if (_locationData.value.hasRealFix && _locationData.value.title != "Acquiring GPS...") {
            _locationData.value.title
        } else {
            String.format(Locale.US, "GPS: %.4f, %.4f", lat, lng)
        }
        val interimAddress = if (_locationData.value.hasRealFix && !_locationData.value.addressLine.startsWith("Locating")) {
            _locationData.value.addressLine
        } else {
            val latCard = if (lat >= 0) "N" else "S"
            val lngCard = if (lng >= 0) "E" else "W"
            String.format(Locale.US, "%.5f° %s, %.5f° %s (±%.1fm)", Math.abs(lat), latCard, Math.abs(lng), lngCard, acc)
        }

        _locationData.value = _locationData.value.copy(
            latitude = lat,
            longitude = lng,
            altitudeMeters = alt,
            accuracyMeters = acc,
            speedKmh = spd,
            bearingDegrees = bearing,
            title = interimTitle,
            addressLine = interimAddress,
            timestamp = System.currentTimeMillis(),
            isLiveFix = true,
            hasRealFix = true
        )

        // Reverse geocode in background
        scope.launch(Dispatchers.IO) {
            reverseGeocode(lat, lng)
        }
    }

    private suspend fun reverseGeocode(lat: Double, lng: Double) {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            applyAddress(addresses[0])
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        applyAddress(addresses[0])
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LocationSensorManager", "Geocode failed", e)
        }
    }

    private fun applyAddress(address: Address) {
        val admin = address.adminArea ?: ""
        val country = address.countryName ?: ""
        val locality = address.locality ?: address.subAdminArea ?: ""
        val thoroughfare = address.thoroughfare ?: ""
        val feature = address.featureName ?: ""

        val title = when {
            locality.isNotEmpty() && country.isNotEmpty() -> "$locality, $country"
            admin.isNotEmpty() && country.isNotEmpty() -> "$admin, $country"
            country.isNotEmpty() -> country
            else -> "GPS Location"
        }

        val fullAddress = address.getAddressLine(0) ?: run {
            val parts = listOfNotNull(
                thoroughfare.ifEmpty { feature },
                locality.ifEmpty { null },
                admin.ifEmpty { null },
                country.ifEmpty { null }
            )
            parts.joinToString(", ")
        }

        scope.launch(Dispatchers.Main) {
            _locationData.value = _locationData.value.copy(
                title = title,
                addressLine = fullAddress
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthRad = orientation[0]
            var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            if (azimuthDeg < 0) azimuthDeg += 360f

            updateCompass(azimuthDeg)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3)
            hasGravity = true
            calculateAzimuth()
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            hasGeomagnetic = true
            calculateAzimuth()
        }
    }

    private fun calculateAzimuth() {
        if (hasGravity && hasGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f
                updateCompass(azimuthDeg)
            }
        }
    }

    private fun updateCompass(azimuthDeg: Float) {
        val direction = LocationData.degreesToDirection(azimuthDeg)
        _locationData.value = _locationData.value.copy(
            compassDegrees = azimuthDeg,
            compassDirection = direction
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
