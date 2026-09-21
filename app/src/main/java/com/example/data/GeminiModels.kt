package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val system_instruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiApiError? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: ContentResponse? = null
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    val parts: List<PartResponse>? = null
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiApiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

data class InteractionResult(
    val prompt: String,
    val outputText: String,
    val modelUsed: String,
    val durationMs: Long,
    val isSuccess: Boolean,
    val isFallback: Boolean = false,
    val isOfflineOrSimulated: Boolean = false,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
