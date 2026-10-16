package id.gustyxganteng.agent.core.network.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val tools: List<ToolDto>? = null,
    @SerialName("tool_choice")
    val toolChoice: String? = if (tools.isNullOrEmpty()) null else "auto",
    val stream: Boolean = true,
    @SerialName("stream_options")
    val streamOptions: StreamOptionsDto = StreamOptionsDto(includeUsage = true),
    val temperature: Double? = 0.7
)

@Serializable
data class StreamOptionsDto(
    @SerialName("include_usage")
    val includeUsage: Boolean = true
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCallDto>? = null,
    @SerialName("tool_call_id")
    val toolCallId: String? = null
)

@Serializable
data class ToolDto(
    val type: String = "function",
    val function: FunctionDto
)

@Serializable
data class FunctionDto(
    val name: String,
    val description: String,
    val parameters: kotlinx.serialization.json.JsonObject
)

@Serializable
data class ToolCallDto(
    val id: String? = null,
    val index: Int? = null,
    val type: String? = "function",
    val function: FunctionCallDto? = null
)

@Serializable
data class FunctionCallDto(
    val name: String? = null,
    val arguments: String? = null
)

@Serializable
data class ChatCompletionChunk(
    val id: String? = null,
    val model: String? = null,
    val choices: List<StreamChoiceDto> = emptyList(),
    val usage: TokenUsageDto? = null
)

@Serializable
data class StreamChoiceDto(
    val index: Int = 0,
    val delta: StreamDeltaDto? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class StreamDeltaDto(
    val role: String? = null,
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCallDto>? = null
)

@Serializable
data class TokenUsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
    @SerialName("completion_tokens_details")
    val completionTokensDetails: CompletionTokensDetailsDto? = null
)

@Serializable
data class CompletionTokensDetailsDto(
    @SerialName("reasoning_tokens")
    val reasoningTokens: Int = 0
)
