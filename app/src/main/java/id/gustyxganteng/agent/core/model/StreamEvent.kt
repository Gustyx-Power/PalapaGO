package id.gustyxganteng.agent.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface StreamEvent {
    data class ContentDelta(val text: String) : StreamEvent
    data class ReasoningDelta(val text: String) : StreamEvent
    data class ToolCallChunk(val index: Int, val id: String?, val name: String?, val argumentsDelta: String) : StreamEvent
    data class UsageUpdate(val usage: TokenUsage) : StreamEvent
    data class RateLimitWarning(val retryAfterSeconds: Long, val attempt: Int, val maxAttempts: Int) : StreamEvent
    data object StreamCompleted : StreamEvent
    data class StreamError(val error: Throwable, val message: String) : StreamEvent
}

@Serializable
data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "Sesi Baru",
    val model: String = "agnes-3.0-flash",
    val workspacePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
