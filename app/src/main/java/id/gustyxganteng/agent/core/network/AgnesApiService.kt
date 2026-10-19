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
        val endpoint = "$baseWithV1/chat/completions"

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

        val requestPayload = ChatCompletionRequest(
            model = model,
            messages = chatDtos,
            tools = if (toolsDto.isNullOrEmpty()) null else toolsDto,
            stream = true,
            maxTokens = maxTokens,
            thinking = if (budgetTokens > 0) id.gustyxganteng.agent.core.network.dto.ThinkingConfigDto(budgetTokens = budgetTokens) else null,
            reasoningEffort = if (budgetTokens > 0) reasoningEffort else null
        )

        val jsonBody = json.encodeToString(requestPayload)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val candidateKeys = mutableListOf<String>()
        if (apiKey.isNotBlank()) candidateKeys.add(apiKey.trim())
        for (k in KeyVault.getAgnesKeyPool()) {
            if (k.isNotBlank() && !candidateKeys.contains(k)) {
                candidateKeys.add(k)
            }
        }

        if (candidateKeys.isEmpty()) {
            throw IOException("API Key belum terkonfigurasi. Pastikan API Key sudah diisi pada Pengaturan atau local.properties.")
        }

        var lastResponseCode = 0
        var lastErrBody = ""

        for ((index, currentKey) in candidateKeys.withIndex()) {
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $currentKey")
                .addHeader("x-api-key", currentKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Connection", "keep-alive")
                .addHeader("Accept-Encoding", "identity")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val call = client.newCall(request)
            val response = call.execute()

            if (response.isSuccessful) {
                val body = response.body ?: throw IOException("Empty response body from API server")
                return sseStreamParser.parseStream(body)
            }

            lastResponseCode = response.code
            lastErrBody = response.body?.string() ?: "HTTP ${response.code}"
            response.close()

            // Jika kena Rate Limit (429) atau Quota (403), coba failover ke key berikutnya jika ada
            if ((lastResponseCode == 429 || lastResponseCode == 403) && index < candidateKeys.size - 1) {
                android.util.Log.w("AgnesApiService", "Kunci Agnes #$index terkena limit ($lastResponseCode). Beralih ke kunci cadangan...")
                continue
            }
            break
        }

        val friendlyMsg = when (lastResponseCode) {
            401 -> "Autentikasi gagal (HTTP 401): API Key tidak valid. Periksa konfigurasi API Key di Pengaturan."
            403 -> "Akses ditolak (HTTP 403): Tidak memiliki izin akses atau kuota habis: $lastErrBody"
            404 -> "Endpoint API tidak ditemukan (HTTP 404): $endpoint"
            429 -> "Rate limit tercapai (HTTP 429): Terlalu banyak permintaan ke server API ($lastErrBody). Mohon tunggu sesaat."
            else -> "Gagal memproses permintaan ($lastResponseCode): $lastErrBody"
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
        if (apiKey.isNotBlank()) keyPool.add(apiKey)
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
                    } else if (response.code == 429 || response.code == 403) {
                        lastException = IOException("Agnes Image API Key limit (${response.code}), failover...")
                    } else {
                        val errStr = response.body?.string() ?: ""
                        lastException = IOException("Agnes Image API Error ${response.code}: $errStr")
                    }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: IOException("Gagal generate gambar dengan Agnes AI."))
    }
}
