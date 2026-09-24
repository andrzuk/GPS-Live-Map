package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

class GoogleMapsGroundingRepository {

    private val apiService = GeminiClient.service

    suspend fun getGroundedStreetInfo(
        latitude: Double,
        longitude: Double
    ): GroundedLocationInfo = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        val defaultMapsUrl = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GroundedLocationInfo(
                streetName = String.format("%.4f°N, %.4f°E", latitude, longitude),
                details = "Lokalizacja GPS w centrum mapy (Google Maps Grounding oczekuje na klucz API w Secrets).",
                googleMapsUrl = defaultMapsUrl,
                isLoading = false
            )
        }

        try {
            val prompt = "Identify the exact street name and area at GPS coordinates $latitude, $longitude using Google Maps. Format your response as: First line: the exact street name. Second line: district and city."

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                ),
                tools = listOf(
                    buildJsonObject {
                        putJsonObject("googleMaps") {}
                    }
                ),
                toolConfig = ToolConfig(
                    retrievalConfig = RetrievalConfig(
                        latLng = LatLngLiteral(latitude = latitude, longitude = longitude)
                    )
                ),
                systemInstruction = Content(
                    parts = listOf(
                        Part(
                            text = "You are a precise geographic locator grounded strictly in Google Maps data. Always identify the street name corresponding to the GPS coordinates."
                        )
                    )
                )
            )

            val response = apiService.generateContent(apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val text = candidate?.content?.parts?.firstOrNull()?.text?.trim() ?: ""

            val lines = text.lines().map { it.trim().removePrefix("**").removeSuffix("**") }.filter { it.isNotBlank() }
            val street = lines.firstOrNull() ?: String.format("%.4f°N, %.4f°E", latitude, longitude)
            val details = lines.drop(1).joinToString(" ").ifBlank {
                "Zweryfikowane dane lokalizacyjne z Google Maps"
            }

            val mapsChunkUri = candidate?.groundingMetadata?.groundings
                ?.mapNotNull { it.maps?.uri }
                ?.firstOrNull() ?: defaultMapsUrl

            GroundedLocationInfo(
                streetName = street,
                details = details,
                googleMapsUrl = mapsChunkUri,
                isLoading = false
            )
        } catch (e: Exception) {
            Log.e("GoogleMapsGrounding", "Error fetching grounded info", e)
            GroundedLocationInfo(
                streetName = String.format("%.4f°N, %.4f°E", latitude, longitude),
                details = "Pozycja GPS: $latitude, $longitude (Google Maps)",
                googleMapsUrl = defaultMapsUrl,
                isLoading = false
            )
        }
    }
}
