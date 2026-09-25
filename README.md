# GPS Live Map

GPS Live Map is an Android application that keeps the user location pinned to the center of a dark-themed live map, while enriching the current position with Gemini-powered Google Maps grounding data (street/area details and deep link to Google Maps).

The app is implemented with Jetpack Compose UI plus an osmdroid map surface, and uses a ViewModel + repository flow for location and AI grounding updates.

## Quick Start (60 seconds)

1. Create or edit `.env` in repo root:

```dotenv
GEMINI_API_KEY=YOUR_REAL_GEMINI_API_KEY
```

2. Build a debug APK:

```powershell
.\gradlew.bat assembleDebug
```

3. Install on device/emulator:

```powershell
.\gradlew.bat installDebug
```

4. Launch the app and grant location permission when prompted.

### Common First-Run Issues

1. No live location on map.
  - Confirm Android location permission is granted for the app.
  - If denied before, enable it in system app settings and relaunch.

2. Grounding pill shows fallback text only.
  - Confirm `.env` exists in repo root.
  - Confirm `GEMINI_API_KEY` in `.env` is a real key (not `MY_GEMINI_API_KEY`).
  - Rebuild after editing `.env`:

```powershell
.\gradlew.bat assembleDebug
```

3. Warning about `google-services.json`.
  - This project allows missing `google-services.json` by default.
  - Add `app/google-services.json` only if you enable Firebase features that require it.

## Features

- Live GPS tracking with fused provider first and native LocationManager fallback.
- Center-locked map behavior: position remains centered, map orientation forced to north.
- Smooth gesture zoom with custom map view behavior.
- OpenStreetMap map tiles (MAPNIK via osmdroid).
- Top HUD pill showing grounded street/location information.
- Open current grounded location directly in Google Maps.
- Graceful fallback when Gemini API key is missing or placeholder.

## Technology Stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture: ViewModel + Repository + Flow
- Map engine: osmdroid
- Location: Google Play Services Fused Location Provider + Android LocationManager fallback
- Networking: Retrofit + OkHttp + kotlinx.serialization
- AI/Grounding: Gemini GenerateContent API with Google Maps tool config
- Build system: Gradle Kotlin DSL, AGP 9.1.1, Kotlin 2.2.10

## Key Modules and Flow

### App entry and UI

- Main activity sets the Compose content and hosts the map screen.
- Map screen:
  - requests location permissions,
  - hosts CenteredMapView via AndroidView,
  - renders a grounding info pill (collapsed/expanded),
  - coordinates lifecycle start/stop tracking behavior.

### Location pipeline

1. LocationTracker starts fused updates.
2. It attempts immediate last known position for fast first center.
3. If fused updates fail, it falls back to native providers.
4. Location updates are exposed as StateFlow.

### Grounding pipeline

1. MapViewModel observes location flow.
2. Grounding requests trigger on first location and when distance changes by over 30 meters.
3. GoogleMapsGroundingRepository reads BuildConfig.GEMINI_API_KEY.
4. If key is missing/placeholder, repository returns safe local fallback text.
5. If key exists, repository calls Gemini endpoint and maps response into GroundedLocationInfo.

## Project Structure

- app/src/main/java/com/example/MainActivity.kt
- app/src/main/java/com/example/ui/MapScreen.kt
- app/src/main/java/com/example/ui/MapViewModel.kt
- app/src/main/java/com/example/location/LocationTracker.kt
- app/src/main/java/com/example/map/CenteredMapView.kt
- app/src/main/java/com/example/map/CenteredLocationOverlay.kt
- app/src/main/java/com/example/ai/GeminiApiService.kt
- app/src/main/java/com/example/ai/GeminiModels.kt
- app/src/main/java/com/example/ai/GoogleMapsGroundingRepository.kt

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

## Secrets and API Key Management

This project uses the Secrets Gradle Plugin with these files:

- .env (local, not committed)
- .env.example (committed template)

Configuration is in app/build.gradle.kts:

- propertiesFileName = ".env"
- defaultPropertiesFileName = ".env.example"

### Set your Gemini key locally

1. Edit .env in repository root.
2. Set:

```dotenv
GEMINI_API_KEY=YOUR_REAL_GEMINI_API_KEY
```

3. Keep .env.example as placeholder only.

Current ignore rules already protect local secrets:

- .env
- .env.*
- !.env.example

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

### Grounding pill stays in fallback mode

Check:

- .env exists in repository root
- GEMINI_API_KEY is set and not placeholder
- app was rebuilt after changing .env

### Google services warning

If your feature set needs Firebase config file, add app/google-services.json.

## Notes on Map Behavior

- The map center is intentionally locked to the latest location.
- North orientation is intentionally forced (rotation disabled).
- Pinch and double-tap zoom are supported while preserving center lock.

## License and attributions

- Map tiles: OpenStreetMap MAPNIK
- Base map data: OpenStreetMap contributors
