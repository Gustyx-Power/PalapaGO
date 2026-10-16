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

class SseStreamParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) {
    fun parseStream(responseBody: ResponseBody): Flow<StreamEvent> = flow {
        val source = responseBody.source()
        val currentThinkingBuffer = StringBuilder()
        val currentContentBuffer = StringBuilder()
        var isInsideThinkTag = false
        var lastReceivedUsage: TokenUsage? = null

        try {
            while (true) {
                val line = source.readUtf8Line() ?: break
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        break
                    }
                    if (data.isNotEmpty()) {
                        try {
                            val chunk = json.decodeFromString<ChatCompletionChunk>(data)

                            // 1. Ekstrak delta choices
                            for (choice in chunk.choices) {
                                val delta = choice.delta ?: continue

                                // Cek explicit reasoning (reasoning_content, thinking, reasoning)
                                val explicitReasoning = delta.reasoningContent
                                    ?: delta.thinking
                                    ?: delta.reasoning

                                if (!explicitReasoning.isNullOrEmpty()) {
                                    currentThinkingBuffer.append(explicitReasoning)
                                    emit(StreamEvent.ReasoningDelta(explicitReasoning))
                                }

                                // Cek delta.content dan pisahkan tag <think>...</think> secara real-time
                                delta.content?.let { rawContent ->
                                    if (rawContent.isNotEmpty()) {
                                        var currentText = rawContent

                                        while (currentText.isNotEmpty()) {
                                            if (!isInsideThinkTag) {
                                                val thinkTag = when {
                                                    currentText.contains("<think>", ignoreCase = true) -> "<think>"
                                                    currentText.contains("<thought>", ignoreCase = true) -> "<thought>"
                                                    else -> null
                                                }

                                                if (thinkTag != null) {
                                                    val beforeTag = currentText.substringBefore(thinkTag)
                                                    if (beforeTag.isNotEmpty()) {
                                                        currentContentBuffer.append(beforeTag)
                                                        emit(StreamEvent.ContentDelta(beforeTag))
                                                    }
                                                    isInsideThinkTag = true
                                                    currentText = currentText.substringAfter(thinkTag)
                                                } else {
                                                    currentContentBuffer.append(currentText)
                                                    emit(StreamEvent.ContentDelta(currentText))
                                                    currentText = ""
                                                }
                                            } else {
                                                val closeTag = when {
                                                    currentText.contains("</think>", ignoreCase = true) -> "</think>"
                                                    currentText.contains("</thought>", ignoreCase = true) -> "</thought>"
                                                    else -> null
                                                }

                                                if (closeTag != null) {
                                                    val thinkText = currentText.substringBefore(closeTag)
                                                    if (thinkText.isNotEmpty()) {
                                                        currentThinkingBuffer.append(thinkText)
                                                        emit(StreamEvent.ReasoningDelta(thinkText))
                                                    }
                                                    isInsideThinkTag = false
                                                    currentText = currentText.substringAfter(closeTag)
                                                } else {
                                                    currentThinkingBuffer.append(currentText)
                                                    emit(StreamEvent.ReasoningDelta(currentText))
                                                    currentText = ""
                                                }
                                            }
                                        }
                                    }
                                }

                                // Cek tool calls delta
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

                            // 2. Kalkulasi Token Thinking yang Akurat dari usage
                            chunk.usage?.let { u ->
                                val apiReasoningTokens = u.completionTokensDetails?.reasoningTokens ?: 0
                                val computedReasoningTokens = if (apiReasoningTokens > 0) {
                                    apiReasoningTokens
                                } else if (currentThinkingBuffer.isNotEmpty()) {
                                    (currentThinkingBuffer.length / 4).coerceAtLeast(1)
                                } else {
                                    0
                                }

                                val usage = TokenUsage(
                                    promptTokens = u.promptTokens,
                                    completionTokens = u.completionTokens,
                                    totalTokens = u.totalTokens,
                                    reasoningTokens = computedReasoningTokens
                                )
                                lastReceivedUsage = usage
                                emit(StreamEvent.UsageUpdate(usage))
                            }
                        } catch (_: Exception) {
                            // Abaikan chunk malformed atau keep-alive ping
                        }
                    }
                }
            }

            // 3. Pastikan token usage final dipancarkan saat stream selesai
            val finalThinkingTokens = if (currentThinkingBuffer.isNotEmpty()) {
                (currentThinkingBuffer.length / 4).coerceAtLeast(1)
            } else 0

            val finalCompletionTokens = if (currentContentBuffer.isNotEmpty()) {
                (currentContentBuffer.length / 4).coerceAtLeast(1)
            } else 0

            val finalUsage = if (lastReceivedUsage != null) {
                if (lastReceivedUsage!!.reasoningTokens == 0 && finalThinkingTokens > 0) {
                    lastReceivedUsage!!.copy(reasoningTokens = finalThinkingTokens)
                } else {
                    lastReceivedUsage!!
                }
            } else {
                TokenUsage(
                    promptTokens = 0,
                    completionTokens = finalCompletionTokens,
                    totalTokens = finalCompletionTokens + finalThinkingTokens,
                    reasoningTokens = finalThinkingTokens
                )
            }
            emit(StreamEvent.UsageUpdate(finalUsage))
            emit(StreamEvent.StreamCompleted)
        } catch (e: Throwable) {
            emit(StreamEvent.StreamError(e, e.localizedMessage ?: "Unknown SSE stream error"))
        } finally {
            try {
                source.close()
                responseBody.close()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
}
