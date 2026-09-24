package com.example.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.map.CenteredLocationOverlay
import com.example.map.CenteredMapView
import com.example.map.DarkMatterTileSource
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
    val groundedInfo by viewModel.groundedInfo.collectAsStateWithLifecycle()
    var isPillExpanded by remember { mutableStateOf(false) }

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
                val cartoApiKeyRaw = try {
                    BuildConfig.CARTO_API_KEY
                } catch (_: Throwable) {
                    ""
                }
                val cartoApiKey = normalizeConfigValue(cartoApiKeyRaw)
                val useCartoDarkTiles = isValidCartoApiKey(cartoApiKey)

                CenteredMapView(ctx).apply {
                    if (useCartoDarkTiles) {
                        setTileSource(DarkMatterTileSource(cartoApiKey))
                    } else {
                        setTileSource(TileSourceFactory.MAPNIK)
                    }
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

        // Google Maps Grounding HUD Pill at top safe area
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            val currentStreet = groundedInfo?.streetName ?: "Google Maps Grounding..."
            val isLoading = groundedInfo?.isLoading == true

            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xE610141D))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(20.dp))
                    .clickable { isPillExpanded = !isPillExpanded }
                    .animateContentSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("google_maps_grounding_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0x2600E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = Color(0xFF00E5FF)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Google Maps Location",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentStreet,
                            color = Color(0xFFF0F6FC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x1F2979FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Maps",
                            color = Color(0xFF80D8FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (isPillExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Szczegóły Google Maps",
                        tint = Color(0xFF8B949E),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expanded Google Maps Grounding Info
                AnimatedVisibility(
                    visible = isPillExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp)
                            .testTag("google_maps_details_card")
                    ) {
                        groundedInfo?.details?.let { details ->
                            if (details.isNotBlank()) {
                                Text(
                                    text = details,
                                    color = Color(0xFFC9D1D9),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        locationState?.let { loc ->
                            Text(
                                text = "Współrzędne GPS: ${String.format("%.5f", loc.latitude)}°, ${String.format("%.5f", loc.longitude)}° (±${loc.accuracy.toInt()}m)",
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1A00E5FF))
                                .clickable {
                                    val uriString = groundedInfo?.googleMapsUrl
                                        ?: locationState?.let {
                                            "https://www.google.com/maps/search/?api=1&query=${it.latitude},${it.longitude}"
                                        }
                                    if (uriString != null) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                                        context.startActivity(intent)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("open_in_google_maps_button")
                        ) {
                            Text(
                                text = "Otwórz w Google Maps",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Otwórz w Google Maps",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
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

private fun normalizeConfigValue(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.length >= 2) {
        val isDoubleQuoted = trimmed.first() == '"' && trimmed.last() == '"'
        val isSingleQuoted = trimmed.first() == '\'' && trimmed.last() == '\''
        if (isDoubleQuoted || isSingleQuoted) {
            return trimmed.substring(1, trimmed.length - 1).trim()
        }
    }
    return trimmed
}

private fun isValidCartoApiKey(value: String): Boolean {
    if (value.isBlank()) return false
    val placeholders = listOf(
        "MY_CARTO_API_KEY",
        "YOUR_CARTO_API_KEY",
        "CARTO_API_KEY",
        "PLACEHOLDER",
        "CHANGEME",
        "REPLACE_ME",
        "TODO"
    )
    return placeholders.none { value.equals(it, ignoreCase = true) }
}
