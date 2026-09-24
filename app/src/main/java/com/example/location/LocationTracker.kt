package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages battery-conscious real-time GPS tracking updated every few seconds.
 * Leverages Google Play Services Fused Location Provider with native LocationManager fallback.
 */
class LocationTracker(private val context: Context) {

    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val locationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    private var isTracking = false

    private val fusedLocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { newLoc ->
                _locationFlow.value = newLoc
            }
        }
    }

    private val nativeLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _locationFlow.value = location
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking || !hasLocationPermission()) return
        isTracking = true

        // 1. Immediately check last known location to center without waiting for first interval
        try {
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && _locationFlow.value == null) {
                    _locationFlow.value = lastLoc
                }
            }
        } catch (_: SecurityException) {}

        // Fallback last known from LocationManager
        if (_locationFlow.value == null) {
            try {
                val gpsLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val netLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val bestLast = gpsLoc ?: netLoc
                if (bestLast != null) {
                    _locationFlow.value = bestLast
                }
            } catch (_: SecurityException) {}
        }

        // 2. Request updates every few seconds (~4 seconds interval)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
            .setMinUpdateIntervalMillis(3000L)
            .setMaxUpdateDelayMillis(5000L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        try {
            fusedClient.requestLocationUpdates(
                locationRequest,
                fusedLocationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener {
                startNativeLocationFallback()
            }
        } catch (e: Exception) {
            startNativeLocationFallback()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startNativeLocationFallback() {
        try {
            val lm = locationManager ?: return
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    4000L,
                    1f,
                    nativeLocationListener,
                    Looper.getMainLooper()
                )
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    4000L,
                    1f,
                    nativeLocationListener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: SecurityException) {}
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        try {
            fusedClient.removeLocationUpdates(fusedLocationCallback)
        } catch (_: Exception) {}
        try {
            locationManager?.removeUpdates(nativeLocationListener)
        } catch (_: Exception) {}
    }
}
