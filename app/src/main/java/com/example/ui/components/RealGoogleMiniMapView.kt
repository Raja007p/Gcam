package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.LocationData
import com.example.model.MapStyle
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

private val LiveGreen = Color(0xFF10B981)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RealGoogleMiniMapView(
    location: LocationData,
    mapStyle: MapStyle,
    modifier: Modifier = Modifier,
    onMapClick: (() -> Unit)? = null,
    onCycleStyle: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    // Build the initial self-contained HTML containing real Google Maps tiles & Leaflet
    val initialHtml = remember {
        generateMapHtml(location.latitude, location.longitude, mapStyle)
    }

    // Real-time synchronization: When GPS coordinates change, glide to the new location
    LaunchedEffect(location.latitude, location.longitude, location.bearingDegrees) {
        webViewRef?.let { webView ->
            val js = "if (window.updateLocation) { window.updateLocation(${location.latitude}, ${location.longitude}, ${location.bearingDegrees}); }"
            webView.evaluateJavascript(js, null)
        }
    }

    // Real-time synchronization: When map style is switched, update map layer
    LaunchedEffect(mapStyle) {
        webViewRef?.let { webView ->
            val js = "if (window.setMapStyle) { window.setMapStyle('${mapStyle.name}'); }"
            webView.evaluateJavascript(js, null)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .background(Slate950)
            .testTag("real_google_mini_map")
    ) {
        // Real Google Map WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        displayZoomControls = false
                        builtInZoomControls = false
                        allowContentAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                            view?.evaluateJavascript("if (window.updateLocation) { window.updateLocation(${location.latitude}, ${location.longitude}, ${location.bearingDegrees}); }", null)
                            view?.evaluateJavascript("if (window.setMapStyle) { window.setMapStyle('${mapStyle.name}'); }", null)
                        }
                    }
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onMapTapped() {
                            onMapClick?.invoke()
                        }
                    }, "AndroidBridge")

                    loadDataWithBaseURL("https://maps.google.com/", initialHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { view ->
                webViewRef = view
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay transparent click sink if onMapClick is provided
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onMapClick?.invoke() }
        )

        // Top-Left: Map Mode Switcher Pill (Tap to cycle between Standard, Satellite, 3D, Street View)
        Surface(
            color = Slate900.copy(alpha = 0.9f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clickable { onCycleStyle?.invoke() }
                .testTag("cycle_map_style_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Change map style",
                    tint = Cyan400,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = mapStyle.badge,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top-Right: Open in Google Maps icon
        Surface(
            color = Slate900.copy(alpha = 0.85f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(18.dp)
                .clickable { onMapClick?.invoke() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Open Full Google Maps",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // Bottom-Left: LIVE GPS status dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .background(Slate950.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(LiveGreen, CircleShape)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "LIVE",
                color = LiveGreen,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom-Right: Google Watermark
        Text(
            text = "Google",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                .padding(horizontal = 2.dp)
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}

private fun generateMapHtml(initialLat: Double, initialLng: Double, initialStyle: MapStyle): String {
    return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<style>
  html, body {
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 0;
    overflow: hidden;
    background: #070b14;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  }
  
  #map-container {
    width: 100%;
    height: 100%;
    position: relative;
    overflow: hidden;
  }
  
  #map {
    width: 100%;
    height: 100%;
    background: #0f172a;
    transition: transform 0.4s ease;
  }
  
  /* 3D View perspective tilt */
  .view-3d-active #map {
    transform: perspective(350px) rotateX(24deg) scale(1.15);
    transform-origin: 50% 80%;
  }

  /* Custom Google Maps Live Location Beacon */
  .gps-marker-container {
    position: relative;
    width: 24px;
    height: 24px;
    margin-left: -12px;
    margin-top: -12px;
    pointer-events: none;
  }
  .gps-marker-pulse {
    position: absolute;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: rgba(66, 133, 244, 0.45);
    animation: gps-pulse 2s infinite cubic-bezier(0.2, 0.8, 0.2, 1);
  }
  .gps-marker-dot {
    position: absolute;
    top: 5px;
    left: 5px;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: #1a73e8;
    border: 2px solid #ffffff;
    box-shadow: 0 2px 6px rgba(0,0,0,0.6);
  }
  .gps-marker-heading {
    position: absolute;
    top: -1px;
    left: 6px;
    width: 0;
    height: 0;
    border-left: 6px solid transparent;
    border-right: 6px solid transparent;
    border-bottom: 9px solid #1a73e8;
    transform-origin: 50% 13px;
    display: none;
  }
  @keyframes gps-pulse {
    0% { transform: scale(0.5); opacity: 0.95; }
    80% { transform: scale(1.5); opacity: 0.0; }
    100% { transform: scale(1.5); opacity: 0.0; }
  }

  /* Canvas direct fallback if Leaflet JS is blocked */
  #fallback-canvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    display: none;
  }
</style>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
</head>
<body>
<div id="map-container">
  <div id="map"></div>
  <canvas id="fallback-canvas"></canvas>
</div>

<script>
(function() {
  var curLat = $initialLat;
  var curLng = $initialLng;
  var curStyle = '${initialStyle.name}';
  var curHeading = 0;
  var map = null;
  var marker = null;
  var currentLayer = null;

  function initLeafletMap() {
    if (typeof L === 'undefined') {
      initDirectTileFallback();
      return;
    }

    try {
      map = L.map('map', {
        center: [curLat, curLng],
        zoom: 16,
        zoomControl: false,
        attributionControl: false,
        dragging: true,
        touchZoom: true,
        doubleClickZoom: false,
        scrollWheelZoom: true
      });

      // Define real Google Maps tile layers
      window.googleLayers = {
        'NORMAL_STREET': L.tileLayer('https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
          subdomains: ['0','1','2','3'],
          maxZoom: 20
        }),
        'SATELLITE': L.tileLayer('https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
          subdomains: ['0','1','2','3'],
          maxZoom: 20
        }),
        'VIEW_3D': L.tileLayer('https://mt{s}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}', {
          subdomains: ['0','1','2','3'],
          maxZoom: 20
        }),
        'STREET_VIEW': L.tileLayer('https://mt{s}.google.com/vt/lyrs=m,bike&x={x}&y={y}&z={z}', {
          subdomains: ['0','1','2','3'],
          maxZoom: 20
        })
      };

      currentLayer = window.googleLayers[curStyle] || window.googleLayers['NORMAL_STREET'];
      currentLayer.addTo(map);

      // Add animated GPS location pin
      var gpsIcon = L.divIcon({
        className: 'gps-custom-icon',
        html: '<div class="gps-marker-container"><div class="gps-marker-pulse"></div><div class="gps-marker-dot"></div><div id="marker-heading" class="gps-marker-heading"></div></div>',
        iconSize: [24, 24],
        iconAnchor: [12, 12]
      });

      marker = L.marker([curLat, curLng], { icon: gpsIcon }).addTo(map);

      map.on('click', function() {
        if (window.AndroidBridge && window.AndroidBridge.onMapTapped) {
          window.AndroidBridge.onMapTapped();
        }
      });

      applyStyleClasses(curStyle);
    } catch(e) {
      initDirectTileFallback();
    }
  }

  function applyStyleClasses(style) {
    var container = document.getElementById('map-container');
    if (style === 'VIEW_3D') {
      container.classList.add('view-3d-active');
      if (map) map.setZoom(17);
    } else if (style === 'STREET_VIEW') {
      container.classList.remove('view-3d-active');
      if (map) map.setZoom(18);
    } else if (style === 'SATELLITE') {
      container.classList.remove('view-3d-active');
      if (map) map.setZoom(17);
    } else {
      container.classList.remove('view-3d-active');
      if (map) map.setZoom(16);
    }
  }

  window.setMapStyle = function(style) {
    curStyle = style;
    if (map && window.googleLayers) {
      if (currentLayer) map.removeLayer(currentLayer);
      currentLayer = window.googleLayers[style] || window.googleLayers['NORMAL_STREET'];
      currentLayer.addTo(map);
      applyStyleClasses(style);
      map.invalidateSize();
    } else {
      drawCanvasFallback();
    }
  };

  window.updateLocation = function(lat, lng, heading) {
    curLat = lat;
    curLng = lng;
    if (heading) curHeading = heading;

    if (map && marker) {
      var newPos = new L.LatLng(lat, lng);
      marker.setLatLng(newPos);
      map.panTo(newPos, { animate: true, duration: 0.6 });

      var headingElem = document.getElementById('marker-heading');
      if (headingElem && curHeading > 0) {
        headingElem.style.display = 'block';
        headingElem.style.transform = 'rotate(' + curHeading + 'deg)';
      }
    } else {
      drawCanvasFallback();
    }
  };

  // Direct HTML5 Canvas Tile Fallback for instant offline rendering
  function initDirectTileFallback() {
    var canvas = document.getElementById('fallback-canvas');
    var mapDiv = document.getElementById('map');
    if (canvas && mapDiv) {
      mapDiv.style.display = 'none';
      canvas.style.display = 'block';
      drawCanvasFallback();
    }
  }

  function drawCanvasFallback() {
    var canvas = document.getElementById('fallback-canvas');
    if (!canvas) return;
    var ctx = canvas.getContext('2d');
    var w = canvas.width = canvas.clientWidth || 200;
    var h = canvas.height = canvas.clientHeight || 200;

    var lyrs = 'm';
    if (curStyle === 'SATELLITE') lyrs = 'y';
    else if (curStyle === 'VIEW_3D') lyrs = 'p';
    else if (curStyle === 'STREET_VIEW') lyrs = 'm,bike';

    // Slippy map tile math
    var zoom = 16;
    var n = Math.pow(2, zoom);
    var latRad = curLat * Math.PI / 180;
    var xExact = (curLng + 180.0) / 360.0 * n;
    var yExact = (1.0 - Math.asinh(Math.tan(latRad)) / Math.PI) / 2.0 * n;
    var tileX = Math.floor(xExact);
    var tileY = Math.floor(yExact);
    var offsetX = (xExact - tileX) * 256;
    var offsetY = (yExact - tileY) * 256;

    var img = new Image();
    img.crossOrigin = 'anonymous';
    img.src = 'https://mt1.google.com/vt/lyrs=' + lyrs + '&x=' + tileX + '&y=' + tileY + '&z=' + zoom;
    img.onload = function() {
      ctx.clearRect(0, 0, w, h);
      ctx.drawImage(img, w/2 - offsetX, h/2 - offsetY, 256, 256);
      drawFallbackMarker(ctx, w/2, h/2);
    };
    img.onerror = function() {
      // Offline grid
      ctx.fillStyle = '#1e293b';
      ctx.fillRect(0, 0, w, h);
      ctx.strokeStyle = '#38bdf8';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(w/2, h/2, 30, 0, Math.PI*2);
      ctx.stroke();
      drawFallbackMarker(ctx, w/2, h/2);
    };
  }

  function drawFallbackMarker(ctx, cx, cy) {
    ctx.fillStyle = 'rgba(66, 133, 244, 0.4)';
    ctx.beginPath();
    ctx.arc(cx, cy, 14, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#1a73e8';
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(cx, cy, 6, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
  }

  window.addEventListener('load', initLeafletMap);
  setTimeout(function() {
    if (!map) initLeafletMap();
  }, 100);
})();
</script>
</body>
</html>
    """.trimIndent()
}
