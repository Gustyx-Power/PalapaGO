package id.gustyxganteng.agent.domain.agent

import id.gustyxganteng.agent.core.database.ChatRepository
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
import id.gustyxganteng.agent.domain.tools.CanvasTool
import id.gustyxganteng.agent.domain.tools.FileSystemTool
import id.gustyxganteng.agent.domain.tools.ShellTool
import id.gustyxganteng.agent.domain.tools.WebTool
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface AgentState {
    data object Idle : AgentState
    data class Streaming(val content: String, val reasoning: String?, val thinkingElapsedMs: Long?) : AgentState
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
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private var activeJob: Job? = null
    private var pendingApprovalDeferred: CompletableDeferred<Boolean>? = null

    var onCanvasArtifactCreated: ((CanvasArtifact) -> Unit)? = null

    private val canvasTool = CanvasTool { artifact ->
        onCanvasArtifactCreated?.invoke(artifact)
    }

    fun approvePendingAction() {
        pendingApprovalDeferred?.complete(true)
    }

    fun rejectPendingAction() {
        pendingApprovalDeferred?.complete(false)
    }

    fun cancel() {
        activeJob?.cancel()
        pendingApprovalDeferred?.complete(false)
        _agentState.value = AgentState.Idle
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
        useTermuxEnv: Boolean
    ) {
        activeJob?.cancel()
        activeJob = scope.launch(Dispatchers.IO) {
            try {
                executeTurnLoop(
                    sessionId = sessionId,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    model = model,
                    workspacePath = workspacePath,
                    yoloMode = yoloMode,
                    enableWebSearch = enableWebSearch,
                    useTermuxEnv = useTermuxEnv
                )
            } catch (e: Exception) {
                _agentState.value = AgentState.Error(e.localizedMessage ?: "Unknown Agent error")
            } finally {
                if (_agentState.value !is AgentState.Error) {
                    _agentState.value = AgentState.Idle
                }
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
        useTermuxEnv: Boolean
    ) {
        var continueLoop = true
        var turnCount = 0
        val maxTurns = 15

        // Kumpulkan definisi tool yang aktif
        val activeTools = mutableListOf<ToolDefinition>()
        if (workspacePath != null) {
            activeTools.addAll(FileSystemTool.DEFINITIONS)
            activeTools.add(ShellTool.DEFINITION)
        }
        if (enableWebSearch) {
            activeTools.addAll(WebTool.DEFINITIONS)
        }
        activeTools.add(CanvasTool.DEFINITION)

        while (continueLoop && turnCount < maxTurns) {
            turnCount++
            val messages = chatRepository.getMessagesForSessionSync(sessionId)

            val reasoningBuilder = StringBuilder()
            val contentBuilder = StringBuilder()
            val toolCallMap = mutableMapOf<Int, MutableToolCallBuilder>()
            var usage: TokenUsage? = null
            var thinkingStart = System.currentTimeMillis()
            var thinkingDuration: Long? = null

            _agentState.value = AgentState.Streaming(content = "", reasoning = "", thinkingElapsedMs = 0)

            agnesApiService.streamChat(
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                messages = messages,
                tools = if (activeTools.isNotEmpty()) activeTools else null
            ).collect { event ->
                when (event) {
                    is StreamEvent.ReasoningDelta -> {
                        reasoningBuilder.append(event.text)
                        val elapsed = System.currentTimeMillis() - thinkingStart
                        thinkingDuration = elapsed
                        _agentState.value = AgentState.Streaming(
                            content = contentBuilder.toString(),
                            reasoning = reasoningBuilder.toString(),
                            thinkingElapsedMs = elapsed
                        )
                    }
                    is StreamEvent.ContentDelta -> {
                        contentBuilder.append(event.text)
                        _agentState.value = AgentState.Streaming(
                            content = contentBuilder.toString(),
                            reasoning = reasoningBuilder.toString().ifEmpty { null },
                            thinkingElapsedMs = thinkingDuration
                        )
                    }
                    is StreamEvent.ToolCallChunk -> {
                        val builder = toolCallMap.getOrPut(event.index) { MutableToolCallBuilder() }
                        event.id?.let { builder.id = it }
                        event.name?.let { builder.name = it }
                        builder.arguments.append(event.argumentsDelta)
                    }
                    is StreamEvent.UsageUpdate -> {
                        usage = event.usage
                    }
                    is StreamEvent.StreamCompleted -> {}
                    is StreamEvent.StreamError -> throw event.error
                    else -> {}
                }
            }

            val accumulatedToolCalls = toolCallMap.values.mapNotNull { it.build() }
            val finalContent = contentBuilder.toString().trim()
            val finalReasoning = reasoningBuilder.toString().trim().ifEmpty { null }

            // Simpan respon assistant
            val assistantMessage = Message(
                sessionId = sessionId,
                role = MessageRole.ASSISTANT,
                content = finalContent,
                reasoningContent = finalReasoning,
                thinkingDurationMs = thinkingDuration,
                toolCalls = accumulatedToolCalls,
                tokenUsage = usage
            )
            chatRepository.saveMessage(assistantMessage)

            // Jika tidak ada tool calls, loop selesai
            if (accumulatedToolCalls.isEmpty()) {
                continueLoop = false
                break
            }

            // Eksekusi tiap tool call
            for (toolCall in accumulatedToolCalls) {
                val toolName = toolCall.function.name
                val rawArgs = toolCall.function.arguments
                val parsedArgs = try {
                    json.parseToJsonElement(rawArgs).jsonObject
                } catch (_: Exception) {
                    JsonObject(emptyMap())
                }

                // Cek approval jika YOLO mode mati
                val requiresApproval = !yoloMode && (toolName == "write_file" || toolName == "apply_diff" || toolName == "execute_shell")
                if (requiresApproval) {
                    val deferred = CompletableDeferred<Boolean>()
                    pendingApprovalDeferred = deferred

                    val details = when (toolName) {
                        "write_file" -> parsedArgs["content"]?.jsonPrimitive?.content ?: ""
                        "apply_diff" -> "${parsedArgs["search_block"]?.jsonPrimitive?.content}\n\n---> GANTI DENGAN --->\n\n${parsedArgs["replace_block"]?.jsonPrimitive?.content}"
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

                // Eksekusi tool
                _agentState.value = AgentState.ExecutingTool(toolName, "Menjalankan $toolName...")
                val result = executeToolInternal(toolCall.id, toolName, parsedArgs, workspacePath, useTermuxEnv)
                saveToolResult(sessionId, toolCall.id, result)
            }
        }
    }

    private suspend fun executeToolInternal(
        toolCallId: String,
        toolName: String,
        args: JsonObject,
        workspacePath: String?,
        useTermuxEnv: Boolean
    ): ToolExecutionResult {
        val root = workspacePath ?: "/storage/emulated/0"
        return when (toolName) {
            "list_directory" -> fileSystemTool.listDirectory(toolCallId, root, args)
            "read_file" -> fileSystemTool.readFile(toolCallId, root, args)
            "write_file" -> fileSystemTool.writeFile(toolCallId, root, args)
            "apply_diff" -> fileSystemTool.applyDiff(toolCallId, root, args)
            "search_files" -> fileSystemTool.searchFiles(toolCallId, root, args)
            "execute_shell" -> shellTool.execute(toolCallId, root, args, useTermuxEnv)
            "web_search" -> webTool.search(toolCallId, args)
            "read_webpage" -> webTool.readWebpage(toolCallId, args)
            "open_canvas" -> canvasTool.execute(toolCallId, args)
            else -> ToolExecutionResult(toolCallId, toolName, stderr = "Tool tidak dikenal: $toolName", isSuccess = false)
        }
    }

    private suspend fun saveToolResult(sessionId: String, toolCallId: String, result: ToolExecutionResult) {
        val content = if (result.isSuccess) {
            result.stdout.ifEmpty { "Berhasil dieksekusi (tanpa output)." }
        } else {
            "Error: ${result.stderr.ifEmpty { "Gagal dengan exit code ${result.exitCode}" }}"
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
