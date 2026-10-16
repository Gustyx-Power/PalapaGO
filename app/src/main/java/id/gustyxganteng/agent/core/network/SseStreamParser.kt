package id.gustyxganteng.agent.core.network

import id.gustyxganteng.agent.core.model.StreamEvent
import id.gustyxganteng.agent.core.model.TokenUsage
import id.gustyxganteng.agent.core.network.dto.ChatCompletionChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.InputStreamReader

class SseStreamParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) {
    fun parseStream(responseBody: ResponseBody): Flow<StreamEvent> = flow {
        val reader = BufferedReader(InputStreamReader(responseBody.byteStream(), Charsets.UTF_8))
        var isInsideThinkTag = false
        var accumulatedThinkingChars = 0

        try {
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        emit(StreamEvent.StreamCompleted)
                        break
                    }
                    if (data.isNotEmpty()) {
                        try {
                            val chunk = json.decodeFromString<ChatCompletionChunk>(data)

                            // Emit delta choices
                            for (choice in chunk.choices) {
                                val delta = choice.delta ?: continue

                                // 1. Ekstrak teks berpikir dari reasoning_content, thinking, atau reasoning
                                val explicitReasoning = delta.reasoningContent
                                    ?: delta.thinking
                                    ?: delta.reasoning

                                if (!explicitReasoning.isNullOrEmpty()) {
                                    accumulatedThinkingChars += explicitReasoning.length
                                    emit(StreamEvent.ReasoningDelta(explicitReasoning))
                                }

                                // 2. Ekstrak delta content dan pisahkan tag <think>...</think> secara real-time
                                delta.content?.let { rawContent ->
                                    if (rawContent.isNotEmpty()) {
                                        var currentText = rawContent

                                        while (currentText.isNotEmpty()) {
                                            if (!isInsideThinkTag) {
                                                if (currentText.contains("<think>")) {
                                                    val beforeTag = currentText.substringBefore("<think>")
                                                    if (beforeTag.isNotEmpty()) {
                                                        emit(StreamEvent.ContentDelta(beforeTag))
                                                    }
                                                    isInsideThinkTag = true
                                                    currentText = currentText.substringAfter("<think>")
                                                } else {
                                                    emit(StreamEvent.ContentDelta(currentText))
                                                    currentText = ""
                                                }
                                            } else {
                                                if (currentText.contains("</think>")) {
                                                    val thinkText = currentText.substringBefore("</think>")
                                                    if (thinkText.isNotEmpty()) {
                                                        accumulatedThinkingChars += thinkText.length
                                                        emit(StreamEvent.ReasoningDelta(thinkText))
                                                    }
                                                    isInsideThinkTag = false
                                                    currentText = currentText.substringAfter("</think>")
                                                } else {
                                                    accumulatedThinkingChars += currentText.length
                                                    emit(StreamEvent.ReasoningDelta(currentText))
                                                    currentText = ""
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Tool calls delta
                                delta.toolCalls?.forEach { toolCall ->
                                    emit(
                                        StreamEvent.ToolCallChunk(
                                            index = toolCall.index ?: 0,
                                            id = toolCall.id,
                                            name = toolCall.function?.name,
                                            argumentsDelta = toolCall.function?.arguments ?: ""
                                        )
                                    )
                                }
                            }

                            // 4. Token usage calculation dengan fallback thinkingTokens
                            chunk.usage?.let { u ->
                                val apiReasoningTokens = u.completionTokensDetails?.reasoningTokens ?: 0
                                val computedReasoningTokens = if (apiReasoningTokens > 0) {
                                    apiReasoningTokens
                                } else if (accumulatedThinkingChars > 0) {
                                    (accumulatedThinkingChars / 4).coerceAtLeast(1)
                                } else {
                                    0
                                }

                                emit(
                                    StreamEvent.UsageUpdate(
                                        TokenUsage(
                                            promptTokens = u.promptTokens,
                                            completionTokens = u.completionTokens,
                                            totalTokens = u.totalTokens,
                                            reasoningTokens = computedReasoningTokens
                                        )
                                    )
                                )
                            }
                        } catch (_: Exception) {
                            // Abaikan chunk malformed atau keep-alive ping
                        }
                    }
                }
                line = reader.readLine()
            }
            emit(StreamEvent.StreamCompleted)
        } catch (e: Throwable) {
            emit(StreamEvent.StreamError(e, e.localizedMessage ?: "Unknown SSE stream error"))
        } finally {
            try {
                reader.close()
                responseBody.close()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
}
