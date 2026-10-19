package id.gustyxganteng.agent.domain.context

import androidx.compose.ui.graphics.Color
import id.gustyxganteng.agent.core.database.ChatRepository
import id.gustyxganteng.agent.core.model.AgentExecutionMode
import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.model.TokenUsage
import id.gustyxganteng.agent.core.network.AgnesApiService
import kotlinx.coroutines.flow.firstOrNull

data class ContextMeterStatus(
    val usedTokens: Int,
    val maxCapacity: Int,
    val percentage: Float,
    val formattedDisplay: String,
    val compactDisplay: String,
    val indicatorColor: Color,
    val sessionPromptTokens: Long = 0L,
    val sessionReasoningTokens: Long = 0L,
    val sessionCompletionTokens: Long = 0L,
    val sessionTotalTokens: Long = 0L,
    val sessionTokensDisplay: String = ""
)

class ContextMeterManager(
    private val chatRepository: ChatRepository,
    private val agnesApiService: AgnesApiService
) {
    fun getModelCapacity(
        model: String,
        executionMode: AgentExecutionMode = AgentExecutionMode.FAST,
        claudeMaxMode: Boolean = executionMode == AgentExecutionMode.MAX
    ): Int {
        val lower = model.lowercase()
        val isMax = claudeMaxMode || executionMode == AgentExecutionMode.MAX
        return when {
            lower.contains("flash") || lower.contains("pro") || lower.contains("agnes") || lower.contains("gemini") -> 150_000
            lower.contains("gpt-4o") -> 128_000
            lower.contains("claude") && !isMax -> 150_000
            else -> 150_000
        }
    }

    fun calculateStatus(
        usedTokens: Int,
        model: String,
        executionMode: AgentExecutionMode = AgentExecutionMode.FAST,
        claudeMaxMode: Boolean = executionMode == AgentExecutionMode.MAX,
        sessionMessages: List<Message> = emptyList()
    ): ContextMeterStatus {
        val capacity = getModelCapacity(model, executionMode, claudeMaxMode)
        val clampedUsed = usedTokens.coerceAtLeast(0)
        val percentage = (clampedUsed.toFloat() / capacity.toFloat()) * 100f

        // Material 3 Expressive Tonal Status Colors
        val color = when {
            percentage < 60f -> Color(0xFF7BE598) // M3 Mint Green (Optimal)
            percentage < 85f -> Color(0xFFFFD180) // M3 Warm Amber (Moderate)
            else -> Color(0xFFFFB4AB)             // M3 Coral Red (High)
        }

        // Kalkulasi akumulasi token dalam sesi percakapan aktif
        val sessionPrompt = sessionMessages.sumOf { it.tokenUsage?.promptTokens?.toLong() ?: 0L }
        val sessionReasoning = sessionMessages.sumOf { it.tokenUsage?.reasoningTokens?.toLong() ?: 0L }
        val sessionCompletion = sessionMessages.sumOf { it.tokenUsage?.completionTokens?.toLong() ?: 0L }
        val sessionTotal = if (sessionMessages.any { it.tokenUsage != null }) {
            sessionMessages.sumOf { msg ->
                val tu = msg.tokenUsage
                if (tu != null) {
                    tu.totalTokens.toLong().coerceAtLeast(
                        tu.promptTokens.toLong() + tu.reasoningTokens.toLong() + tu.completionTokens.toLong()
                    )
                } else 0L
            }
        } else {
            clampedUsed.toLong()
        }

        val display = "${formatTokenNumber(clampedUsed)} / ${formatTokenNumber(capacity)} (${String.format(java.util.Locale.US, "%.1f", percentage)}%)"
        val compact = "${formatTokenNumber(clampedUsed)} • ${String.format(java.util.Locale.US, "%.1f", percentage)}%"
        val sessionTokensDisplay = "In: ${formatTokenNumber(sessionPrompt.toInt())} | Think: ${formatTokenNumber(sessionReasoning.toInt())} | Out: ${formatTokenNumber(sessionCompletion.toInt())}"

        return ContextMeterStatus(
            usedTokens = clampedUsed,
            maxCapacity = capacity,
            percentage = percentage,
            formattedDisplay = display,
            compactDisplay = compact,
            indicatorColor = color,
            sessionPromptTokens = sessionPrompt,
            sessionReasoningTokens = sessionReasoning,
            sessionCompletionTokens = sessionCompletion,
            sessionTotalTokens = sessionTotal,
            sessionTokensDisplay = sessionTokensDisplay
        )
    }

    companion object {
        fun formatTokenNumber(tokens: Int): String {
            return if (tokens >= 1_000_000) {
                String.format(java.util.Locale.US, "%.1fM", tokens / 1_000_000.0)
            } else if (tokens >= 1_000) {
                String.format(java.util.Locale.US, "%.1fK", tokens / 1000.0)
            } else {
                tokens.toString()
            }
        }

        fun formatFullNumber(tokens: Int): String {
            return String.format(java.util.Locale.US, "%,d", tokens)
        }
    }

    suspend fun compactSession(
        sessionId: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): Boolean {
        val currentMessages = chatRepository.getMessagesForSessionSync(sessionId)
        if (currentMessages.size <= 2) return false

        val promptForCompaction = Message(
            sessionId = sessionId,
            role = MessageRole.USER,
            content = "Buat ringkasan sangat padat dan lengkap mengenai konteks pembicaraan, keputusan teknis, dan status pengerjaan sejauh ini agar bisa digunakan sebagai konteks dasar untuk melanjutkan obrolan selanjutnya."
        )

        val fullHistory = currentMessages + promptForCompaction
        val summaryBuilder = StringBuilder()

        try {
            agnesApiService.streamChat(
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                messages = fullHistory
            ).collect { event ->
                if (event is id.gustyxganteng.agent.core.model.StreamEvent.ContentDelta) {
                    summaryBuilder.append(event.text)
                }
            }

            val summaryText = summaryBuilder.toString().trim()
            if (summaryText.isNotEmpty()) {
                chatRepository.clearSessionMessages(sessionId)

                val summaryMessage = Message(
                    sessionId = sessionId,
                    role = MessageRole.SYSTEM,
                    content = "Konteks Ringkasan Sesi Sebelumnya:\n\n$summaryText"
                )
                chatRepository.saveMessage(summaryMessage)
                return true
            }
        } catch (_: Exception) {
            return false
        }
        return false
    }
}
