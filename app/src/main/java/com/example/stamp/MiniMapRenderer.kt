package com.example.stamp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.example.model.MapStyle
import kotlin.math.abs
import kotlin.math.sin

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

        drawMapBackground(canvas, width, height, latitude, longitude, mapStyle)
        drawRoadsAndFeatures(canvas, width, height, latitude, longitude, mapStyle)
        drawMarkerPin(canvas, width / 2f, height / 2f, mapStyle)
        drawMapFrame(canvas, width, height)

        return bitmap
    }

    private fun drawMapBackground(
        canvas: Canvas,
        w: Int,
        h: Int,
        lat: Double,
        lng: Double,
        mapStyle: MapStyle
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (mapStyle) {
            MapStyle.NORMAL_STREET -> {
                // Classic Google Maps street palette: Light warm neutral
                paint.color = Color.rgb(241, 238, 232)
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                // Park / Green area
                val parkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(212, 232, 204)
                }
                val parkPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.45f, 0f)
                    cubicTo(w * 0.4f, h * 0.25f, w * 0.25f, h * 0.35f, 0f, h * 0.4f)
                    close()
                }
                canvas.drawPath(parkPath, parkPaint)

                // Water body
                val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(170, 218, 255)
                }
                val waterPath = Path().apply {
                    moveTo(w.toFloat(), h * 0.55f)
                    cubicTo(w * 0.8f, h * 0.65f, w * 0.7f, h * 0.85f, w * 0.5f, h.toFloat())
                    lineTo(w.toFloat(), h.toFloat())
                    close()
                }
                canvas.drawPath(waterPath, waterPaint)
            }

            MapStyle.SATELLITE -> {
                // Deep satellite earth tones
                val satGradient = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(Color.rgb(36, 52, 40), Color.rgb(22, 34, 28), Color.rgb(44, 58, 48)),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = satGradient
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                // Agricultural / Field textures
                val fieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(45, 120, 150, 100)
                }
                canvas.drawRect(w * 0.1f, w * 0.1f, w * 0.4f, h * 0.5f, fieldPaint)
                canvas.drawRect(w * 0.6f, w * 0.2f, w * 0.9f, h * 0.6f, fieldPaint)

                // Water coast
                val coastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(18, 40, 62)
                }
                val coastPath = Path().apply {
                    moveTo(w * 0.7f, 0f)
                    cubicTo(w * 0.75f, h * 0.4f, w * 0.85f, h * 0.6f, w.toFloat(), h * 0.7f)
                    lineTo(w.toFloat(), 0f)
                    close()
                }
                canvas.drawPath(coastPath, coastPaint)
            }

            MapStyle.TERRAIN -> {
                // Topo warm beige/ochre
                paint.color = Color.rgb(238, 230, 215)
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                // Topo contour elevation rings
                val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(210, 195, 175)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                val cx = w * 0.4f
                val cy = h * 0.6f
                for (r in 1..4) {
                    canvas.drawCircle(cx, cy, r * (w * 0.12f), contourPaint)
                }
            }
        }
    }

    private fun drawRoadsAndFeatures(
        canvas: Canvas,
        w: Int,
        h: Int,
        lat: Double,
        lng: Double,
        mapStyle: MapStyle
    ) {
        val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        when (mapStyle) {
            MapStyle.NORMAL_STREET -> {
                // Secondary streets (white)
                roadPaint.color = Color.WHITE
                roadPaint.strokeWidth = w * 0.045f

                // Street grid
                canvas.drawLine(0f, h * 0.35f, w.toFloat(), h * 0.35f, roadPaint)
                canvas.drawLine(0f, h * 0.7f, w.toFloat(), h * 0.7f, roadPaint)
                canvas.drawLine(w * 0.25f, 0f, w * 0.25f, h.toFloat(), roadPaint)
                canvas.drawLine(w * 0.75f, 0f, w * 0.75f, h.toFloat(), roadPaint)

                // Primary Arterial Highway (Google yellow/orange)
                roadPaint.color = Color.rgb(255, 214, 107)
                roadPaint.strokeWidth = w * 0.07f
                val mainRoad = Path().apply {
                    moveTo(0f, h * 0.85f)
                    cubicTo(w * 0.3f, h * 0.65f, w * 0.7f, h * 0.35f, w.toFloat(), h * 0.2f)
                }
                canvas.drawPath(mainRoad, roadPaint)

                // Highway casing outline
                val casingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    color = Color.rgb(228, 180, 80)
                }
                canvas.drawPath(mainRoad, casingPaint)
            }

            MapStyle.SATELLITE -> {
                roadPaint.color = Color.argb(180, 255, 255, 255)
                roadPaint.strokeWidth = w * 0.035f
                val satRoad = Path().apply {
                    moveTo(0f, h * 0.8f)
                    cubicTo(w * 0.4f, h * 0.6f, w * 0.6f, h * 0.4f, w.toFloat(), h * 0.25f)
                }
                canvas.drawPath(satRoad, roadPaint)

                roadPaint.strokeWidth = w * 0.02f
                canvas.drawLine(w * 0.3f, 0f, w * 0.3f, h.toFloat(), roadPaint)
            }

            MapStyle.TERRAIN -> {
                roadPaint.color = Color.rgb(205, 140, 90)
                roadPaint.strokeWidth = w * 0.04f
                val terrainRoad = Path().apply {
                    moveTo(0f, h * 0.3f)
                    cubicTo(w * 0.35f, h * 0.45f, w * 0.65f, h * 0.2f, w.toFloat(), h * 0.5f)
                }
                canvas.drawPath(terrainRoad, roadPaint)
            }
        }
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
            color = Color.argb(60, 66, 133, 244)
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
            // Left curve to upper circle
            cubicTo(
                cx - pinWidth * 0.55f, cy - pinHeight * 0.4f,
                cx - pinWidth * 0.55f, pinTop + pinWidth * 0.2f,
                cx, pinTop
            )
            // Right curve back to tip
            cubicTo(
                cx + pinWidth * 0.55f, pinTop + pinWidth * 0.2f,
                cx + pinWidth * 0.55f, cy - pinHeight * 0.4f,
                cx, cy
            )
            close()
        }
        canvas.drawPath(pinPath, pinPaint)

        // Darker shadow on right side of pin for 3D realism
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

    private fun drawMapFrame(canvas: Canvas, w: Int, h: Int) {
        // Subtle outer border and tiny Google-style watermark banner
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, w - 1f, h - 1f, borderPaint)

        // Mini Google badge in bottom corner
        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
        }
        val bannerRect = RectF(6f, h - 22f, 48f, h - 6f)
        canvas.drawRoundRect(bannerRect, 4f, 4f, badgeBg)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(66, 133, 244)
            textSize = 10f
            isFakeBoldText = true
        }
        canvas.drawText("Google", 10f, h - 10f, textPaint)
    }
}
