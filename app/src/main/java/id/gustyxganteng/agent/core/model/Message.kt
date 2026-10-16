package id.gustyxganteng.agent.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MessageRole {
    @SerialName("system")
    SYSTEM,
    @SerialName("user")
    USER,
    @SerialName("assistant")
    ASSISTANT,
    @SerialName("tool")
    TOOL
}

@Serializable
data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sessionId: String,
    val role: MessageRole,
    val content: String = "",
    val reasoningContent: String? = null,
    val thinkingDurationMs: Long? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val imagesBase64: List<String> = emptyList(),
    val tokenUsage: TokenUsage? = null,
    val timestamp: Long = System.currentTimeMillis()
)
