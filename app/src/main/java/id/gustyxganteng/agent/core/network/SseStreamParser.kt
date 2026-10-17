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

        val pendingBuffer = StringBuilder()

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

                            // Dukungan Anthropic root delta (jika gateway mem-forward format Anthropic)
                            val rootDelta = chunk.delta
                            if (rootDelta != null) {
                                val rootReasoning = rootDelta.reasoningContent
                                    ?: rootDelta.thinking
                                    ?: rootDelta.reasoning
                                    ?: if (rootDelta.type == "thinking_delta") (rootDelta.text ?: rootDelta.content) else null

                                if (!rootReasoning.isNullOrEmpty()) {
                                    currentThinkingBuffer.append(rootReasoning)
                                    emit(StreamEvent.ReasoningDelta(rootReasoning))
                                } else {
                                    val rootText = rootDelta.text ?: rootDelta.content
                                    if (!rootText.isNullOrEmpty() && rootDelta.type != "thinking_delta") {
                                        currentContentBuffer.append(rootText)
                                        emit(StreamEvent.ContentDelta(rootText))
                                    }
                                }
                            }

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

                                // Cek delta.content dan pisahkan tag <think>...</think> secara streaming aman batas chunk
                                delta.content?.let { rawContent ->
                                    if (rawContent.isNotEmpty()) {
                                        pendingBuffer.append(rawContent)

                                        while (pendingBuffer.isNotEmpty()) {
                                            if (!isInsideThinkTag) {
                                                val lower = pendingBuffer.toString().lowercase()
                                                val thinkIdx = lower.indexOf("<think>")
                                                val thoughtIdx = lower.indexOf("<thought>")
                                                val reasoningIdx = lower.indexOf("<reasoning>")
                                                val validIndices = listOf(thinkIdx, thoughtIdx, reasoningIdx).filter { it != -1 }
                                                val matchIdx = validIndices.minOrNull() ?: -1
                                                val matchTagLen = when (matchIdx) {
                                                    thinkIdx -> 7
                                                    thoughtIdx -> 9
                                                    reasoningIdx -> 11
                                                    else -> 0
                                                }

                                                if (matchIdx != -1) {
                                                    val before = pendingBuffer.substring(0, matchIdx)
                                                    if (before.isNotEmpty()) {
                                                        currentContentBuffer.append(before)
                                                        emit(StreamEvent.ContentDelta(before))
                                                    }
                                                    pendingBuffer.delete(0, matchIdx + matchTagLen)
                                                    isInsideThinkTag = true
                                                } else {
                                                    // Cek apakah ada akhiran pendingBuffer yang berpotensi merupakan prefix dari <think>, <thought>, atau <reasoning>
                                                    val str = pendingBuffer.toString()
                                                    var prefixLen = 0
                                                    val maxCheck = minOf(str.length, 10)
                                                    for (k in maxCheck downTo 1) {
                                                        val suffix = str.takeLast(k).lowercase()
                                                        if ("<think>".startsWith(suffix) || "<thought>".startsWith(suffix) || "<reasoning>".startsWith(suffix)) {
                                                            prefixLen = k
                                                            break
                                                        }
                                                    }

                                                    if (prefixLen > 0) {
                                                        val emitLen = str.length - prefixLen
                                                        if (emitLen > 0) {
                                                            val toEmit = str.substring(0, emitLen)
                                                            currentContentBuffer.append(toEmit)
                                                            emit(StreamEvent.ContentDelta(toEmit))
                                                            pendingBuffer.delete(0, emitLen)
                                                        }
                                                        break
                                                    } else {
                                                        currentContentBuffer.append(str)
                                                        emit(StreamEvent.ContentDelta(str))
                                                        pendingBuffer.clear()
                                                    }
                                                }
                                            } else {
                                                val lower = pendingBuffer.toString().lowercase()
                                                val closeThinkIdx = lower.indexOf("</think>")
                                                val closeThoughtIdx = lower.indexOf("</thought>")
                                                val closeReasoningIdx = lower.indexOf("</reasoning>")
                                                val validCloseIndices = listOf(closeThinkIdx, closeThoughtIdx, closeReasoningIdx).filter { it != -1 }
                                                val matchIdx = validCloseIndices.minOrNull() ?: -1
                                                val matchTagLen = when (matchIdx) {
                                                    closeThinkIdx -> 8
                                                    closeThoughtIdx -> 10
                                                    closeReasoningIdx -> 12
                                                    else -> 0
                                                }

                                                if (matchIdx != -1) {
                                                    val thinkText = pendingBuffer.substring(0, matchIdx)
                                                    if (thinkText.isNotEmpty()) {
                                                        currentThinkingBuffer.append(thinkText)
                                                        emit(StreamEvent.ReasoningDelta(thinkText))
                                                    }
                                                    pendingBuffer.delete(0, matchIdx + matchTagLen)
                                                    isInsideThinkTag = false
                                                } else {
                                                    // Cek apakah ada akhiran yang merupakan prefix dari </think>, </thought>, atau </reasoning>
                                                    val str = pendingBuffer.toString()
                                                    var prefixLen = 0
                                                    val maxCheck = minOf(str.length, 11)
                                                    for (k in maxCheck downTo 1) {
                                                        val suffix = str.takeLast(k).lowercase()
                                                        if ("</think>".startsWith(suffix) || "</thought>".startsWith(suffix) || "</reasoning>".startsWith(suffix)) {
                                                            prefixLen = k
                                                            break
                                                        }
                                                    }

                                                    if (prefixLen > 0) {
                                                        val emitLen = str.length - prefixLen
                                                        if (emitLen > 0) {
                                                            val toEmit = str.substring(0, emitLen)
                                                            currentThinkingBuffer.append(toEmit)
                                                            emit(StreamEvent.ReasoningDelta(toEmit))
                                                            pendingBuffer.delete(0, emitLen)
                                                        }
                                                        break
                                                    } else {
                                                        currentThinkingBuffer.append(str)
                                                        emit(StreamEvent.ReasoningDelta(str))
                                                        pendingBuffer.clear()
                                                    }
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

            // Flush sisa pendingBuffer jika ada
            if (pendingBuffer.isNotEmpty()) {
                val remaining = pendingBuffer.toString()
                if (isInsideThinkTag) {
                    currentThinkingBuffer.append(remaining)
                    emit(StreamEvent.ReasoningDelta(remaining))
                } else {
                    currentContentBuffer.append(remaining)
                    emit(StreamEvent.ContentDelta(remaining))
                }
                pendingBuffer.clear()
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
            val isTimeout = e is java.net.SocketTimeoutException || e.message?.contains("timeout", ignoreCase = true) == true
            val errorMsg = if (isTimeout) {
                "Koneksi terputus karena batas waktu (Timeout) saat menerima respons. Silakan tekan Coba Lagi."
            } else {
                e.localizedMessage ?: "Terjadi kesalahan saat memproses streaming data."
            }
            emit(StreamEvent.StreamError(e, errorMsg))
        } finally {
            try {
                source.close()
                responseBody.close()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
}
