package com.example.map

import android.content.Context
import android.graphics.Color
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import kotlin.math.ln

/**
 * Custom MapView configured to:
 * 1. Keep the user's GPS position permanently centered on screen.
 * 2. Keep the map permanently oriented to North (0°).
 * 3. Provide smooth interactive pinch-to-zoom (scaling, shrinking, stretching) anchored at the center.
 * 4. Hide all default buttons, zoom controls, and menus.
 */
class CenteredMapView(context: Context) : MapView(context) {

    var currentGeoPoint: GeoPoint? = null
        set(value) {
            field = value
            if (value != null) {
                controller.setCenter(value)
                postInvalidateOnAnimation()
            }
        }

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                if (factor > 0f) {
                    val deltaZoom = (ln(factor.toDouble()) / ln(2.0))
                    val targetZoom = (zoomLevelDouble + deltaZoom).coerceIn(minZoomLevel, maxZoomLevel)
                    controller.setZoom(targetZoom)
                    currentGeoPoint?.let { controller.setCenter(it) }
                }
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val targetZoom = (zoomLevelDouble + 1.0).coerceAtMost(maxZoomLevel)
                controller.setZoom(targetZoom)
                currentGeoPoint?.let { controller.setCenter(it) }
                return true
            }
        }
    )

    init {
        // Use dark placeholder background while tiles load
        setBackgroundColor(Color.parseColor("#0D1117"))

        // Disable default multi-touch controls so we can guarantee center-locked zoom
        setMultiTouchControls(false)

        // Completely hide built-in on-screen zoom buttons
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        // Scale tiles according to device screen density for razor sharp street names
        isTilesScaledToDpi = true

        // Strict North orientation
        super.setMapOrientation(0f, false)

        // Set default zoom level to a comfortable city street level
        controller.setZoom(16.5)
    }

    override fun setMapOrientation(degrees: Float, force: Boolean) {
        // Enforce North orientation at all times
        super.setMapOrientation(0f, false)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Route gestures to double-tap and pinch-to-zoom detectors
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)

        // Always clamp map center to current position and maintain 0° North orientation
        currentGeoPoint?.let { controller.setCenter(it) }
        super.setMapOrientation(0f, false)

        return true
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        currentGeoPoint?.let { controller.setCenter(it) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        currentGeoPoint?.let { controller.setCenter(it) }
    }

    override fun computeScroll() {
        super.computeScroll()
        // Ensure no inertial scroll drifts away from center
        currentGeoPoint?.let { controller.setCenter(it) }
    }
}
