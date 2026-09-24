package com.example.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import android.os.SystemClock
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance hardware-accelerated overlay that draws the user's GPS position
 * at the screen center with a pulsing radar wave and accuracy indicator.
 */
class CenteredLocationOverlay : Overlay() {

    var currentLocation: Location? = null

    private val accuracyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(25, 0, 229, 255)
    }

    private val accuracyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(90, 0, 229, 255)
    }

    private val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#00E5FF")
        setShadowLayer(14f, 0f, 0f, Color.argb(180, 0, 229, 255))
    }

    private val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val bearingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#8000E5FF")
    }

    private val bearingPath = Path()

    override fun draw(canvas: Canvas, osmv: MapView, shadow: Boolean) {
        if (shadow) return
        val loc = currentLocation ?: return

        val cx = osmv.width / 2f
        val cy = osmv.height / 2f

        val density = osmv.resources.displayMetrics.density

        // 1. Accuracy Circle
        if (loc.hasAccuracy() && loc.accuracy > 0) {
            val meters = loc.accuracy
            val accuracyRadius = osmv.projection.metersToEquatorPixels(meters)
                .coerceIn(16f * density, osmv.width * 0.45f)

            canvas.drawCircle(cx, cy, accuracyRadius, accuracyFillPaint)
            canvas.drawCircle(cx, cy, accuracyRadius, accuracyStrokePaint)
        }

        // 2. Animated Radar Pulse
        val animDuration = 2200L
        val progress = (SystemClock.uptimeMillis() % animDuration).toFloat() / animDuration
        val pulseMaxRadius = 42f * density
        val currentPulseRadius = (10f * density) + (pulseMaxRadius - 10f * density) * progress
        val pulseAlpha = ((1f - progress) * 160).toInt().coerceIn(0, 255)

        radarPaint.color = Color.argb(pulseAlpha, 0, 229, 255)
        canvas.drawCircle(cx, cy, currentPulseRadius, radarPaint)

        // 3. Direction pointer if moving with valid bearing
        if (loc.hasBearing() && loc.speed > 0.4f) {
            val bearingRad = Math.toRadians((loc.bearing - 90.0)).toFloat()
            val arrowLength = 26f * density
            val arrowSpread = 0.35f

            val tipX = cx + arrowLength * cos(bearingRad.toDouble()).toFloat()
            val tipY = cy + arrowLength * sin(bearingRad.toDouble()).toFloat()

            val leftRad = bearingRad + Math.PI.toFloat() - arrowSpread
            val rightRad = bearingRad + Math.PI.toFloat() + arrowSpread

            val sideLength = 16f * density
            val leftX = cx + sideLength * cos(leftRad.toDouble()).toFloat()
            val leftY = cy + sideLength * sin(leftRad.toDouble()).toFloat()
            val rightX = cx + sideLength * cos(rightRad.toDouble()).toFloat()
            val rightY = cy + sideLength * sin(rightRad.toDouble()).toFloat()

            bearingPath.reset()
            bearingPath.moveTo(tipX, tipY)
            bearingPath.lineTo(leftX, leftY)
            bearingPath.lineTo(cx, cy)
            bearingPath.lineTo(rightX, rightY)
            bearingPath.close()

            canvas.drawPath(bearingPath, bearingPaint)
        }

        // 4. Center beacon: outer ring and inner white dot
        val outerRadius = 8.5f * density
        val innerRadius = 4f * density

        canvas.drawCircle(cx, cy, outerRadius, outerRingPaint)
        canvas.drawCircle(cx, cy, innerRadius, innerDotPaint)

        // Request next animation frame for smooth radar pulse
        osmv.postInvalidateOnAnimation()
    }
}
