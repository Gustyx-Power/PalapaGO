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
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
                            arguments = tc.function.arguments
                        )
                    )
                }
            } else null

            ChatMessageDto(
                role = roleStr,
                content = if (msg.content.isNotEmpty()) msg.content else if (toolCallsDto != null) null else "",
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
            thinking = id.gustyxganteng.agent.core.network.dto.ThinkingConfigDto(budgetTokens = budgetTokens),
            reasoningEffort = reasoningEffort
        )

        val jsonBody = json.encodeToString(requestPayload)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("x-api-key", apiKey)
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

        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: "HTTP ${response.code}"
            response.close()
            throw IOException("Agnes API error (${response.code}): $errBody")
        }

        val body = response.body ?: throw IOException("Empty response body from Agnes API")
        return sseStreamParser.parseStream(body)
    }
}
