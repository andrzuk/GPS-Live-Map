# GPS Live Map

GPS Live Map is an Android application that keeps the user location pinned to the center of a dark-themed live map.

The app is implemented with Jetpack Compose UI plus an osmdroid map surface, and uses a ViewModel + location tracker flow for continuous GPS updates.

## Quick Start (60 seconds)

1. Build a debug APK:

```powershell
.\gradlew.bat assembleDebug
```

2. Install on device/emulator:

```powershell
.\gradlew.bat installDebug
```

3. Launch the app and grant location permission when prompted.

### Common First-Run Issues

1. No live location on map.
  - Confirm Android location permission is granted for the app.
  - If denied before, enable it in system app settings and relaunch.

2. Warning about `google-services.json`.
  - This project allows missing `google-services.json` by default.
  - Add `app/google-services.json` only if you enable Firebase features that require it.

## Features

- Live GPS tracking with fused provider first and native LocationManager fallback.
- Center-locked map behavior: position remains centered, map orientation forced to north.
- Smooth gesture zoom with custom map view behavior.
- OpenStreetMap map tiles (MAPNIK via osmdroid).

## Technology Stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture: ViewModel + Location Tracker + Flow
- Map engine: osmdroid
- Location: Google Play Services Fused Location Provider + Android LocationManager fallback
- Build system: Gradle Kotlin DSL, AGP 9.1.1, Kotlin 2.2.10

## Key Modules and Flow

### App entry and UI

- Main activity sets the Compose content and hosts the map screen.
- Map screen:
  - requests location permissions,
  - hosts CenteredMapView via AndroidView,
  - coordinates lifecycle start/stop tracking behavior.

### Location pipeline

1. LocationTracker starts fused updates.
2. It attempts immediate last known position for fast first center.
3. If fused updates fail, it falls back to native providers.
4. Location updates are exposed as StateFlow.

## Project Structure

- app/src/main/java/com/example/MainActivity.kt
- app/src/main/java/com/example/ui/MapScreen.kt
- app/src/main/java/com/example/ui/MapViewModel.kt
- app/src/main/java/com/example/location/LocationTracker.kt
- app/src/main/java/com/example/map/CenteredMapView.kt
- app/src/main/java/com/example/map/CenteredLocationOverlay.kt

## Permissions

Declared in AndroidManifest:

- android.permission.INTERNET
- android.permission.ACCESS_NETWORK_STATE
- android.permission.ACCESS_FINE_LOCATION
- android.permission.ACCESS_COARSE_LOCATION

```dotenv
adb shell pm grant --user 0 com.aistudio.gpslivemap.vntxq android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant --user 0 com.aistudio.gpslivemap.vntxq android.permission.ACCESS_FINE_LOCATION
adb shell dumpsys package com.aistudio.gpslivemap.vntxq | findstr /I "ACCESS_COARSE_LOCATION ACCESS_FINE_LOCATION granted=true"
```

## Google Services behavior

- Google Services plugin is enabled.
- Missing google-services.json is configured to passthrough via gradle.properties:
  - googleServices.missing.passthrough=true
- Build script also sets missing strategy to WARN.

If you add Firebase services that require google-services.json, place it at:

- app/google-services.json

## Build and Run

## Prerequisites

- Android Studio (latest stable recommended)
- Android SDK installed
- JDK 17+ recommended for modern AGP toolchains

## Build debug APK (Windows)

From repository root:

```powershell
.\gradlew.bat assembleDebug
```

Output APK:

- app/build/outputs/apk/debug/app-debug.apk

## Build debug APK (macOS/Linux)

```bash
chmod +x ./gradlew
./gradlew assembleDebug
```

## Install to connected device/emulator

```powershell
.\gradlew.bat installDebug
```

## Clean build

```powershell
.\gradlew.bat clean
```

## Testing

### Unit tests

```powershell
.\gradlew.bat testDebugUnitTest
```

### Instrumented tests (device/emulator required)

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Signing

### Debug

- If debug.keystore exists in repo root, custom debugConfig is used.
- If it does not exist, build falls back to default Android debug signing.

### Release

Release signing expects environment variables:

- KEYSTORE_PATH (optional, defaults to root/my-upload-key.jks)
- STORE_PASSWORD
- KEY_PASSWORD

## Troubleshooting

### Build fails at validateSigningDebug

Cause: custom debug keystore missing.

Resolution:

- Use current build script behavior (automatic fallback already implemented), or
- provide debug.keystore in repository root if you want explicit custom debug signing.

### Google services warning

If your feature set needs Firebase config file, add app/google-services.json.

## Notes on Map Behavior

- The map center is intentionally locked to the latest location.
- North orientation is intentionally forced (rotation disabled).
- Pinch and double-tap zoom are supported while preserving center lock.

## License and attributions

- Map tiles: OpenStreetMap MAPNIK
- Base map data: OpenStreetMap contributors
