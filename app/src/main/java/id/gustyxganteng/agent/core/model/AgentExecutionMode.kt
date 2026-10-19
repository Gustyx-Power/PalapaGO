package id.gustyxganteng.agent.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AgentExecutionMode(
    val id: String,
    val badge: String,
    val maxTurns: Int,
    val budgetTokens: Int,
    val maxTokens: Int,
    val reasoningEffort: String
) {
    @SerialName("fast")
    FAST(
        id = "fast",
        badge = "CEPAT",
        maxTurns = 6,
        budgetTokens = 2048,
        maxTokens = 8192,
        reasoningEffort = "low"
    ),

    @SerialName("standard")
    STANDARD(
        id = "standard",
        badge = "STANDAR",
        maxTurns = 10,
        budgetTokens = 4096,
        maxTokens = 16384,
        reasoningEffort = "medium"
    ),

    @SerialName("high")
    HIGH(
        id = "high",
        badge = "TINGGI",
        maxTurns = 16,
        budgetTokens = 8192,
        maxTokens = 24576,
        reasoningEffort = "high"
    ),

    @SerialName("smart")
    SMART(
        id = "smart",
        badge = "SMART",
        maxTurns = 20,
        budgetTokens = 12288,
        maxTokens = 32768,
        reasoningEffort = "high"
    ),

    @SerialName("max")
    MAX(
        id = "max",
        badge = "MAX",
        maxTurns = 30,
        budgetTokens = 16384,
        maxTokens = 65536,
        reasoningEffort = "high"
    );

    companion object {
        fun fromId(id: String?): AgentExecutionMode {
            return when (id?.lowercase()?.trim()) {
                "fast", "cepat" -> FAST
                "standard", "standar" -> STANDARD
                "high", "tinggi" -> HIGH
                "smart", "cerdas" -> SMART
                "max", "maximum", "maksimal", "claude_max" -> MAX
                else -> FAST
            }
        }
    }
}
