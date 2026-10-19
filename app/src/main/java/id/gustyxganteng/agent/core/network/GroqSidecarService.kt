package id.gustyxganteng.agent.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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

interface GroqSidecarService {
    suspend fun summarizeLargeOutput(rawOutput: String, sourceHint: String = ""): String
    suspend fun triageStacktrace(rawError: String): String
    suspend fun generateBranchSummary(sessionTitle: String, conversationSnippet: String): String
    suspend fun auditCodeQuality(codeSnippet: String, contextHint: String = ""): String
}

class GroqSidecarServiceImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()
) : GroqSidecarService {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    override suspend fun summarizeLargeOutput(rawOutput: String, sourceHint: String): String = withContext(Dispatchers.IO) {
        if (rawOutput.length <= 1500) return@withContext rawOutput

        val prompt = buildString {
            appendLine("You are a high-velocity, precision pre-flight log and data compactor for PalapaGO AI Coding Agent.")
            if (sourceHint.isNotBlank()) appendLine("Context / Source: $sourceHint")
            appendLine("Task: Summarize the following raw tool output into maximum 15 concise, high-signal bullet points.")
            appendLine("Strict constraints: Retain exact error codes, file paths, line numbers, and critical values. Omit redundant verbose progress logs or repetitive whitespace.")
            appendLine("\n[RAW OUTPUT TO SUMMARIZE]:\n")
            // Batasi input maksimal 20.000 karakter agar tidak menghabiskan TPM
            append(rawOutput.take(20000))
        }

        val result = executeGroqChat(prompt, maxTokens = 512)
        if (result.isNotBlank()) {
            "> *[Intisari Output Cepat oleh Groq Sidecar (Llama-3.1-8b)]:*\n$result"
        } else {
            // Fallback truncation jika Groq offline/unconfigured
            fallbackTruncation(rawOutput)
        }
    }

    override suspend fun triageStacktrace(rawError: String): String = withContext(Dispatchers.IO) {
        if (rawError.length <= 800) return@withContext rawError

        val prompt = buildString {
            appendLine("Analyze this build failure or error stacktrace.")
            appendLine("Extract only:")
            appendLine("1. The primary root cause ('Caused by:' or fatal assertion).")
            appendLine("2. Exact file path and line number where the failure originated.")
            appendLine("3. A 1-sentence recommended fix.")
            appendLine("\n[RAW ERROR]:\n")
            append(rawError.take(15000))
        }

        val result = executeGroqChat(prompt, maxTokens = 256)
        if (result.isNotBlank()) {
            "> *[Analisis Stacktrace Kilat oleh Groq Sidecar]:*\n$result\n\n---\n" + rawError.take(1000)
        } else {
            rawError
        }
    }

    override suspend fun generateBranchSummary(sessionTitle: String, conversationSnippet: String): String = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("You are a technical context synthesizer for PalapaGO AI Coding Agent.")
            appendLine("The user is branching this conversation to a fresh context window.")
            appendLine("Source Session: $sessionTitle")
            appendLine("Task: Synthesize the current conversation history into a structured briefing note.")
            appendLine("Strict constraints:")
            appendLine("- Output in Indonesian (or hybrid bilingual if technical terms).")
            appendLine("- Must be concise, actionable, and dense (max 300 words).")
            appendLine("- Structure EXACTLY as follows:")
            appendLine("### 🌿 Konteks Cabang Percakapan")
            appendLine("- **🎯 Tujuan Utama**: (1-2 kalimat inti masalah/kebutuhan user)")
            appendLine("- **⚡ Progres & Perubahan**: (poin-poin apa yang sudah dibuat/diedit, file path spesifik jika ada)")
            appendLine("- **📐 Keputusan Arsitektur**: (keputusan kunci atau aturan yang harus dipatuhi)")
            appendLine("- **📋 Langkah Selanjutnya**: (tugas berikutnya yang belum selesai atau tertunda)")
            appendLine("\n[CONVERSATION SNIPPET]:\n")
            append(conversationSnippet.take(12000))
        }

        val result = executeGroqChat(prompt, maxTokens = 600)
        if (result.isNotBlank()) {
            result
        } else {
            fallbackBranchSummary(sessionTitle, conversationSnippet)
        }
    }

    override suspend fun auditCodeQuality(codeSnippet: String, contextHint: String): String = withContext(Dispatchers.IO) {
        if (codeSnippet.length <= 40) return@withContext ""

        val prompt = buildString {
            appendLine("You are the Fast Quality Auditor in a multi-model collaborative coding ensemble.")
            if (contextHint.isNotBlank()) appendLine("Context: $contextHint")
            appendLine("Task: Audit the following code snippet. Check strictly for:")
            appendLine("1. Any lazy placeholders, TODO comments, or incomplete functions ('// TODO', '// rest of code', 'pass', etc.)")
            appendLine("2. Critical syntax flaws (unclosed brackets, unescaped tags).")
            appendLine("If flawless and production-ready, output: 'VERIFIED: Clean & Production-Ready.'")
            appendLine("If defects found, output: 'AUDIT WARNING: [brief 1-sentence issue description]'.")
            appendLine("\n[CODE TO AUDIT]:\n")
            append(codeSnippet.take(15000))
        }

        val result = executeGroqChat(prompt, maxTokens = 120)
        result.ifBlank { "" }
    }

    private fun fallbackBranchSummary(sessionTitle: String, conversationSnippet: String): String {
        return buildString {
            appendLine("### 🌿 Konteks Cabang Percakapan")
            appendLine("*Dicabangkan dari sesi: **$sessionTitle***\n")
            appendLine("- **🎯 Sesi Induk**: $sessionTitle")
            appendLine("- **⚡ Status**: Percakapan dicabangkan ke context window segar (0 token terpakai). Riwayat pengerjaan tetap tersimpan di sesi induk.")
            appendLine("- **📋 Riwayat Terakhir**:")
            conversationSnippet.lines().filter { it.isNotBlank() }.takeLast(8).forEach {
                appendLine("  > ${it.take(120)}")
            }
        }
    }

    private fun executeGroqChat(userMessage: String, maxTokens: Int): String {
        val keyPool = KeyVault.getGroqKeyPool()
        if (keyPool.isEmpty()) return ""

        val candidateModels = listOf("openai/gpt-oss-20b", "qwen/qwen3.8-27b", "openai/gpt-oss-120b", "llama-3.1-8b-instant")

        for (apiKey in keyPool) {
            for (model in candidateModels) {
                try {
                    val payload = buildJsonObject {
                        put("model", model)
                        put("messages", buildJsonArray {
                            add(buildJsonObject {
                                put("role", "user")
                                put("content", userMessage)
                            })
                        })
                        put("max_tokens", maxTokens)
                        put("temperature", 0.2)
                    }

                    val requestBody = payload.toString().toRequestBody(jsonMediaType)
                    val request = Request.Builder()
                        .url(endpoint)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("User-Agent", "okhttp/4.12.0")
                        .post(requestBody)
                        .build()

                    var shouldSwitchKey = false
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: return@use
                            val parsed = json.parseToJsonElement(bodyStr).jsonObject
                            val msgObj = parsed["choices"]?.jsonArray
                                ?.firstOrNull()?.jsonObject
                                ?.get("message")?.jsonObject
                            val content = msgObj?.get("content")?.jsonPrimitive?.content
                            val reasoning = msgObj?.get("reasoning")?.jsonPrimitive?.content
                            val text = if (!content.isNullOrBlank()) content else reasoning
                            if (!text.isNullOrBlank()) {
                                return text.trim()
                            }
                        } else if (response.code == 404) {
                            Log.w("GroqSidecar", "Model $model not found (404), trying next model...")
                        } else if (response.code == 429 || response.code == 403) {
                            Log.w("GroqSidecar", "Groq Key limit (${response.code}), failover to next key...")
                            shouldSwitchKey = true
                        }
                    }
                    if (shouldSwitchKey) {
                        break
                    }
                } catch (e: Exception) {
                    Log.w("GroqSidecar", "Groq call failed ($model): ${e.message}")
                }
            }
        }
        return ""
    }

    private fun fallbackTruncation(raw: String): String {
        val head = raw.take(800)
        val tail = raw.takeLast(800)
        return "$head\n\n... [${raw.length - 1600} karakter dipotong demi efisiensi token] ...\n\n$tail"
    }
}
