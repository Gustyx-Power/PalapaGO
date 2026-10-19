package id.gustyxganteng.agent.domain.agent

import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.database.ChatRepository
import id.gustyxganteng.agent.core.model.AgentExecutionMode
import id.gustyxganteng.agent.core.model.CanvasArtifact
import id.gustyxganteng.agent.core.model.FunctionCall
import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.model.StreamEvent
import id.gustyxganteng.agent.core.model.TokenUsage
import id.gustyxganteng.agent.core.model.ToolCall
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.model.ToolExecutionResult
import id.gustyxganteng.agent.core.network.AgnesApiService
import id.gustyxganteng.agent.core.util.JsonRepairUtil
import id.gustyxganteng.agent.domain.tools.AcademicTool
import id.gustyxganteng.agent.domain.tools.CanvasTool
import id.gustyxganteng.agent.domain.tools.FileSystemTool
import id.gustyxganteng.agent.domain.tools.ImageTool
import id.gustyxganteng.agent.domain.tools.ShellTool
import id.gustyxganteng.agent.domain.tools.WebTool
import id.gustyxganteng.agent.core.model.PromptMode
import id.gustyxganteng.agent.core.network.GroqSidecarService
import id.gustyxganteng.agent.core.network.GeminiSidecarService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private class StreamTimeoutException(message: String, cause: Throwable? = null) : java.io.IOException(message, cause)

private const val THINK_ENFORCEMENT = "\n\n[Instruction: You MUST begin your response with <think> and write your deep internal reasoning monologue before any tool call or response text.]"

sealed interface AgentState {
    data object Idle : AgentState
    data class Streaming(
        val content: String,
        val reasoning: String?,
        val thinkingElapsedMs: Long?,
        val isThinking: Boolean = false,
        val thinkingTokens: Int = 0,
        val currentActivity: String = "Menganalisis instruksi...",
        val typingElapsedMs: Long? = null
    ) : AgentState
    data class AwaitingApproval(
        val toolCall: ToolCall,
        val toolName: String,
        val details: String,
        val isShell: Boolean,
        val targetPath: String?
    ) : AgentState
    data class ExecutingTool(val toolName: String, val description: String) : AgentState
    data class Error(val message: String) : AgentState
}

class AgentOrchestrator(
    private val agnesApiService: AgnesApiService,
    private val chatRepository: ChatRepository,
    private val fileSystemTool: FileSystemTool,
    private val shellTool: ShellTool,
    private val webTool: WebTool,
    private val academicTool: AcademicTool = AcademicTool(),
    private val imageTool: ImageTool? = null,
    private val settingsRepository: AppSettingsRepository? = null,
    private val groqSidecarService: GroqSidecarService? = null,
    private val geminiSidecarService: GeminiSidecarService? = null,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private var activeJob: Job? = null
    private var pendingApprovalDeferred: CompletableDeferred<Boolean>? = null

    var onCanvasArtifactCreated: ((CanvasArtifact) -> Unit)? = null
    var onTurnFinished: (() -> Unit)? = null

    private val canvasTool = CanvasTool { artifact ->
        onCanvasArtifactCreated?.invoke(artifact)
    }

    fun approvePendingAction() {
        pendingApprovalDeferred?.complete(true)
    }

    fun rejectPendingAction() {
        pendingApprovalDeferred?.complete(false)
    }

    fun clearError() {
        if (_agentState.value is AgentState.Error) {
            _agentState.value = AgentState.Idle
        }
    }

    fun cancel() {
        activeJob?.cancel()
        pendingApprovalDeferred?.complete(false)
        _agentState.value = AgentState.Idle
        onTurnFinished?.invoke()
    }

    fun runTurn(
        scope: CoroutineScope,
        sessionId: String,
        baseUrl: String,
        apiKey: String,
        model: String,
        workspacePath: String?,
        yoloMode: Boolean,
        enableWebSearch: Boolean,
        useTermuxEnv: Boolean,
        executionMode: AgentExecutionMode = AgentExecutionMode.FAST,
        antiAiSlopMode: Boolean = true,
        tavilyDeepSearchEnabled: Boolean = false,
        academicMode: Boolean = false,
        promptMode: PromptMode = PromptMode.CHAT,
        claudeMaxMode: Boolean = executionMode == AgentExecutionMode.MAX
    ) {
        val previousJob = activeJob
        activeJob?.cancel()
        activeJob = scope.launch(Dispatchers.IO) {
            _agentState.value = AgentState.Streaming(
                content = "",
                reasoning = null,
                thinkingElapsedMs = 0,
                isThinking = true,
                thinkingTokens = 0,
                currentActivity = "Mempersiapkan instruksi..."
            )
            try {
                previousJob?.join()
                executeTurnLoop(
                    sessionId = sessionId,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    model = model,
                    workspacePath = workspacePath,
                    yoloMode = yoloMode,
                    enableWebSearch = enableWebSearch,
                    useTermuxEnv = useTermuxEnv,
                    executionMode = executionMode,
                    antiAiSlopMode = antiAiSlopMode,
                    tavilyDeepSearchEnabled = tavilyDeepSearchEnabled,
                    academicMode = academicMode,
                    promptMode = promptMode,
                    claudeMaxMode = claudeMaxMode
                )
            } catch (e: CancellationException) {
                // Jangan ubah CancellationException menjadi AgentState.Error
                throw e
            } catch (e: Exception) {
                _agentState.value = AgentState.Error(e.localizedMessage ?: "Terjadi kesalahan pada Agent")
            } finally {
                if (_agentState.value !is AgentState.Error) {
                    _agentState.value = AgentState.Idle
                }
                onTurnFinished?.invoke()
            }
        }
    }

    private suspend fun executeTurnLoop(
        sessionId: String,
        baseUrl: String,
        apiKey: String,
        model: String,
        workspacePath: String?,
        yoloMode: Boolean,
        enableWebSearch: Boolean,
        useTermuxEnv: Boolean,
        executionMode: AgentExecutionMode = AgentExecutionMode.FAST,
        antiAiSlopMode: Boolean = true,
        tavilyDeepSearchEnabled: Boolean = false,
        academicMode: Boolean = false,
        promptMode: PromptMode = PromptMode.CHAT,
        claudeMaxMode: Boolean = executionMode == AgentExecutionMode.MAX
    ) {
        var continueLoop = true
        var turnCount = 0
        val effectiveMode = if (claudeMaxMode && executionMode != AgentExecutionMode.MAX) {
            AgentExecutionMode.MAX
        } else {
            executionMode
        }
        val maxTurns = effectiveMode.maxTurns
        val budgetTokens = effectiveMode.budgetTokens
        val maxTokens = effectiveMode.maxTokens
        val reasoningEffort = effectiveMode.reasoningEffort

        // Kumpulkan definisi tool yang aktif berdasarkan promptMode
        val activeTools = mutableListOf<ToolDefinition>()
        activeTools.add(ImageTool.DEFINITION)
        when (promptMode) {
            PromptMode.CHAT -> {
                if (enableWebSearch) activeTools.addAll(WebTool.DEFINITIONS)
                activeTools.add(CanvasTool.DEFINITION)
            }
            PromptMode.CANVAS -> {
                activeTools.add(CanvasTool.DEFINITION)
                activeTools.addAll(FileSystemTool.DEFINITIONS)
                if (enableWebSearch) activeTools.addAll(WebTool.DEFINITIONS)
            }
            PromptMode.ACADEMIC -> {
                activeTools.addAll(AcademicTool.DEFINITIONS)
                if (enableWebSearch) activeTools.addAll(WebTool.DEFINITIONS)
                activeTools.addAll(FileSystemTool.DEFINITIONS)
                activeTools.add(CanvasTool.DEFINITION)
            }
            PromptMode.AGENT -> {
                activeTools.addAll(FileSystemTool.DEFINITIONS)
                activeTools.add(ShellTool.DEFINITION)
                if (enableWebSearch) activeTools.addAll(WebTool.DEFINITIONS)
                activeTools.addAll(AcademicTool.DEFINITIONS)
                activeTools.add(CanvasTool.DEFINITION)
            }
        }

        while (continueLoop && turnCount < maxTurns) {
            turnCount++
            val dbMessages = chatRepository.getMessagesForSessionSync(sessionId)
            val systemPrompt = AgentSystemPrompt.getPrompt(
                workspacePath = workspacePath,
                yoloMode = yoloMode,
                hasWebTools = enableWebSearch,
                executionMode = effectiveMode,
                antiAiSlopMode = antiAiSlopMode,
                academicMode = academicMode,
                promptMode = promptMode
            )
            val systemMessage = Message(
                sessionId = sessionId,
                role = MessageRole.SYSTEM,
                content = systemPrompt
            )
            val filteredDbMessages = dbMessages.filter { it.role != MessageRole.SYSTEM }
            val messages = mutableListOf<Message>()
            messages.add(systemMessage)

            // Auto-Reminder di Turn 8+ (Recency bias injection untuk menjaga disiplin)
            if (turnCount >= 8) {
                messages.add(
                    Message(
                        sessionId = sessionId,
                        role = MessageRole.SYSTEM,
                        content = "[SYSTEM REMINDER: Turn $turnCount/$maxTurns. Wajib patuhi Zero Lazy Placeholders, Surgical Patching (Pilar 8), State Anchor (Pilar 10), dan verifikasi Definition of Done (Pilar 12) sebelum mengakhiri tugas.]"
                    )
                )
            }

            if (turnCount == 1 && !workspacePath.isNullOrBlank()) {
                val lastUserContent = filteredDbMessages.lastOrNull { it.role == MessageRole.USER }?.content ?: ""
                val ragContext = id.gustyxganteng.agent.domain.context.LocalCodebaseRag.retrieve(workspacePath, lastUserContent)
                if (!ragContext.isNullOrBlank()) {
                    messages.add(
                        Message(
                            sessionId = sessionId,
                            role = MessageRole.SYSTEM,
                            content = ragContext
                        )
                    )
                }
            }

            filteredDbMessages.forEachIndexed { index, msg ->
                if (index == filteredDbMessages.lastIndex && msg.role == MessageRole.USER) {
                    messages.add(msg.copy(content = msg.content + THINK_ENFORCEMENT))
                } else {
                    messages.add(msg)
                }
            }

            val reasoningBuilder = StringBuilder()
            val contentBuilder = StringBuilder()
            val toolCallMap = mutableMapOf<Int, MutableToolCallBuilder>()
            var usage: TokenUsage? = null
            val thinkingStart = System.currentTimeMillis()
            var thinkingDuration: Long? = null
            var isCurrentlyThinking = true
            var typingStart: Long? = null

            val initialActivity = if (turnCount == 1) {
                "Menganalisis instruksi pengguna..."
            } else {
                "Mengevaluasi hasil tool & merencanakan langkah..."
            }

            _agentState.value = AgentState.Streaming(
                content = "",
                reasoning = null,
                thinkingElapsedMs = 0,
                isThinking = true,
                thinkingTokens = 0,
                currentActivity = initialActivity
            )

            var streamCompleted = false
            var streamAttempt = 0
            val maxStreamRetries = 3
            var currentBudgetTokens = budgetTokens
            var streamMessages = messages.toMutableList()

            while (!streamCompleted && streamAttempt <= maxStreamRetries) {
                try {
                    agnesApiService.streamChat(
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        model = model,
                        messages = streamMessages,
                        tools = if (activeTools.isNotEmpty()) activeTools else null,
                        budgetTokens = currentBudgetTokens,
                        maxTokens = maxTokens,
                        reasoningEffort = reasoningEffort
                    ).collect { event ->
                        when (event) {
                            is StreamEvent.ReasoningDelta -> {
                                reasoningBuilder.append(event.text)
                                val elapsed = System.currentTimeMillis() - thinkingStart
                                thinkingDuration = elapsed
                                val tokens = (reasoningBuilder.length / 4).coerceAtLeast(1)

                                val lower = reasoningBuilder.toString().lowercase()
                                val thinkingActivity = when {
                                    lower.contains("read_file") || lower.contains("membaca") || lower.contains("baca berkas") -> "Menganalisis & membaca berkas..."
                                    lower.contains("apply_diff") || lower.contains("write_file") || lower.contains("refactor") || lower.contains("modifikasi") -> "Merencanakan perubahan & refactoring..."
                                    lower.contains("execute_shell") || lower.contains("terminal") || lower.contains("perintah") -> "Mempersiapkan eksekusi terminal..."
                                    lower.contains("web_search") || lower.contains("mencari") || lower.contains("search") -> "Mempersiapkan pencarian referensi..."
                                    lower.contains("list_directory") || lower.contains("direktori") || lower.contains("struktur") -> "Memeriksa struktur proyek..."
                                    else -> "Sedang menalar & memikirkan solusi..."
                                }

                                _agentState.value = AgentState.Streaming(
                                    content = contentBuilder.toString(),
                                    reasoning = reasoningBuilder.toString(),
                                    thinkingElapsedMs = elapsed,
                                    isThinking = true,
                                    thinkingTokens = tokens,
                                    currentActivity = thinkingActivity
                                )
                            }
                            is StreamEvent.ContentDelta -> {
                                if (isCurrentlyThinking) {
                                    isCurrentlyThinking = false
                                    typingStart = System.currentTimeMillis()
                                    if (thinkingDuration == null && reasoningBuilder.isNotEmpty()) {
                                        thinkingDuration = System.currentTimeMillis() - thinkingStart
                                    }
                                }
                                val elapsedTypingMs = if (typingStart != null) System.currentTimeMillis() - typingStart!! else 0L
                                val typingSec = String.format(java.util.Locale.US, "%.1fs", elapsedTypingMs / 1000.0)
                                contentBuilder.append(event.text)
                                val tokens = if (reasoningBuilder.isNotEmpty()) {
                                    (reasoningBuilder.length / 4).coerceAtLeast(1)
                                } else 0
                                _agentState.value = AgentState.Streaming(
                                    content = contentBuilder.toString(),
                                    reasoning = reasoningBuilder.toString().ifEmpty { null },
                                    thinkingElapsedMs = thinkingDuration,
                                    isThinking = false,
                                    thinkingTokens = tokens,
                                    currentActivity = "Menulis respons ($typingSec)...",
                                    typingElapsedMs = elapsedTypingMs
                                )
                            }
                            is StreamEvent.ToolCallChunk -> {
                                if (isCurrentlyThinking) {
                                    isCurrentlyThinking = false
                                    typingStart = System.currentTimeMillis()
                                    if (thinkingDuration == null && reasoningBuilder.isNotEmpty()) {
                                        thinkingDuration = System.currentTimeMillis() - thinkingStart
                                    }
                                }
                                val elapsedTypingMs = if (typingStart != null) System.currentTimeMillis() - typingStart!! else 0L
                                val typingSec = String.format(java.util.Locale.US, "%.1fs", elapsedTypingMs / 1000.0)
                                val builder = toolCallMap.getOrPut(event.index) { MutableToolCallBuilder() }
                                event.id?.let { builder.id = it }
                                event.name?.let { builder.name = it }
                                builder.arguments.append(event.argumentsDelta)
                                _agentState.value = AgentState.Streaming(
                                    content = contentBuilder.toString(),
                                    reasoning = reasoningBuilder.toString().ifEmpty { null },
                                    thinkingElapsedMs = thinkingDuration,
                                    isThinking = false,
                                    thinkingTokens = if (reasoningBuilder.isNotEmpty()) (reasoningBuilder.length / 4).coerceAtLeast(1) else 0,
                                    currentActivity = "Mempersiapkan parameter tool ($typingSec)...",
                                    typingElapsedMs = elapsedTypingMs
                                )
                            }
                            is StreamEvent.UsageUpdate -> {
                                usage = event.usage
                            }
                            is StreamEvent.StreamCompleted -> {
                                streamCompleted = true
                            }
                            is StreamEvent.StreamError -> {
                                val err = event.error
                                val errMsg = event.message
                                val isTimeout = err is java.net.SocketTimeoutException ||
                                    err is java.io.InterruptedIOException ||
                                    err is java.net.ConnectException ||
                                    err is java.net.SocketException ||
                                    err.message?.contains("timeout", ignoreCase = true) == true ||
                                    errMsg.contains("timeout", ignoreCase = true) ||
                                    errMsg.contains("Batas waktu", ignoreCase = true) ||
                                    err.message?.contains("unexpected end of stream", ignoreCase = true) == true ||
                                    err.message?.contains("connection reset", ignoreCase = true) == true ||
                                    err.message?.contains("broken pipe", ignoreCase = true) == true

                                if (isTimeout) {
                                    throw StreamTimeoutException(errMsg, err)
                                } else {
                                    throw err
                                }
                            }
                            else -> {}
                        }
                    }
                    streamCompleted = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    val isTimeout = e is StreamTimeoutException ||
                        e is java.net.SocketTimeoutException ||
                        e is java.io.InterruptedIOException ||
                        e is java.net.ConnectException ||
                        e is java.net.SocketException ||
                        e.message?.contains("timeout", ignoreCase = true) == true ||
                        e.message?.contains("Batas waktu", ignoreCase = true) == true ||
                        e.message?.contains("unexpected end of stream", ignoreCase = true) == true ||
                        e.message?.contains("connection reset", ignoreCase = true) == true ||
                        e.message?.contains("broken pipe", ignoreCase = true) == true

                    if (isTimeout && streamAttempt < maxStreamRetries) {
                        streamAttempt++
                        val currentReasoning = reasoningBuilder.toString().trim()
                        val currentContent = contentBuilder.toString().trim()

                        _agentState.value = AgentState.Streaming(
                            content = contentBuilder.toString(),
                            reasoning = currentReasoning.ifEmpty { null },
                            thinkingElapsedMs = thinkingDuration ?: (System.currentTimeMillis() - thinkingStart),
                            isThinking = false,
                            thinkingTokens = if (currentReasoning.isNotEmpty()) (currentReasoning.length / 4).coerceAtLeast(1) else 0,
                            currentActivity = "Batas waktu tercapai. Otomatis melanjutkan respons ($streamAttempt/$maxStreamRetries)..."
                        )

                        streamMessages = messages.map { msg ->
                            if (msg.role == MessageRole.USER && msg.content.contains(THINK_ENFORCEMENT)) {
                                msg.copy(content = msg.content.replace(THINK_ENFORCEMENT, ""))
                            } else {
                                msg
                            }
                        }.toMutableList()

                        if (currentReasoning.isNotEmpty() && currentContent.isEmpty()) {
                            currentBudgetTokens = 0
                            toolCallMap.clear()
                            streamMessages.add(
                                Message(
                                    sessionId = sessionId,
                                    role = MessageRole.USER,
                                    content = "[System Directive: Koneksi sebelumnya timeout tepat setelah proses berpikir selesai. Analisis penalaran Anda:\n\n\"\"\"\n$currentReasoning\n\"\"\"\n\nJANGAN ulangi tag <think>. Langsung lanjutkan pemanggilan tool atau penulisan respons sekarang.]"
                                )
                            )
                        } else if (currentContent.isNotEmpty()) {
                            currentBudgetTokens = 0
                            streamMessages.add(
                                Message(
                                    sessionId = sessionId,
                                    role = MessageRole.USER,
                                    content = "[System Directive: Koneksi streaming terputus saat menulis respons. Bagian teks yang telah diterima:\n\n\"\"\"\n$currentContent\n\"\"\"\n\nLanjutkan teks berikutnya dari titik tersebut sampai tuntas.]"
                                )
                            )
                        }

                        delay(1500)
                    } else {
                        throw e
                    }
                }
            }

            val accumulatedToolCalls = toolCallMap.values.mapNotNull { it.build() }.map { tc ->
                val safeArgs = JsonRepairUtil.repairOrSanitize(tc.function.arguments)
                tc.copy(function = tc.function.copy(arguments = safeArgs))
            }
            val finalContent = contentBuilder.toString().trim()
            val finalReasoning = reasoningBuilder.toString().trim().ifEmpty { null }
            val computedThinkingTokens = if (!finalReasoning.isNullOrEmpty()) {
                (finalReasoning.length / 4).coerceAtLeast(1)
            } else 0

            val estimatedPromptTokens = messages.sumOf { (it.content.length / 4).coerceAtLeast(1) } +
                (systemPrompt.length / 4) +
                activeTools.sumOf { (it.function.description.length / 4) + 50 }

            // Fallback token calculation untuk reasoning dan total konteks
            val adjustedUsage = if (usage != null) {
                val rTokens = if (usage!!.reasoningTokens == 0 && computedThinkingTokens > 0) computedThinkingTokens else usage!!.reasoningTokens
                val pTokens = if (usage!!.promptTokens == 0) estimatedPromptTokens else usage!!.promptTokens
                val cTokens = if (usage!!.completionTokens == 0) ((finalContent.length / 4) + rTokens).coerceAtLeast(1) else usage!!.completionTokens
                val tTokens = if (usage!!.totalTokens == 0) pTokens + cTokens else usage!!.totalTokens
                usage!!.copy(
                    promptTokens = pTokens,
                    completionTokens = cTokens,
                    totalTokens = tTokens,
                    reasoningTokens = rTokens
                )
            } else {
                val completionTokens = ((finalContent.length / 4) + computedThinkingTokens).coerceAtLeast(1)
                TokenUsage(
                    promptTokens = estimatedPromptTokens,
                    completionTokens = completionTokens,
                    totalTokens = estimatedPromptTokens + completionTokens,
                    reasoningTokens = computedThinkingTokens
                )
            }

            // Simpan respon assistant
            val assistantMessage = Message(
                sessionId = sessionId,
                role = MessageRole.ASSISTANT,
                content = finalContent,
                reasoningContent = finalReasoning,
                thinkingDurationMs = thinkingDuration,
                toolCalls = accumulatedToolCalls,
                tokenUsage = adjustedUsage
            )
            chatRepository.saveMessage(assistantMessage)
            settingsRepository?.recordUsage(
                promptTokens = adjustedUsage.promptTokens.toLong(),
                completionTokens = adjustedUsage.completionTokens.toLong(),
                totalTokens = adjustedUsage.totalTokens.toLong()
            )

            // Jika tidak ada tool calls, loop selesai
            if (accumulatedToolCalls.isEmpty()) {
                // Jika model selesai setelah mengeksekusi tool tapi konten responnya kosong,
                // pancing model satu turn lagi untuk menuliskan ringkasan akhir
                if (turnCount > 1 && finalContent.isBlank()) {
                    val promptForSummary = Message(
                        sessionId = sessionId,
                        role = MessageRole.USER,
                        content = "[System Directive: Semua aksi tool telah selesai dieksekusi. Berikan kesimpulan dan ringkasan akhir yang terstruktur: sebutkan file yang dibuat atau di-refactor, baris kode yang dibuang dan ditambahkan, serta hasil eksekusi.]"
                    )
                    chatRepository.saveMessage(promptForSummary)
                    continue
                }
                continueLoop = false
                break
            }

            // Eksekusi tiap tool call
            for (toolCall in accumulatedToolCalls) {
                val toolName = toolCall.function.name
                val rawArgs = toolCall.function.arguments
                val parsedArgs = JsonRepairUtil.parseSafeObject(rawArgs)

                // Cek approval jika YOLO mode mati
                val requiresApproval = !yoloMode && (toolName == "write_file" || toolName == "apply_diff" || toolName == "apply_unified_patch" || toolName == "execute_shell")
                if (requiresApproval) {
                    val deferred = CompletableDeferred<Boolean>()
                    pendingApprovalDeferred = deferred

                    val details = when (toolName) {
                        "write_file" -> parsedArgs["content"]?.jsonPrimitive?.content ?: ""
                        "apply_diff" -> "${parsedArgs["search_block"]?.jsonPrimitive?.content}\n\n---> GANTI DENGAN --->\n\n${parsedArgs["replace_block"]?.jsonPrimitive?.content}"
                        "apply_unified_patch" -> parsedArgs["patch_content"]?.jsonPrimitive?.content ?: ""
                        "execute_shell" -> parsedArgs["command"]?.jsonPrimitive?.content ?: ""
                        else -> rawArgs
                    }
                    val targetPath = parsedArgs["path"]?.jsonPrimitive?.content

                    _agentState.value = AgentState.AwaitingApproval(
                        toolCall = toolCall,
                        toolName = toolName,
                        details = details,
                        isShell = toolName == "execute_shell",
                        targetPath = targetPath
                    )

                    val approved = deferred.await()
                    if (!approved) {
                        // User menolak aksi
                        val rejectedResult = ToolExecutionResult(
                            toolCallId = toolCall.id,
                            toolName = toolName,
                            stderr = "Operasi dibatalkan oleh pengguna (Approval Rejected).",
                            isSuccess = false
                        )
                        saveToolResult(sessionId, toolCall.id, rejectedResult)
                        continue
                    }
                }

                // Eksekusi tool dengan deskripsi aksi yang jelas
                val executionDesc = when (toolName) {
                    "write_file" -> {
                        val path = parsedArgs["path"]?.jsonPrimitive?.content ?: "file"
                        val lines = parsedArgs["content"]?.jsonPrimitive?.content?.lines()?.size ?: 0
                        "Menulis file $path (+$lines baris)"
                    }
                    "apply_diff" -> {
                        val path = parsedArgs["path"]?.jsonPrimitive?.content ?: "file"
                        val searchLines = parsedArgs["search_block"]?.jsonPrimitive?.content?.lines()?.size ?: 0
                        val replaceLines = parsedArgs["replace_block"]?.jsonPrimitive?.content?.lines()?.size ?: 0
                        "Refactor $path (-$searchLines / +$replaceLines baris)"
                    }
                    "execute_shell" -> {
                        val cmd = parsedArgs["command"]?.jsonPrimitive?.content ?: ""
                        "Terminal: $cmd"
                    }
                    "read_file" -> {
                        val path = parsedArgs["path"]?.jsonPrimitive?.content ?: "file"
                        val start = parsedArgs["start_line"]?.jsonPrimitive?.content
                        val end = parsedArgs["end_line"]?.jsonPrimitive?.content
                        if (start != null && end != null) "Membaca $path (baris $start-$end)" else "Membaca $path"
                    }
                    "list_directory" -> {
                        val path = parsedArgs["path"]?.jsonPrimitive?.content?.ifEmpty { "." } ?: "."
                        "Inspeksi direktori: $path"
                    }
                    "search_files" -> {
                        val query = parsedArgs["query"]?.jsonPrimitive?.content ?: ""
                        "Mencari kode: '$query'"
                    }
                    "search_code_symbols" -> {
                        val sym = parsedArgs["symbol"]?.jsonPrimitive?.content ?: ""
                        "Mencari simbol/hook: '$sym'"
                    }
                    "apply_unified_patch" -> {
                        val path = parsedArgs["path"]?.jsonPrimitive?.content ?: "file"
                        val hunks = parsedArgs["patch_content"]?.jsonPrimitive?.content?.lines()?.count { it.startsWith("@@") } ?: 0
                        "Patch file: $path ($hunks hunk)"
                    }
                    "web_search" -> {
                        val query = parsedArgs["query"]?.jsonPrimitive?.content ?: ""
                        "Mencari web: '$query'"
                    }
                    "wikipedia_search" -> {
                        val query = parsedArgs["query"]?.jsonPrimitive?.content ?: ""
                        val lang = parsedArgs["lang"]?.jsonPrimitive?.content ?: "id"
                        "Mencari Wikipedia ($lang): '$query'"
                    }
                    "read_webpage" -> {
                        val url = parsedArgs["url"]?.jsonPrimitive?.content ?: ""
                        "Membaca dokumentasi web: $url"
                    }
                    "search_academic_papers" -> {
                        val query = parsedArgs["query"]?.jsonPrimitive?.content ?: ""
                        "Mencari jurnal ilmiah: '$query'"
                    }
                    "verify_doi_or_citation" -> {
                        val doi = parsedArgs["doi"]?.jsonPrimitive?.content ?: ""
                        "Memverifikasi DOI: $doi"
                    }
                    "download_paper_pdf" -> {
                        val title = parsedArgs["title"]?.jsonPrimitive?.content ?: "paper"
                        "Mengunduh PDF jurnal: '$title'"
                    }
                    "open_canvas" -> {
                        val title = parsedArgs["title"]?.jsonPrimitive?.content ?: "dokumen"
                        "Membuka Canvas: $title"
                    }
                    "generate_image" -> {
                        val prompt = parsedArgs["prompt"]?.jsonPrimitive?.content?.take(36) ?: "gambar"
                        "Menghasilkan gambar ilustrasi AI: '$prompt'"
                    }
                    else -> "Menjalankan $toolName..."
                }

                _agentState.value = AgentState.ExecutingTool(toolName, executionDesc)
                val result = executeToolInternal(
                    toolCallId = toolCall.id,
                    toolName = toolName,
                    args = parsedArgs,
                    workspacePath = workspacePath,
                    useTermuxEnv = useTermuxEnv,
                    tavilyDeepSearchEnabled = tavilyDeepSearchEnabled,
                    claudeMaxMode = effectiveMode == AgentExecutionMode.MAX
                )
                saveToolResult(sessionId, toolCall.id, result)
            }
        }
    }

    private suspend fun executeToolInternal(
        toolCallId: String,
        toolName: String,
        args: JsonObject,
        workspacePath: String?,
        useTermuxEnv: Boolean,
        tavilyDeepSearchEnabled: Boolean,
        claudeMaxMode: Boolean = false
    ): ToolExecutionResult {
        val root = workspacePath ?: "/storage/emulated/0"
        return when (toolName) {
            "list_directory" -> fileSystemTool.listDirectory(toolCallId, root, args)
            "read_file" -> fileSystemTool.readFile(toolCallId, root, args)
            "write_file" -> fileSystemTool.writeFile(toolCallId, root, args)
            "apply_diff" -> fileSystemTool.applyDiff(toolCallId, root, args)
            "apply_unified_patch" -> fileSystemTool.applyUnifiedPatch(toolCallId, root, args)
            "search_files" -> fileSystemTool.searchFiles(toolCallId, root, args)
            "search_code_symbols" -> fileSystemTool.searchCodeSymbols(toolCallId, root, args)
            "execute_shell" -> shellTool.execute(toolCallId, root, args, useTermuxEnv)
            "web_search" -> webTool.search(
                toolCallId = toolCallId,
                args = args,
                useTavilyDeepSearch = tavilyDeepSearchEnabled || claudeMaxMode,
                isMaxMode = claudeMaxMode
            )
            "wikipedia_search" -> webTool.wikipediaSearch(toolCallId, args)
            "read_webpage" -> webTool.readWebpage(toolCallId, args)
            "search_academic_papers" -> academicTool.searchAcademicPapers(toolCallId, args)
            "verify_doi_or_citation" -> academicTool.verifyDoiOrCitation(toolCallId, args)
            "download_paper_pdf" -> academicTool.downloadPaperPdf(toolCallId, root, args)
            "open_canvas" -> canvasTool.execute(toolCallId, args)
            "generate_image" -> {
                val prompt = args["prompt"]?.jsonPrimitive?.content ?: ""
                val size = args["aspect_ratio"]?.jsonPrimitive?.content ?: "1024x1024"
                val res = agnesApiService.generateImage(
                    baseUrl = "",
                    apiKey = "",
                    prompt = prompt,
                    size = size
                )
                res.fold(
                    onSuccess = { url ->
                        ToolExecutionResult(
                            toolCallId = toolCallId,
                            toolName = toolName,
                            stdout = "![$prompt]($url)\n\n*Gambar berhasil digenerate oleh Agnes Image 2.5 Flash ($size)*\n[Buka Resolusi Penuh]($url)",
                            isSuccess = true
                        )
                    },
                    onFailure = { err ->
                        ToolExecutionResult(
                            toolCallId = toolCallId,
                            toolName = toolName,
                            stderr = "Gagal menghasilkan gambar: ${err.message}",
                            isSuccess = false
                        )
                    }
                )
            }
            else -> ToolExecutionResult(toolCallId, toolName, stderr = "Tool tidak dikenal: $toolName", isSuccess = false)
        }
    }

    private suspend fun saveToolResult(sessionId: String, toolCallId: String, result: ToolExecutionResult) {
        val rawContent = if (result.isSuccess) {
            val stdout = result.stdout.ifEmpty { "Berhasil dieksekusi (tanpa output)." }
            if (groqSidecarService != null && stdout.length > 1500) {
                groqSidecarService.summarizeLargeOutput(stdout, sourceHint = "Tool: ${result.toolName}")
            } else {
                stdout
            }
        } else {
            val stderr = result.stderr.ifEmpty { "Gagal dengan exit code ${result.exitCode}" }
            if (groqSidecarService != null && stderr.length > 800) {
                groqSidecarService.triageStacktrace(stderr)
            } else {
                "Error: $stderr"
            }
        }

        val content = if (rawContent.length > 12000) {
            rawContent.take(12000) + "\n\n[Output dipotong untuk menjaga efisiensi context window...]"
        } else {
            rawContent
        }

        val toolMsg = Message(
            sessionId = sessionId,
            role = MessageRole.TOOL,
            content = content,
            toolCallId = toolCallId
        )
        chatRepository.saveMessage(toolMsg)
    }

    private class MutableToolCallBuilder {
        var id: String = java.util.UUID.randomUUID().toString()
        var name: String = ""
        val arguments = StringBuilder()

        fun build(): ToolCall? {
            if (name.isEmpty()) return null
            return ToolCall(
                id = id,
                function = FunctionCall(
                    name = name,
                    arguments = arguments.toString()
                )
            )
        }
    }
}
