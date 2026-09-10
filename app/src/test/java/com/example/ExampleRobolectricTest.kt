package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AltitudeUnit
import com.example.model.CoordinateFormat
import com.example.model.LocationData
import com.example.model.MapStyle
import com.example.model.StampConfig
import com.example.stamp.MiniMapRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `verify app name resource`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GPS Map Camera", appName)
  }

  @Test
  fun `verify location data DMS coordinate conversion`() {
    val lat = 35.434008
    val lng = -118.730612
    val dmsLat = LocationData.toDms(lat, isLatitude = true)
    val dmsLng = LocationData.toDms(lng, isLatitude = false)

    assertTrue("DMS Latitude should contain degrees", dmsLat.contains("35°"))
    assertTrue("DMS Latitude should end with N", dmsLat.endsWith("N"))
    assertTrue("DMS Longitude should contain degrees", dmsLng.contains("118°"))
    assertTrue("DMS Longitude should end with W", dmsLng.endsWith("W"))
  }

  @Test
  fun `verify compass degrees to direction`() {
    assertEquals("N", LocationData.degreesToDirection(0f))
    assertEquals("E", LocationData.degreesToDirection(90f))
    assertEquals("S", LocationData.degreesToDirection(180f))
    assertEquals("W", LocationData.degreesToDirection(270f))
    assertEquals("SW", LocationData.degreesToDirection(225f))
  }

  @Test
  fun `verify mini map renderer generates bitmap`() {
    val bitmap = MiniMapRenderer.generateMiniMapBitmap(
      width = 120,
      height = 120,
      latitude = 35.434008,
      longitude = -118.730612,
      mapStyle = MapStyle.NORMAL_STREET
    )

    assertNotNull(bitmap)
    assertEquals(120, bitmap.width)
    assertEquals(120, bitmap.height)
    bitmap.recycle()
  }

  @Test
  fun `verify default stamp config has all features enabled`() {
    val config = StampConfig()
    assertTrue(config.showMiniMap)
    assertTrue(config.showCoordinates)
    assertTrue(config.showAltitude)
    assertTrue(config.showCompass)
    assertTrue(config.showAddress)
    assertTrue(config.showDateTime)
  }
}

