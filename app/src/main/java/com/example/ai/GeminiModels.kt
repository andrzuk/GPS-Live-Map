package com.example.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val tools: List<JsonObject>? = null,
    val toolConfig: ToolConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class ToolConfig(
    val retrievalConfig: RetrievalConfig? = null
)

@Serializable
data class RetrievalConfig(
    val latLng: LatLngLiteral? = null
)

@Serializable
data class LatLngLiteral(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val groundingMetadata: GroundingMetadata? = null
)

@Serializable
data class GroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val searchEntryPoint: SearchEntryPoint? = null,
    val groundings: List<GroundingChunk>? = null
)

@Serializable
data class SearchEntryPoint(
    val renderedContent: String? = null
)

@Serializable
data class GroundingChunk(
    val maps: MapsChunk? = null
)

@Serializable
data class MapsChunk(
    val uri: String? = null,
    val title: String? = null
)

data class GroundedLocationInfo(
    val streetName: String,
    val details: String,
    val googleMapsUrl: String? = null,
    val isLoading: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
