package id.gustyxganteng.agent.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
    @SerialName("reasoning_tokens")
    val reasoningTokens: Int = 0
) {
    fun toCompactString(): String {
        return "In: ${formatToken(promptTokens)} | Think: ${formatToken(reasoningTokens)} | Out: ${formatToken(completionTokens)}"
    }

    private fun formatToken(count: Int): String {
        return if (count >= 1000) {
            String.format(java.util.Locale.US, "%.1fK", count / 1000.0)
        } else {
            count.toString()
        }
    }
}
