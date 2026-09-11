package com.example.stamp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import com.example.model.MapStyle
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

object RealMapTileFetcher {
    private const val TAG = "RealMapTileFetcher"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 GoogleMapsMobile/1.0"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    // 15 MB in-memory tile cache
    private val memoryCache = object : LruCache<String, Bitmap>(30) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    var cacheDir: File? = null

    fun getLyrsParam(style: MapStyle): String {
        return when (style) {
            MapStyle.NORMAL_STREET -> "m"
            MapStyle.SATELLITE -> "y" // Hybrid: Satellite photo + street labels
            MapStyle.VIEW_3D -> "p" // Terrain elevation contours & 3D relief
            MapStyle.STREET_VIEW -> "m,bike" // Street level view with street network
        }
    }

    fun getTileCoords(lat: Double, lng: Double, zoom: Int): Triple<Int, Int, Pair<Double, Double>> {
        val n = 1 shl zoom
        val latClamped = lat.coerceIn(-85.05112878, 85.05112878)
        val latRad = Math.toRadians(latClamped)
        val xExact = ((lng + 180.0) / 360.0 * n)
        val yExact = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n)
        val tileX = xExact.toInt()
        val tileY = yExact.toInt()
        val offsetX = (xExact - tileX) * 256.0
        val offsetY = (yExact - tileY) * 256.0
        return Triple(tileX, tileY, Pair(offsetX, offsetY))
    }

    fun fetchTileSync(tileX: Int, tileY: Int, zoom: Int, style: MapStyle): Bitmap? {
        val lyrs = getLyrsParam(style)
        val cacheKey = "tile_${lyrs}_${zoom}_${tileX}_${tileY}"

        // 1. Check memory cache
        memoryCache.get(cacheKey)?.let { return it }

        // 2. Check disk cache if available
        cacheDir?.let { dir ->
            val diskFile = File(dir, "$cacheKey.png")
            if (diskFile.exists() && diskFile.length() > 0) {
                try {
                    val bmp = BitmapFactory.decodeFile(diskFile.absolutePath)
                    if (bmp != null) {
                        memoryCache.put(cacheKey, bmp)
                        return bmp
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed reading disk tile: ${e.message}")
                }
            }
        }

        // 3. Fetch from Google Maps tile servers (rotate subdomains mt0..mt3)
        val subdomain = (tileX + tileY) % 4
        val url = "https://mt${subdomain}.google.com/vt/lyrs=${lyrs}&x=${tileX}&y=${tileY}&z=${zoom}"

        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        memoryCache.put(cacheKey, bitmap)

                        // Save to disk cache asynchronously
                        cacheDir?.let { dir ->
                            try {
                                if (!dir.exists()) dir.mkdirs()
                                val diskFile = File(dir, "$cacheKey.png")
                                FileOutputStream(diskFile).use { out ->
                                    out.write(bytes)
                                }
                            } catch (e: Exception) {
                                // Non-critical cache write error
                            }
                        }
                        return bitmap
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching Google Maps tile from $url: ${e.message}")
            null
        }
    }
}
