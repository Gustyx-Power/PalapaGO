package id.gustyxganteng.agent.domain.context

import androidx.compose.ui.graphics.Color
import id.gustyxganteng.agent.core.database.ChatRepository
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
    val indicatorColor: Color
)

class ContextMeterManager(
    private val chatRepository: ChatRepository,
    private val agnesApiService: AgnesApiService
) {
    fun getModelCapacity(model: String): Int {
        val lower = model.lowercase()
        return when {
            lower.contains("flash") || lower.contains("pro") || lower.contains("agnes") -> 1_000_000
            lower.contains("gpt-4o") -> 128_000
            lower.contains("claude") -> 200_000
            else -> 1_000_000
        }
    }

    fun calculateStatus(usedTokens: Int, model: String): ContextMeterStatus {
        val capacity = getModelCapacity(model)
        val clampedUsed = usedTokens.coerceAtLeast(0)
        val percentage = (clampedUsed.toFloat() / capacity.toFloat()) * 100f

        val color = when {
            percentage < 50f -> Color(0xFF4CAF50) // Green
            percentage < 80f -> Color(0xFFFFC107) // Yellow
            else -> Color(0xFFF44336)             // Red
        }

        val display = "${formatTokenNumber(clampedUsed)} / ${formatTokenNumber(capacity)} (${String.format(java.util.Locale.US, "%.1f", percentage)}%)"

        return ContextMeterStatus(
            usedTokens = clampedUsed,
            maxCapacity = capacity,
            percentage = percentage,
            formattedDisplay = display,
            indicatorColor = color
        )
    }

    private fun formatTokenNumber(tokens: Int): String {
        return if (tokens >= 1_000_000) {
            String.format(java.util.Locale.US, "%.1fM", tokens / 1_000_000.0)
        } else if (tokens >= 1_000) {
            String.format(java.util.Locale.US, "%.1fK", tokens / 1000.0)
        } else {
            tokens.toString()
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
