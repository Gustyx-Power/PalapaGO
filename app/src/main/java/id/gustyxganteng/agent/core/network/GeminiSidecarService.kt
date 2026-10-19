package id.gustyxganteng.agent.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

interface GeminiSidecarService {
    suspend fun analyzeImageWithPrompt(base64Image: String, mimeType: String = "image/jpeg", prompt: String): String
    suspend fun synthesizeResearchGap(literatureSummary: String): String
}

class GeminiSidecarServiceImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
) : GeminiSidecarService {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"

    override suspend fun analyzeImageWithPrompt(
        base64Image: String,
        mimeType: String,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = KeyVault.getGeminiApiKey()
        if (apiKey.isBlank()) return@withContext ""

        try {
            val imageUrl = "data:$mimeType;base64,$base64Image"
            val payload = buildJsonObject {
                put("model", "gemini-3.8-flash")
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", prompt.ifBlank { "Describe and analyze this image or UI screenshot in detail for coding integration." })
                            })
                            add(buildJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject {
                                    put("url", imageUrl)
                                })
                            })
                        })
                    })
                })
                put("max_tokens", 1024)
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: return@use ""
                    val parsed = json.parseToJsonElement(bodyStr).jsonObject
                    return@withContext parsed["choices"]?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("message")?.jsonObject
                        ?.get("content")?.jsonPrimitive?.content ?: ""
                }
            }
        } catch (e: Exception) {
            Log.w("GeminiSidecar", "Gemini vision analysis failed: ${e.message}")
        }
        return@withContext ""
    }

    override suspend fun synthesizeResearchGap(literatureSummary: String): String = withContext(Dispatchers.IO) {
        val apiKey = KeyVault.getGeminiApiKey()
        if (apiKey.isBlank()) return@withContext ""

        try {
            val payload = buildJsonObject {
                put("model", "gemini-3.8-flash")
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", "You are an elite academic research methodologist. Identify novel research gaps from literature summaries.")
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", "Synthesize the potential research gaps and novel contributions from this literature:\n\n$literatureSummary")
                    })
                })
                put("max_tokens", 768)
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: return@use ""
                    val parsed = json.parseToJsonElement(bodyStr).jsonObject
                    return@withContext parsed["choices"]?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("message")?.jsonObject
                        ?.get("content")?.jsonPrimitive?.content ?: ""
                }
            }
        } catch (e: Exception) {
            Log.w("GeminiSidecar", "Gemini research gap synthesis failed: ${e.message}")
        }
        return@withContext ""
    }
}
