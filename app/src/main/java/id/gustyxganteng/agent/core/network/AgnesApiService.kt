package id.gustyxganteng.agent.core.network

import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.model.StreamEvent
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.network.dto.ChatCompletionRequest
import id.gustyxganteng.agent.core.network.dto.ChatMessageDto
import id.gustyxganteng.agent.core.network.dto.FunctionCallDto
import id.gustyxganteng.agent.core.network.dto.FunctionDto
import id.gustyxganteng.agent.core.network.dto.ToolCallDto
import id.gustyxganteng.agent.core.network.dto.ToolDto
import id.gustyxganteng.agent.core.util.JsonRepairUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

interface AgnesApiService {
    fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<Message>,
        tools: List<ToolDefinition>? = null,
        budgetTokens: Int = 16384,
        maxTokens: Int = 32768,
        reasoningEffort: String = "high"
    ): Flow<StreamEvent>

    suspend fun generateImage(
        baseUrl: String,
        apiKey: String,
        prompt: String,
        model: String = "agnes-image-2.5-flash",
        size: String = "1024x1024"
    ): Result<String>
}

class AgnesApiServiceImpl(
    private val retryInterceptor: SmartRetryInterceptor,
    private val sseStreamParser: SseStreamParser = SseStreamParser(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : AgnesApiService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(retryInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private data class StreamTarget(
        val providerName: String,
        val endpoint: String,
        val apiKey: String,
        val model: String,
        val isAnthropicHeaders: Boolean = false,
        val includeThinking: Boolean = false,
        val maxTokens: Int = 65536
    )

    override fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<Message>,
        tools: List<ToolDefinition>?,
        budgetTokens: Int,
        maxTokens: Int,
        reasoningEffort: String
    ): Flow<StreamEvent> {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val baseWithV1 = if (cleanBase.endsWith("/v1")) cleanBase else "$cleanBase/v1"
        val agnesEndpoint = "$baseWithV1/chat/completions"

        // Batasi nilai max_tokens dan budget_tokens agar tidak melebihi batas keras provider (65536)
        val effectiveMaxTokens = maxTokens.coerceIn(512, 65536)
        val effectiveBudgetTokens = if (budgetTokens > 0) budgetTokens.coerceIn(1024, 32768) else 0

        val chatDtos = messages.map { msg ->
            val roleStr = when (msg.role) {
                MessageRole.SYSTEM -> "system"
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.TOOL -> "tool"
            }

            val toolCallsDto = if (msg.toolCalls.isNotEmpty()) {
                msg.toolCalls.map { tc ->
                    ToolCallDto(
                        id = tc.id,
                        type = "function",
                        function = FunctionCallDto(
                            name = tc.function.name,
                            arguments = JsonRepairUtil.repairOrSanitize(tc.function.arguments)
                        )
                    )
                }
            } else null

            val contentElement = when {
                msg.imagesBase64.isNotEmpty() -> {
                    buildJsonArray {
                        val textToUse = if (msg.content.isNotEmpty()) msg.content else "Analisis gambar yang dilampirkan ini."
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", textToUse)
                        })
                        for (img in msg.imagesBase64) {
                            val dataUri = if (img.startsWith("data:")) img else "data:image/jpeg;base64,$img"
                            add(buildJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject {
                                    put("url", dataUri)
                                })
                            })
                        }
                    }
                }
                msg.content.isNotEmpty() -> JsonPrimitive(msg.content)
                toolCallsDto != null -> null
                else -> JsonPrimitive("")
            }

            ChatMessageDto(
                role = roleStr,
                content = contentElement,
                reasoningContent = msg.reasoningContent,
                toolCalls = toolCallsDto,
                toolCallId = msg.toolCallId
            )
        }

        val toolsDto = tools?.map { tool ->
            val propsJson = buildJsonObject {
                tool.function.parameters.properties.forEach { (k, v) ->
                    put(k, buildJsonObject {
                        put("type", v.type)
                        v.description?.let { put("description", it) }
                    })
                }
            }

            val paramsJson = buildJsonObject {
                put("type", "object")
                put("properties", propsJson)
                if (tool.function.parameters.required.isNotEmpty()) {
                    put("required", buildJsonArray {
                        tool.function.parameters.required.forEach { add(JsonPrimitive(it)) }
                    })
                }
            }

            ToolDto(
                type = "function",
                function = FunctionDto(
                    name = tool.function.name,
                    description = tool.function.description,
                    parameters = paramsJson
                )
            )
        }

        // Susun Jaringan Provider Failover (Agnes Utama -> Agnes Cadangan -> Xemarux Kimi/DeepSeek -> Gemini Sidecar -> Groq Sidecar)
        val targets = mutableListOf<StreamTarget>()

        val isExplicitXemarux = model.startsWith("kimi-") ||
            model == "deepseek-v4.1-flash" ||
            model == "hy3" ||
            model == "step-5-preview"

        val xemaruxKey = KeyVault.getXemaruxApiKey()
        val xemaruxBase = KeyVault.getXemaruxBaseUrl().trimEnd('/')
        val xemaruxEndpoint = if (xemaruxBase.endsWith("/chat/completions")) xemaruxBase else "$xemaruxBase/chat/completions"

        fun addAgnesTargets() {
            val effectiveAgnesModel = if (model == "agnes-3.0-pro" || model.isBlank()) "agnes-3.0-flash" else model
            val agnesKeys = mutableListOf<String>()
            if (apiKey.isNotBlank()) agnesKeys.add(apiKey.trim())
            for (k in KeyVault.getAgnesKeyPool()) {
                if (k.isNotBlank() && !agnesKeys.contains(k)) agnesKeys.add(k)
            }
            for ((idx, k) in agnesKeys.withIndex()) {
                val label = if (idx == 0) "Agnes AI (Kunci Utama)" else "Agnes AI (Kunci Cadangan #$idx)"
                targets.add(
                    StreamTarget(
                        providerName = label,
                        endpoint = agnesEndpoint,
                        apiKey = k,
                        model = if (isExplicitXemarux) "claude-3-7-sonnet-20250219" else effectiveAgnesModel,
                        isAnthropicHeaders = true,
                        includeThinking = effectiveBudgetTokens > 0,
                        maxTokens = effectiveMaxTokens
                    )
                )
            }
        }

        fun addXemaruxTargets() {
            if (xemaruxKey.isNotBlank()) {
                val primaryXemaruxModel = if (isExplicitXemarux) model else "kimi-k2.8-preview"
                targets.add(
                    StreamTarget(
                        providerName = "Xemarux Router ($primaryXemaruxModel 40 RPM)",
                        endpoint = xemaruxEndpoint,
                        apiKey = xemaruxKey,
                        model = primaryXemaruxModel,
                        isAnthropicHeaders = false,
                        includeThinking = true,
                        maxTokens = effectiveMaxTokens.coerceAtMost(32768)
                    )
                )
                if (primaryXemaruxModel != "deepseek-v4.1-flash") {
                    targets.add(
                        StreamTarget(
                            providerName = "Xemarux Router (DeepSeek-v4.1-flash 12 RPM)",
                            endpoint = xemaruxEndpoint,
                            apiKey = xemaruxKey,
                            model = "deepseek-v4.1-flash",
                            isAnthropicHeaders = false,
                            includeThinking = false,
                            maxTokens = effectiveMaxTokens.coerceAtMost(8192)
                        )
                    )
                }
            }
        }

        fun addGroqTargets() {
            val groqKeys = KeyVault.getGroqKeyPool()
            for ((idx, gk) in groqKeys.withIndex()) {
                val groqLabel = if (idx == 0) "Groq Sidecar (Llama/OSS)" else "Groq Sidecar (Cadangan #$idx)"
                targets.add(
                    StreamTarget(
                        providerName = groqLabel,
                        endpoint = "https://api.groq.com/openai/v1/chat/completions",
                        apiKey = gk,
                        model = "openai/gpt-oss-120b",
                        isAnthropicHeaders = false,
                        includeThinking = false,
                        maxTokens = effectiveMaxTokens.coerceAtMost(8192)
                    )
                )
            }
        }

        fun addGeminiTargets() {
            val geminiKey = KeyVault.getGeminiApiKey()
            if (geminiKey.isNotBlank()) {
                targets.add(
                    StreamTarget(
                        providerName = "Gemini Sidecar (Google Cloud)",
                        endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                        apiKey = geminiKey,
                        model = "gemini-3.8-flash",
                        isAnthropicHeaders = false,
                        includeThinking = false,
                        maxTokens = effectiveMaxTokens.coerceAtMost(8192)
                    )
                )
            }
        }

        if (isExplicitXemarux) {
            addXemaruxTargets()
            addAgnesTargets()
            addGroqTargets()
            addGeminiTargets()
        } else {
            addAgnesTargets()
            addXemaruxTargets()
            addGroqTargets()
            addGeminiTargets()
        }

        if (targets.isEmpty()) {
            throw IOException("Tidak ada API Key yang terkonfigurasi pada Agnes, Gemini, maupun Groq. Periksa local.properties atau Pengaturan.")
        }

        var lastResponseCode = 0
        var lastErrBody = ""
        var lastFailedProvider = ""

        for (target in targets) {
            try {
                val isGemini = target.providerName.contains("Gemini", ignoreCase = true)
                val requestPayload = ChatCompletionRequest(
                    model = target.model,
                    messages = chatDtos,
                    tools = if (toolsDto.isNullOrEmpty()) null else toolsDto,
                    stream = true,
                    maxTokens = target.maxTokens,
                    thinking = if (!isGemini && target.includeThinking && effectiveBudgetTokens > 0) {
                        id.gustyxganteng.agent.core.network.dto.ThinkingConfigDto(budgetTokens = effectiveBudgetTokens)
                    } else null,
                    reasoningEffort = if (!isGemini && target.includeThinking && effectiveBudgetTokens > 0) reasoningEffort else null
                )

                val jsonBody = json.encodeToString(requestPayload)
                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

                val reqBuilder = Request.Builder()
                    .url(target.endpoint)
                    .addHeader("Authorization", "Bearer ${target.apiKey}")
                    .addHeader("Accept", "text/event-stream")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Accept-Encoding", "identity")
                    .addHeader("Content-Type", "application/json")

                if (target.isAnthropicHeaders) {
                    reqBuilder.addHeader("x-api-key", target.apiKey)
                    reqBuilder.addHeader("anthropic-version", "2023-06-01")
                }

                val request = reqBuilder.post(requestBody).build()
                val call = client.newCall(request)
                val response = call.execute()

                if (response.isSuccessful) {
                    val body = response.body ?: throw IOException("Empty response body from ${target.providerName}")
                    android.util.Log.i("AgnesApiService", "[Mesh Provider] Sukses terhubung ke ${target.providerName} (Model: ${target.model})")
                    return sseStreamParser.parseStream(body)
                }

                lastResponseCode = response.code
                lastErrBody = response.body?.string() ?: "HTTP ${response.code}"
                lastFailedProvider = target.providerName
                response.close()

                android.util.Log.w("AgnesApiService", "[Mesh Failover] ${target.providerName} gagal ($lastResponseCode: $lastErrBody). Beralih ke provider berikutnya...")
                continue
            } catch (e: Exception) {
                lastFailedProvider = target.providerName
                lastErrBody = e.message ?: "Network error"
                android.util.Log.w("AgnesApiService", "[Mesh Failover] ${target.providerName} exception: ${e.message}. Beralih ke provider berikutnya...")
                continue
            }
        }

        val friendlyMsg = when (lastResponseCode) {
            401 -> "Autentikasi gagal (HTTP 401) pada seluruh provider cadangan ($lastFailedProvider): $lastErrBody"
            403 -> "Akses ditolak atau kuota habis (HTTP 403) pada seluruh provider: $lastErrBody"
            404 -> "Endpoint atau model tidak ditemukan (HTTP 404): $lastErrBody"
            429 -> "Rate limit tercapai pada seluruh provider (HTTP 429): $lastErrBody"
            else -> "Gagal memproses permintaan ($lastResponseCode pada $lastFailedProvider): $lastErrBody"
        }
        throw IOException(friendlyMsg)
    }

    override suspend fun generateImage(
        baseUrl: String,
        apiKey: String,
        prompt: String,
        model: String,
        size: String
    ): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val keyPool = mutableListOf<String>()
        if (apiKey.isNotBlank()) keyPool.add(apiKey.trim())
        val vaultPool = KeyVault.getAgnesKeyPool()
        vaultPool.forEach { if (!keyPool.contains(it)) keyPool.add(it) }
        if (keyPool.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("API Key Agnes tidak tersedia."))
        }

        val effectiveBaseUrl = if (baseUrl.isBlank()) KeyVault.getAgnesBaseUrl() else baseUrl
        val endpoint = "${effectiveBaseUrl.trimEnd('/')}/images/generations"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        var lastException: Exception? = null

        for (activeKey in keyPool) {
            try {
                val payload = buildJsonObject {
                    put("model", model)
                    put("prompt", prompt)
                    put("n", 1)
                    put("size", size)
                }

                val requestBody = payload.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $activeKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: return@use
                        val parsed = json.parseToJsonElement(bodyStr).jsonObject
                        val dataArray = parsed["data"]?.jsonArray
                        val firstObj = dataArray?.firstOrNull()?.jsonObject
                        val url = firstObj?.get("url")?.jsonPrimitive?.content
                        if (!url.isNullOrBlank()) {
                            return@withContext Result.success(url)
                        } else {
                            val b64 = firstObj?.get("b64_json")?.jsonPrimitive?.content
                            if (!b64.isNullOrBlank()) {
                                return@withContext Result.success("data:image/png;base64,$b64")
                            }
                        }
                    } else {
                        val errStr = response.body?.string() ?: "HTTP ${response.code}"
                        android.util.Log.w("AgnesApiService", "[Image Failover] Kunci Agnes gagal (${response.code}: $errStr). Mencoba kunci cadangan...")
                        lastException = IOException("Agnes Image API Error ${response.code}: $errStr")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("AgnesApiService", "[Image Failover] Exception: ${e.message}. Mencoba kunci cadangan...")
                lastException = e
            }
        }

        Result.failure(lastException ?: IOException("Gagal generate gambar dengan Agnes AI."))
    }
}
