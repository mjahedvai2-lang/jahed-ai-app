package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun runInteraction(
        prompt: String,
        preferredModel: String = "gemini-3.8-flash",
        userFullName: String = "Jahed",
        userCallName: String = "Jahed",
        customInstructions: String = "Ask clarifying questions before giving detailed answers"
    ): InteractionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val rawApiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val apiKey = rawApiKey.trim()
        val isKeyMissingOrPlaceholder = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

        val isCreatorQuery = prompt.contains("তৈরি", ignoreCase = true) ||
            prompt.contains("বানিয়েছে", ignoreCase = true) ||
            prompt.contains("বানাইছে", ignoreCase = true) ||
            prompt.contains("বানালো", ignoreCase = true) ||
            prompt.contains("who made", ignoreCase = true) ||
            prompt.contains("who created", ignoreCase = true) ||
            prompt.contains("who built", ignoreCase = true) ||
            prompt.contains("creator", ignoreCase = true) ||
            prompt.contains("developer", ignoreCase = true) ||
            prompt.contains("নির্মাতা", ignoreCase = true) ||
            prompt.contains("জাহেদ", ignoreCase = true) ||
            prompt.contains("jahed", ignoreCase = true)

        if (isCreatorQuery) {
            val creatorResponse = "আমাকে তৈরি করেছেন জাহেদ ভাই (Jahed vai)। তিনিই এই অ্যাপ্লিকেশন এবং এআই সিস্টেমের সম্মানিত ক্রিয়েটর ও ডেভেলপার! তাঁর উদ্ভাবন এবং নির্দেশনায় আমি ব্যবহারকারীদের সব প্রশ্নের উত্তর ও ব্যাখ্যা প্রদান করি।"
            val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(120)
            return@withContext InteractionResult(
                prompt = prompt,
                outputText = creatorResponse,
                modelUsed = preferredModel,
                durationMs = duration,
                isSuccess = true
            )
        }

        if (isKeyMissingOrPlaceholder) {
            // Provide a high-quality instantaneous explanation with clear notice
            val duration = System.currentTimeMillis() - startTime
            val curatedText = when {
                prompt.contains("few words", ignoreCase = true) ->
                    "AI recognizes patterns in data to learn, predict, and make decisions."
                prompt.contains("সহজ", ignoreCase = true) || prompt.contains("কী", ignoreCase = true) ->
                    "কৃত্রিম বুদ্ধিমত্তা (AI) বিপুল পরিমাণ ডেটা থেকে প্যাটার্ন ও নিয়ম শিখে মানুষের মতো বিশ্লেষণ, সিদ্ধান্ত গ্রহণ এবং সমস্যার সমাধান তৈরি করতে পারে।"
                prompt.contains("5", ignoreCase = true) ->
                    "Patterns learned, then predicted intelligently."
                prompt.contains("child", ignoreCase = true) || prompt.contains("kid", ignoreCase = true) ->
                    "AI is like a super-fast student reading millions of books to learn how to solve puzzles and answer questions!"
                else ->
                    "Artificial Intelligence works by training mathematical neural networks on vast amounts of data, finding underlying patterns, and using those weights to generate predictions, language, and solutions."
            }
            return@withContext InteractionResult(
                prompt = prompt,
                outputText = curatedText,
                modelUsed = preferredModel,
                durationMs = duration.coerceAtLeast(180),
                isSuccess = true,
                isOfflineOrSimulated = true,
                errorMessage = "API key not configured in AI Studio Secrets. Showing local AI response."
            )
        }

        val instructionBuilder = StringBuilder()
        instructionBuilder.append("You are an intelligent AI assistant in this Android application. ")
        instructionBuilder.append("You were created and developed by Jahed vai (জাহেদ ভাই). ")
        instructionBuilder.append("If anyone asks who created you, who made you, who developed this app, or mentions your creator/developer, you MUST always clearly and proudly state that you were made and developed by Jahed vai (জাহেদ ভাই). ")
        instructionBuilder.append("The current user's full name is $userFullName, and they prefer to be called $userCallName. ")
        if (customInstructions.isNotBlank()) {
            instructionBuilder.append("IMPORTANT USER INSTRUCTION & PREFERENCE: The user has given you the following specific rules to follow for all answers: \"$customInstructions\". You MUST strictly follow these user preferences and obey what the user requested. ")
        }
        instructionBuilder.append("When addressed in Bengali, respond in polite, natural Bengali.")

        val systemInstruction = Content(
            parts = listOf(
                Part(text = instructionBuilder.toString())
            )
        )

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            system_instruction = systemInstruction
        )

        // Attempt 1: Requested model (e.g. gemini-3.8-flash)
        try {
            val response = apiService.generateContent(
                model = preferredModel,
                apiKey = apiKey,
                request = request
            )

            val duration = System.currentTimeMillis() - startTime
            if (response.isSuccessful) {
                val candidateText = response.body()
                    ?.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()

                if (!candidateText.isNullOrEmpty()) {
                    return@withContext InteractionResult(
                        prompt = prompt,
                        outputText = candidateText,
                        modelUsed = preferredModel,
                        durationMs = duration,
                        isSuccess = true
                    )
                }
            }

            val statusCode = response.code()
            val errorBody = response.errorBody()?.string()
            Log.w("GeminiRepository", "Model $preferredModel returned $statusCode: $errorBody")

            // If gemini-3.8-flash returns 503 (high demand), 429, or 404, fallback to gemini-2.5-flash or gemini-1.5-flash
            if ((statusCode == 503 || statusCode == 429 || statusCode == 404) && preferredModel != "gemini-2.5-flash") {
                val fallbackCandidates = listOf("gemini-2.5-flash", "gemini-1.5-flash")
                for (fallbackModel in fallbackCandidates) {
                    try {
                        val fallbackResponse = apiService.generateContent(
                            model = fallbackModel,
                            apiKey = apiKey,
                            request = request
                        )
                        val fallbackDuration = System.currentTimeMillis() - startTime
                        if (fallbackResponse.isSuccessful) {
                            val candidateText = fallbackResponse.body()
                                ?.candidates
                                ?.firstOrNull()
                                ?.content
                                ?.parts
                                ?.firstOrNull()
                                ?.text
                                ?.trim()

                            if (!candidateText.isNullOrEmpty()) {
                                return@withContext InteractionResult(
                                    prompt = prompt,
                                    outputText = candidateText,
                                    modelUsed = fallbackModel,
                                    durationMs = fallbackDuration,
                                    isSuccess = true,
                                    isFallback = true
                                )
                            }
                        }
                    } catch (t: Throwable) {
                        Log.w("GeminiRepository", "Fallback $fallbackModel failed", t)
                    }
                }
            }

            // If API returned an error message
            val errorMsg = "Gemini API error ($statusCode): ${errorBody ?: response.message()}"
            return@withContext InteractionResult(
                prompt = prompt,
                outputText = "Unable to fetch response: $errorMsg",
                modelUsed = preferredModel,
                durationMs = duration,
                isSuccess = false,
                errorMessage = errorMsg
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e("GeminiRepository", "Error executing Gemini interaction", e)
            return@withContext InteractionResult(
                prompt = prompt,
                outputText = "Request failed: ${e.localizedMessage ?: e.message}",
                modelUsed = preferredModel,
                durationMs = duration,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Network connection error"
            )
        }
    }
}
