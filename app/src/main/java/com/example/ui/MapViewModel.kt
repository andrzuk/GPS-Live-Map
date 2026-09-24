package com.example.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GoogleMapsGroundingRepository
import com.example.ai.GroundedLocationInfo
import com.example.location.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val locationTracker = LocationTracker(application)
    private val mapsGroundingRepository = GoogleMapsGroundingRepository()

    val locationState: StateFlow<Location?> = locationTracker.locationFlow

    private val _groundedInfo = MutableStateFlow<GroundedLocationInfo?>(null)
    val groundedInfo: StateFlow<GroundedLocationInfo?> = _groundedInfo.asStateFlow()

    private var lastGroundedLocation: Location? = null

    init {
        viewModelScope.launch {
            locationTracker.locationFlow.collect { loc ->
                if (loc != null) {
                    val prev = lastGroundedLocation
                    if (prev == null || prev.distanceTo(loc) > 30f) {
                        lastGroundedLocation = loc
                        fetchGoogleMapsGrounding(loc.latitude, loc.longitude)
                    }
                }
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        return locationTracker.hasLocationPermission()
    }

    fun startTracking() {
        locationTracker.startTracking()
    }

    fun stopTracking() {
        locationTracker.stopTracking()
    }

    fun refreshGroundedInfo() {
        val loc = locationState.value ?: return
        fetchGoogleMapsGrounding(loc.latitude, loc.longitude)
    }

    private fun fetchGoogleMapsGrounding(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _groundedInfo.value = _groundedInfo.value?.copy(isLoading = true)
                ?: GroundedLocationInfo(
                    streetName = "Pobieranie z Google Maps...",
                    details = "Sprawdzanie danych lokalizacyjnych",
                    isLoading = true
                )

            val info = mapsGroundingRepository.getGroundedStreetInfo(latitude, longitude)
            _groundedInfo.value = info
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopTracking()
    }
}
