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
                            
                            // Emit token usage jika ada di chunk
                            chunk.usage?.let { u ->
                                emit(
                                    StreamEvent.UsageUpdate(
                                        TokenUsage(
                                            promptTokens = u.promptTokens,
                                            completionTokens = u.completionTokens,
                                            totalTokens = u.totalTokens,
                                            reasoningTokens = u.completionTokensDetails?.reasoningTokens ?: 0
                                        )
                                    )
                                )
                            }

                            // Emit delta choices
                            for (choice in chunk.choices) {
                                val delta = choice.delta ?: continue
                                
                                // Reasoning delta
                                delta.reasoningContent?.let { reasoning ->
                                    if (reasoning.isNotEmpty()) {
                                        emit(StreamEvent.ReasoningDelta(reasoning))
                                    }
                                }

                                // Normal content delta
                                delta.content?.let { text ->
                                    if (text.isNotEmpty()) {
                                        emit(StreamEvent.ContentDelta(text))
                                    }
                                }

                                // Tool calls delta
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
                        } catch (e: Exception) {
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
