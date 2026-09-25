package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.location.LocationTracker

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val locationTracker = LocationTracker(application)

    val locationState = locationTracker.locationFlow

    fun hasLocationPermission(): Boolean {
        return locationTracker.hasLocationPermission()
    }

    fun startTracking() {
        locationTracker.startTracking()
    }

    fun stopTracking() {
        locationTracker.stopTracking()
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopTracking()
    }
}
