package com.example.stamp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.example.model.MapStyle
import kotlin.math.roundToInt

object MiniMapRenderer {

    fun generateMiniMapBitmap(
        width: Int,
        height: Int,
        latitude: Double,
        longitude: Double,
        mapStyle: MapStyle
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val zoom = if (mapStyle == MapStyle.STREET_VIEW) 18 else 16
        val (tileX, tileY, offsets) = RealMapTileFetcher.getTileCoords(latitude, longitude, zoom)
        val (pixelOffsetX, pixelOffsetY) = offsets

        // Try to fetch real Google Maps center tile
        val centerTile = RealMapTileFetcher.fetchTileSync(tileX, tileY, zoom, mapStyle)

        if (centerTile != null) {
            // Draw real Google Maps tile(s) centered at (width/2, height/2)
            val halfW = width / 2f
            val halfH = height / 2f

            val tileLeft = halfW - pixelOffsetX.toFloat()
            val tileTop = halfH - pixelOffsetY.toFloat()

            // Draw center tile
            canvas.drawBitmap(centerTile, tileLeft, tileTop, null)

            // If necessary, fill edges with neighboring tiles for seamless coverage
            if (tileLeft > 0) {
                RealMapTileFetcher.fetchTileSync(tileX - 1, tileY, zoom, mapStyle)?.let {
                    canvas.drawBitmap(it, tileLeft - 256f, tileTop, null)
                }
            }
            if (tileLeft + 256f < width) {
                RealMapTileFetcher.fetchTileSync(tileX + 1, tileY, zoom, mapStyle)?.let {
                    canvas.drawBitmap(it, tileLeft + 256f, tileTop, null)
                }
            }
            if (tileTop > 0) {
                RealMapTileFetcher.fetchTileSync(tileX, tileY - 1, zoom, mapStyle)?.let {
                    canvas.drawBitmap(it, tileLeft, tileTop - 256f, null)
                }
            }
            if (tileTop + 256f < height) {
                RealMapTileFetcher.fetchTileSync(tileX, tileY + 1, zoom, mapStyle)?.let {
                    canvas.drawBitmap(it, tileLeft, tileTop + 256f, null)
                }
            }
        } else {
            // Graceful Cartographic Fallback when network is offline
            drawOfflineCartography(canvas, width, height, latitude, longitude, mapStyle)
        }

        // Draw iconic Google Maps marker pin in center
        drawMarkerPin(canvas, width / 2f, height / 2f, mapStyle)

        // Draw Map Frame and Google watermark
        drawMapFrame(canvas, width, height, mapStyle)

        return bitmap
    }

    private fun drawOfflineCartography(
        canvas: Canvas,
        w: Int,
        h: Int,
        lat: Double,
        lng: Double,
        mapStyle: MapStyle
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (mapStyle) {
                MapStyle.SATELLITE -> Color.rgb(24, 38, 30)
                MapStyle.VIEW_3D -> Color.rgb(30, 41, 59)
                MapStyle.STREET_VIEW -> Color.rgb(15, 23, 42)
                MapStyle.NORMAL_STREET -> Color.rgb(241, 238, 232)
            }
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Draw latitude and longitude coordinate grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mapStyle == MapStyle.NORMAL_STREET) Color.argb(45, 100, 116, 139) else Color.argb(40, 255, 255, 255)
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }

        val step = w / 4f
        for (i in 1..3) {
            val pos = i * step
            canvas.drawLine(pos, 0f, pos, h.toFloat(), gridPaint)
            canvas.drawLine(0f, pos, w.toFloat(), pos, gridPaint)
        }

        // Concentric GPS radar rings
        val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 14, 165, 233)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(w / 2f, h / 2f, w * 0.22f, radarPaint)
        canvas.drawCircle(w / 2f, h / 2f, w * 0.40f, radarPaint)

        // Coordinates text
        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mapStyle == MapStyle.NORMAL_STREET) Color.rgb(71, 85, 105) else Color.rgb(148, 163, 184)
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(String.format("%.4f°, %.4f°", lat, lng), w / 2f, h - 14f, coordPaint)
    }

    private fun drawMarkerPin(canvas: Canvas, cx: Float, cy: Float, mapStyle: MapStyle) {
        // Drop shadow under pin
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(cx - 10f, cy - 2f, cx + 10f, cy + 6f), shadowPaint)

        // Pulsing radar / accuracy ring
        val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 66, 133, 244)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 22f, radarPaint)

        // The iconic Google Map Pin Marker (Red #EA4335)
        val pinWidth = 26f
        val pinHeight = 36f
        val pinTop = cy - pinHeight

        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(234, 67, 53) // Google Maps iconic Red
            style = Paint.Style.FILL
        }

        val pinPath = Path().apply {
            moveTo(cx, cy) // Bottom pointed tip
            cubicTo(
                cx - pinWidth * 0.55f, cy - pinHeight * 0.4f,
                cx - pinWidth * 0.55f, pinTop + pinWidth * 0.2f,
                cx, pinTop
            )
            cubicTo(
                cx + pinWidth * 0.55f, pinTop + pinWidth * 0.2f,
                cx + pinWidth * 0.55f, cy - pinHeight * 0.4f,
                cx, cy
            )
            close()
        }
        canvas.drawPath(pinPath, pinPaint)

        // Darker shade on right side of pin for 3D realism
        val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val shadePath = Path().apply {
            moveTo(cx, cy)
            cubicTo(
                cx + pinWidth * 0.55f, cy - pinHeight * 0.4f,
                cx + pinWidth * 0.55f, pinTop + pinWidth * 0.2f,
                cx, pinTop
            )
            lineTo(cx, cy)
            close()
        }
        canvas.drawPath(shadePath, shadePaint)

        // Center white dot in marker head
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val headCenterY = pinTop + (pinWidth * 0.5f)
        canvas.drawCircle(cx, headCenterY, 5f, dotPaint)
    }

    private fun drawMapFrame(canvas: Canvas, w: Int, h: Int, mapStyle: MapStyle) {
        // Subtle outer border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, w - 1f, h - 1f, borderPaint)

        // Google watermark pill
        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
        }
        val bannerRect = RectF(6f, h - 20f, 48f, h - 5f)
        canvas.drawRoundRect(bannerRect, 3f, 3f, badgeBg)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(66, 133, 244)
            textSize = 9.5f
            isFakeBoldText = true
        }
        canvas.drawText("Google", 9f, h - 8f, textPaint)

        // Map mode badge at top right
        val modeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 15, 23, 42)
        }
        val modeRect = RectF(w - 38f, 5f, w - 5f, 19f)
        canvas.drawRoundRect(modeRect, 3f, 3f, modeBg)

        val modeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 189, 248)
            textSize = 8.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(mapStyle.badge, modeRect.centerX(), modeRect.centerY() + 3f, modeTextPaint)
    }
}
