package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.map.CenteredLocationOverlay
import com.example.map.CenteredMapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.io.File

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()

    // Request permissions quietly via system prompt on first launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.startTracking()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasLocationPermission()) {
            viewModel.startTracking()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Configure osmdroid cache in private app storage
    remember {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        config.osmdroidBasePath = File(context.cacheDir, "osmdroid")
        config.osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        true
    }

    // Retain overlay reference
    val locationOverlay = remember { CenteredLocationOverlay() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .testTag("map_screen_root")
    ) {
        // Full screen interactive MapView (North locked, center locked, interactive zoom)
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_gps_map_view"),
            factory = { ctx ->
                CenteredMapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    overlays.add(locationOverlay)

                    val defaultGeo = GeoPoint(52.2297, 21.0122)
                    currentGeoPoint = defaultGeo
                    controller.setCenter(defaultGeo)
                }
            },
            update = { mapView ->
                locationState?.let { loc ->
                    val geo = GeoPoint(loc.latitude, loc.longitude)
                    mapView.currentGeoPoint = geo
                    locationOverlay.currentLocation = loc
                    mapView.controller.setCenter(geo)
                }
            }
        )
    }

    // Handle Android lifecycle for osmdroid MapView and location tracking
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (viewModel.hasLocationPermission()) {
                        viewModel.startTracking()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopTracking()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopTracking()
        }
    }
}
