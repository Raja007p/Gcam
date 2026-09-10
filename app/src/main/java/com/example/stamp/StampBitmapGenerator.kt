package com.example.stamp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.AltitudeUnit
import com.example.model.CoordinateFormat
import com.example.model.LocationData
import com.example.model.StampBgStyle
import com.example.model.StampConfig
import com.example.model.StampPosition
import kotlin.math.max

object StampBitmapGenerator {

    fun applyStampToBitmap(
        original: Bitmap,
        location: LocationData,
        config: StampConfig
    ): Bitmap {
        // Create a mutable copy of the bitmap for drawing
        val output = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val bmpWidth = output.width
        val bmpHeight = output.height

        // Scaling factor relative to 1080p base width
        val scale = max(0.65f, bmpWidth / 1080f)

        // Calculate heights and dimensions
        val padding = (24f * scale)
        val miniMapSize = if (config.showMiniMap) (190f * scale).toInt() else 0

        // Measure text sizes
        val titleTextSize = 26f * scale
        val bodyTextSize = 18f * scale
        val subTextSize = 16f * scale
        val metricTextSize = 17f * scale
        val badgeTextSize = 14f * scale

        val lineSpacing = 6f * scale

        // Calculate total stamp content height
        var textContentHeight = padding * 2
        if (config.showBadge && !config.showMiniMap) textContentHeight += badgeTextSize + lineSpacing + (10f * scale)
        if (config.showAddress) textContentHeight += titleTextSize + lineSpacing + (bodyTextSize * 1.8f)
        if (config.showDateTime) textContentHeight += subTextSize + lineSpacing
        if (config.showNote && config.customNote.isNotBlank()) textContentHeight += subTextSize + lineSpacing
        if (config.showCoordinates) textContentHeight += subTextSize + lineSpacing
        if (config.showAltitude || config.showCompass || config.showWeather) textContentHeight += metricTextSize + (14f * scale)

        val stampHeight = max(
            textContentHeight,
            if (config.showMiniMap) miniMapSize + (padding * 2) + (if (config.showBadge) 28f * scale else 0f) else 140f * scale
        )

        // Stamp rectangle
        val stampTop = if (config.stampPosition == StampPosition.BOTTOM) {
            bmpHeight - stampHeight
        } else {
            0f
        }
        val stampRect = RectF(0f, stampTop, bmpWidth.toFloat(), stampTop + stampHeight)

        // 1. Draw Background Overlay
        drawStampBackground(canvas, stampRect, config.bgStyle, scale)

        // Text paints
        val isLightBg = config.bgStyle == StampBgStyle.FROSTED_LIGHT
        val primaryTextColor = if (isLightBg) Color.rgb(15, 23, 42) else Color.WHITE
        val secondaryTextColor = if (isLightBg) Color.rgb(71, 85, 105) else Color.rgb(226, 232, 240)
        val accentColor = if (isLightBg) Color.rgb(2, 132, 199) else Color.rgb(56, 189, 248)
        val amberColor = Color.rgb(245, 158, 11)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = titleTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = bodyTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLightBg) Color.rgb(180, 83, 9) else amberColor
            textSize = subTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = subTextSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = subTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = metricTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // 2. Draw Left Column (Mini Map & Badge)
        var contentLeft = padding
        if (config.showMiniMap) {
            val mapLeft = padding
            var mapTop = stampTop + padding

            // Draw Badge above map if enabled
            if (config.showBadge && config.badgeText.isNotBlank()) {
                val badgeHeight = 22f * scale
                val badgeWidth = miniMapSize.toFloat()
                val badgeRect = RectF(mapLeft, mapTop, mapLeft + badgeWidth, mapTop + badgeHeight)

                val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(2, 132, 199) // Google vibrant blue
                }
                canvas.drawRoundRect(badgeRect, 6f * scale, 6f * scale, badgeBgPaint)

                val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = badgeTextSize
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    config.badgeText.uppercase(),
                    badgeRect.centerX(),
                    badgeRect.centerY() + (badgeTextSize * 0.35f),
                    badgeTextPaint
                )

                mapTop += badgeHeight + (6f * scale)
            }

            // Draw Mini-Map Bitmap
            val mapBitmap = MiniMapRenderer.generateMiniMapBitmap(
                miniMapSize,
                miniMapSize,
                location.latitude,
                location.longitude,
                config.mapStyle
            )
            canvas.drawBitmap(mapBitmap, mapLeft, mapTop, null)
            mapBitmap.recycle()

            contentLeft = mapLeft + miniMapSize + (18f * scale)
        }

        // 3. Draw Right Column (Address, Coordinates, Note, Date, Metrics)
        var currentY = stampTop + padding + titleTextSize

        // Title
        if (config.showAddress && location.title.isNotBlank()) {
            val titleText = location.title
            canvas.drawText(titleText, contentLeft, currentY, titlePaint)
            currentY += lineSpacing + bodyTextSize
        }

        // Detailed Address Line
        if (config.showAddress && location.addressLine.isNotBlank()) {
            // Truncate address if too long for the available width
            val maxTextWidth = bmpWidth - contentLeft - padding
            val truncatedAddress = truncateText(location.addressLine, addressPaint, maxTextWidth)
            canvas.drawText(truncatedAddress, contentLeft, currentY, addressPaint)
            currentY += lineSpacing + subTextSize + (4f * scale)
        }

        // Date & Time
        if (config.showDateTime) {
            val dateStr = location.getFormattedDateTime()
            canvas.drawText(dateStr, contentLeft, currentY, subPaint)
            currentY += lineSpacing + subTextSize + (2f * scale)
        }

        // Custom Note / Tag
        if (config.showNote && config.customNote.isNotBlank()) {
            val noteText = "Note : ${config.customNote}"
            val maxTextWidth = bmpWidth - contentLeft - padding
            val truncatedNote = truncateText(noteText, notePaint, maxTextWidth)
            canvas.drawText(truncatedNote, contentLeft, currentY, notePaint)
            currentY += lineSpacing + subTextSize + (2f * scale)
        }

        // Coordinates (DMS or Decimal)
        if (config.showCoordinates) {
            val coordStr = location.getCoordinatesFormatted(config.coordinateFormat)
            canvas.drawText(coordStr, contentLeft, currentY, coordPaint)
            currentY += lineSpacing + subTextSize + (6f * scale)
        }

        // Bottom Metrics Bar (Altitude, Compass, Weather, Speed)
        val metricsList = mutableListOf<String>()
        if (config.showAltitude) {
            metricsList.add("⛰️ ${location.getAltitudeFormatted(config.altitudeUnit)}")
        }
        if (config.showCompass) {
            metricsList.add("🧭 ${location.compassDegrees.toInt()}° ${location.compassDirection}")
        }
        if (config.showWeather) {
            metricsList.add("☀️ ${location.temperatureC}°C")
            metricsList.add("💧 ${location.humidityPercent}%")
        }
        if (location.speedKmh > 1.0f) {
            metricsList.add("💨 ${location.speedKmh.toInt()} km/h")
        }

        if (metricsList.isNotEmpty()) {
            val metricsCombined = metricsList.joinToString("   ")
            val maxTextWidth = bmpWidth - contentLeft - padding
            val truncatedMetrics = truncateText(metricsCombined, metricPaint, maxTextWidth)
            canvas.drawText(truncatedMetrics, contentLeft, currentY, metricPaint)
        }

        return output
    }

    private fun drawStampBackground(
        canvas: Canvas,
        rect: RectF,
        bgStyle: StampBgStyle,
        scale: Float
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (bgStyle) {
            StampBgStyle.TRANSLUCENT_DARK -> {
                bgPaint.color = Color.argb(195, 15, 23, 42) // 76% dark slate
                canvas.drawRect(rect, bgPaint)

                // Subtle glowing top accent line
                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(120, 56, 189, 248)
                    strokeWidth = 2f * scale
                }
                canvas.drawLine(rect.left, rect.top, rect.right, rect.top, accentPaint)
            }

            StampBgStyle.SOLID_BLACK -> {
                bgPaint.color = Color.rgb(10, 15, 26)
                canvas.drawRect(rect, bgPaint)

                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(160, 245, 158, 11)
                    strokeWidth = 2f * scale
                }
                canvas.drawLine(rect.left, rect.top, rect.right, rect.top, accentPaint)
            }

            StampBgStyle.FROSTED_LIGHT -> {
                bgPaint.color = Color.argb(220, 248, 250, 252)
                canvas.drawRect(rect, bgPaint)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(100, 203, 213, 225)
                    strokeWidth = 2f * scale
                }
                canvas.drawLine(rect.left, rect.top, rect.right, rect.top, borderPaint)
            }

            StampBgStyle.MINIMAL_BORDER -> {
                bgPaint.color = Color.argb(140, 15, 23, 42)
                canvas.drawRect(rect, bgPaint)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(180, 56, 189, 248)
                    style = Paint.Style.STROKE
                    strokeWidth = 3f * scale
                }
                canvas.drawRect(rect.left + 4f, rect.top + 4f, rect.right - 4f, rect.bottom - 4f, borderPaint)
            }
        }
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text

        var low = 0
        var high = text.length
        var best = text

        while (low <= high) {
            val mid = (low + high) / 2
            val candidate = text.substring(0, mid) + "…"
            if (paint.measureText(candidate) <= maxWidth) {
                best = candidate
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return best
    }
}
