package id.gustyxganteng.agent.feature.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.gustyxganteng.agent.core.database.AppSettings
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.database.ChatRepository
import id.gustyxganteng.agent.core.model.AgentExecutionMode
import id.gustyxganteng.agent.core.model.CanvasArtifact
import id.gustyxganteng.agent.core.model.ChatSession
import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.model.PromptMode
import id.gustyxganteng.agent.core.network.AgnesApiService
import id.gustyxganteng.agent.core.network.RateLimitStatus
import id.gustyxganteng.agent.core.network.SmartRetryInterceptor
import id.gustyxganteng.agent.core.network.GroqSidecarService
import id.gustyxganteng.agent.core.service.AgentForegroundService
import id.gustyxganteng.agent.domain.agent.AgentOrchestrator
import id.gustyxganteng.agent.domain.tools.ImageTool
import id.gustyxganteng.agent.domain.agent.AgentState
import id.gustyxganteng.agent.domain.context.ContextMeterManager
import id.gustyxganteng.agent.domain.context.ContextMeterStatus
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.database.dao.WorkspaceDao
import id.gustyxganteng.agent.core.database.entity.WorkspaceEntity
import id.gustyxganteng.agent.domain.tools.WorkspacePathResolver
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

import id.gustyxganteng.agent.core.database.GlobalUsageStats

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val currentSession: ChatSession? = null,
    val messages: List<Message> = emptyList(),
    val agentState: AgentState = AgentState.Idle,
    val contextStatus: ContextMeterStatus? = null,
    val rateLimitStatus: RateLimitStatus? = null,
    val appSettings: AppSettings? = null,
    val activeCanvasArtifact: CanvasArtifact? = null,
    val recentWorkspaces: List<WorkspaceEntity> = emptyList(),
    val globalUsageStats: GlobalUsageStats = GlobalUsageStats()
)

class ChatViewModel(
    application: Application,
    private val chatRepository: ChatRepository,
    private val settingsRepository: AppSettingsRepository,
    private val workspaceDao: WorkspaceDao,
    private val orchestrator: AgentOrchestrator,
    private val contextMeterManager: ContextMeterManager,
    private val retryInterceptor: SmartRetryInterceptor,
    private val groqSidecarService: GroqSidecarService? = null,
    private val agnesApiService: AgnesApiService? = null
) : AndroidViewModel(application) {

    private val _currentSessionId = MutableStateFlow<String>("")
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val _activeCanvas = MutableStateFlow<CanvasArtifact?>(null)
    private val _rateLimitStatus = MutableStateFlow<RateLimitStatus?>(null)
    private var messageCollectorJob: Job? = null

    private data class SessionContext(
        val sessions: List<ChatSession>,
        val currentId: String,
        val messages: List<Message>,
        val settings: AppSettings,
        val recentWorkspaces: List<WorkspaceEntity>
    )

    val uiState: StateFlow<ChatUiState> = combine(
        combine(
            chatRepository.getAllSessions(),
            _currentSessionId,
            _messages,
            settingsRepository.settings,
            workspaceDao.getAllWorkspaces()
        ) { sessions, currentId, messages, settings, workspaces ->
            SessionContext(sessions, currentId, messages, settings, workspaces)
        },
        settingsRepository.globalUsageStats,
        orchestrator.agentState,
        _activeCanvas,
        _rateLimitStatus
    ) { sessionCtx, globalStats, agentState, canvas, rateLimit ->
        val currentSession = sessionCtx.sessions.find { it.id == sessionCtx.currentId } ?: sessionCtx.sessions.firstOrNull()
        val lastMessageUsage = sessionCtx.messages.lastOrNull { it.tokenUsage != null }?.tokenUsage
        val activeContextTokens = lastMessageUsage?.promptTokens ?: sessionCtx.messages.mapNotNull { it.tokenUsage?.totalTokens }.maxOrNull() ?: 0
        val rawModel = currentSession?.model ?: sessionCtx.settings.selectedModel
        val model = if (rawModel == "agnes-3.0-pro") "agnes-3.0-flash" else rawModel
        val contextStatus = contextMeterManager.calculateStatus(
            usedTokens = activeContextTokens,
            model = model,
            executionMode = sessionCtx.settings.executionMode,
            claudeMaxMode = sessionCtx.settings.claudeMaxMode,
            sessionMessages = sessionCtx.messages
        )

        ChatUiState(
            sessions = sessionCtx.sessions,
            currentSession = currentSession,
            messages = sessionCtx.messages,
            agentState = agentState,
            contextStatus = contextStatus,
            rateLimitStatus = rateLimit,
            appSettings = sessionCtx.settings,
            activeCanvasArtifact = canvas,
            recentWorkspaces = sessionCtx.recentWorkspaces,
            globalUsageStats = globalStats
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    init {
        orchestrator.onCanvasArtifactCreated = { artifact ->
            _activeCanvas.value = artifact
        }

        orchestrator.onTurnFinished = {
            AgentForegroundService.stop(getApplication())
        }

        viewModelScope.launch {
            retryInterceptor.rateLimitEvents.collect { status ->
                _rateLimitStatus.value = if (status.isThrottled) status else null
            }
        }

        viewModelScope.launch {
            combine(
                chatRepository.getAllSessions(),
                settingsRepository.settings
            ) { sessions, settings ->
                Pair(sessions, settings)
            }.collect { (sessions, settings) ->
                if (_currentSessionId.value.isEmpty()) {
                    // Cold start / App launch: Always open a fresh, clean chat session.
                    // If an existing session is already empty (0 messages), select it to avoid accumulating empty sessions.
                    val emptySession = sessions.firstOrNull { session ->
                        chatRepository.getMessagesForSessionSync(session.id).isEmpty()
                    }
                    if (emptySession != null) {
                        selectSession(emptySession.id, persist = true)
                    } else {
                        createNewSession()
                    }
                }
            }
        }
    }

    fun selectSession(sessionId: String, persist: Boolean = true) {
        if (_currentSessionId.value == sessionId && messageCollectorJob?.isActive == true) return
        _currentSessionId.value = sessionId
        messageCollectorJob?.cancel()
        messageCollectorJob = viewModelScope.launch {
            chatRepository.getMessagesForSession(sessionId).collect {
                _messages.value = it
            }
        }
        if (persist) {
            viewModelScope.launch {
                settingsRepository.updateActiveSessionId(sessionId)
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val currentId = _currentSessionId.value
            if (currentId.isNotEmpty()) {
                val currentMessages = chatRepository.getMessagesForSessionSync(currentId)
                if (currentMessages.isEmpty()) {
                    return@launch
                }
            }
            val settings = uiState.value.appSettings
            val newSession = ChatSession(
                id = UUID.randomUUID().toString(),
                title = getApplication<Application>().getString(R.string.default_session_title),
                model = if (settings?.selectedModel == "agnes-3.0-pro") "agnes-3.0-flash" else (settings?.selectedModel ?: "agnes-3.0-flash"),
                workspacePath = settings?.activeWorkspacePath
            )
            chatRepository.saveSession(newSession)
            selectSession(newSession.id, persist = true)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = ""
                settingsRepository.updateActiveSessionId(null)
            }
        }
    }

    fun retryLastTurn() {
        val session = uiState.value.currentSession ?: return
        val settings = uiState.value.appSettings ?: return
        orchestrator.clearError()

        val pMode = settings.promptMode
        val effectiveWorkspace = if (pMode == PromptMode.CHAT) null else (session.workspacePath ?: settings.activeWorkspacePath)
        val effectiveAcademic = pMode == PromptMode.ACADEMIC || settings.academicMode
        val effectiveWebSearch = pMode == PromptMode.ACADEMIC || settings.webSearchEnabled

        viewModelScope.launch {
            AgentForegroundService.start(getApplication(), getApplication<Application>().getString(R.string.service_notification_text))
            orchestrator.runTurn(
                scope = viewModelScope,
                sessionId = session.id,
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = if (session.model == "agnes-3.0-pro") "agnes-3.0-flash" else session.model,
                workspacePath = effectiveWorkspace,
                yoloMode = settings.yoloMode,
                enableWebSearch = effectiveWebSearch,
                useTermuxEnv = true,
                executionMode = settings.executionMode,
                antiAiSlopMode = settings.antiAiSlopMode,
                tavilyDeepSearchEnabled = settings.tavilyDeepSearchEnabled,
                academicMode = effectiveAcademic,
                promptMode = pMode,
                claudeMaxMode = settings.claudeMaxMode
            )
        }
    }

    fun dismissError() {
        orchestrator.clearError()
    }

    fun regenerateResponse(message: Message) {
        val session = uiState.value.currentSession ?: return
        val settings = uiState.value.appSettings ?: return
        orchestrator.clearError()

        val pMode = settings.promptMode
        val effectiveWorkspace = if (pMode == PromptMode.CHAT) null else (session.workspacePath ?: settings.activeWorkspacePath)
        val effectiveAcademic = pMode == PromptMode.ACADEMIC || settings.academicMode
        val effectiveWebSearch = pMode == PromptMode.ACADEMIC || settings.webSearchEnabled

        viewModelScope.launch {
            chatRepository.deleteMessagesSince(session.id, message.timestamp)
            AgentForegroundService.start(getApplication(), getApplication<Application>().getString(R.string.service_notification_text))
            orchestrator.runTurn(
                scope = viewModelScope,
                sessionId = session.id,
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = if (session.model == "agnes-3.0-pro") "agnes-3.0-flash" else session.model,
                workspacePath = effectiveWorkspace,
                yoloMode = settings.yoloMode,
                enableWebSearch = effectiveWebSearch,
                useTermuxEnv = true,
                executionMode = settings.executionMode,
                antiAiSlopMode = settings.antiAiSlopMode,
                tavilyDeepSearchEnabled = settings.tavilyDeepSearchEnabled,
                academicMode = effectiveAcademic,
                promptMode = pMode,
                claudeMaxMode = settings.claudeMaxMode
            )
        }
    }

    fun sendMessage(content: String, images: List<String> = emptyList()) {
        val session = uiState.value.currentSession ?: return
        val settings = uiState.value.appSettings ?: return

        orchestrator.clearError()

        val pMode = settings.promptMode
        val effectiveWorkspace = if (pMode == PromptMode.CHAT) null else (session.workspacePath ?: settings.activeWorkspacePath)
        val effectiveAcademic = pMode == PromptMode.ACADEMIC || settings.academicMode
        val effectiveWebSearch = pMode == PromptMode.ACADEMIC || settings.webSearchEnabled

        viewModelScope.launch {
            val userMsg = Message(
                sessionId = session.id,
                role = MessageRole.USER,
                content = content,
                imagesBase64 = images
            )
            chatRepository.saveMessage(userMsg)

            // Jalankan Foreground Service agar proses tidak di-kill OS
            AgentForegroundService.start(getApplication(), getApplication<Application>().getString(R.string.service_notification_text))

            orchestrator.runTurn(
                scope = viewModelScope,
                sessionId = session.id,
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = if (session.model == "agnes-3.0-pro") "agnes-3.0-flash" else session.model,
                workspacePath = effectiveWorkspace,
                yoloMode = settings.yoloMode,
                enableWebSearch = effectiveWebSearch,
                useTermuxEnv = true,
                executionMode = settings.executionMode,
                antiAiSlopMode = settings.antiAiSlopMode,
                tavilyDeepSearchEnabled = settings.tavilyDeepSearchEnabled,
                academicMode = effectiveAcademic,
                promptMode = pMode,
                claudeMaxMode = settings.claudeMaxMode
            )
        }
    }

    fun updateExecutionMode(mode: AgentExecutionMode) {
        viewModelScope.launch {
            settingsRepository.updateAgentExecutionMode(mode)
        }
    }

    fun updateClaudeMaxMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateClaudeMaxMode(enabled)
        }
    }

    fun updatePromptMode(mode: PromptMode) {
        viewModelScope.launch {
            settingsRepository.updatePromptMode(mode)
        }
    }

    fun stopStreaming() {
        orchestrator.cancel()
        retryInterceptor.cancelActiveRetries()
        _rateLimitStatus.value = null
        AgentForegroundService.stop(getApplication())
    }

    fun approveAction() {
        orchestrator.approvePendingAction()
    }

    fun rejectAction() {
        orchestrator.rejectPendingAction()
    }

    fun openCanvas(artifact: CanvasArtifact) {
        _activeCanvas.value = artifact
    }

    fun closeCanvas() {
        _activeCanvas.value = null
    }

    suspend fun generateImageDirect(prompt: String, ratio: String): Result<String> {
        val settings = settingsRepository.settings.first()
        val apiService = agnesApiService ?: return Result.failure(IllegalStateException("Layanan Agnes API tidak tersedia."))
        val dimension = ImageTool.normalizeDimension(ratio)
        return apiService.generateImage(
            baseUrl = settings.baseUrl,
            apiKey = settings.apiKey,
            prompt = prompt,
            size = dimension
        )
    }

    fun insertAssistantMessage(content: String) {
        val current = uiState.value.currentSession ?: return
        viewModelScope.launch {
            val assistantMessage = Message(
                id = UUID.randomUUID().toString(),
                sessionId = current.id,
                role = MessageRole.ASSISTANT,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.saveMessage(assistantMessage)
        }
    }

    fun updateSessionModel(model: String) {
        val current = uiState.value.currentSession ?: return
        viewModelScope.launch {
            chatRepository.saveSession(current.copy(model = model))
        }
    }

    fun updateActiveWorkspace(path: String) {
        val normalized = WorkspacePathResolver.normalizePath(path)
        viewModelScope.launch {
            settingsRepository.updateActiveWorkspace(normalized)
            val current = uiState.value.currentSession
            if (current != null) {
                chatRepository.saveSession(current.copy(workspacePath = normalized))
            }
            val wsName = File(normalized).name.ifEmpty { "Workspace" }
            workspaceDao.insertOrUpdate(
                WorkspaceEntity(
                    id = normalized,
                    name = wsName,
                    rootPath = normalized,
                    lastUsedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleWebSearch() {
        val current = uiState.value.appSettings?.webSearchEnabled ?: false
        viewModelScope.launch {
            settingsRepository.updateWebSearchEnabled(!current)
        }
    }

    fun toggleAcademicMode() {
        val current = uiState.value.appSettings?.academicMode ?: false
        viewModelScope.launch {
            settingsRepository.updateAcademicMode(!current)
        }
    }

    fun toggleYoloMode() {
        val current = uiState.value.appSettings?.yoloMode ?: false
        viewModelScope.launch {
            settingsRepository.updateYoloMode(!current)
        }
    }

    fun compactSession() {
        val session = uiState.value.currentSession ?: return
        val settings = uiState.value.appSettings ?: return
        viewModelScope.launch {
            contextMeterManager.compactSession(
                sessionId = session.id,
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = session.model
            )
        }
    }

    fun branchCurrentSession(onBranched: (newSessionId: String) -> Unit = {}) {
        val currentSession = uiState.value.currentSession ?: return
        val currentMessages = uiState.value.messages
        if (currentMessages.isEmpty()) return

        viewModelScope.launch {
            val branchSummary = if (groqSidecarService != null) {
                val snippet = currentMessages.takeLast(12).joinToString("\n---\n") { msg ->
                    val roleStr = when (msg.role) {
                        MessageRole.USER -> "USER"
                        MessageRole.ASSISTANT -> "AGENT"
                        MessageRole.TOOL -> "TOOL_RESULT"
                        MessageRole.SYSTEM -> "SYSTEM"
                    }
                    "[$roleStr]: ${msg.content.take(800)}"
                }
                groqSidecarService.generateBranchSummary(
                    sessionTitle = currentSession.title,
                    conversationSnippet = snippet
                )
            } else {
                buildString {
                    appendLine("### 🌿 Konteks Cabang Percakapan")
                    appendLine("*Dicabangkan dari sesi: **${currentSession.title}***\n")
                    appendLine("- **🎯 Sesi Induk**: ${currentSession.title}")
                    appendLine("- **⚡ Status**: Percakapan dicabangkan ke context window baru (0 token terpakai). Riwayat pengerjaan tetap tersimpan aman di sesi sebelumnya.")
                }
            }

            val newSessionId = UUID.randomUUID().toString()
            val branchPrefix = getApplication<Application>().getString(R.string.branch_session_prefix)
            val cleanTitle = currentSession.title.removePrefix(branchPrefix).trim()
            val newTitle = "$branchPrefix $cleanTitle"
            val newSession = ChatSession(
                id = newSessionId,
                title = newTitle,
                model = currentSession.model,
                workspacePath = currentSession.workspacePath,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            chatRepository.saveSession(newSession)

            val initialContextMessage = Message(
                id = UUID.randomUUID().toString(),
                sessionId = newSessionId,
                role = MessageRole.ASSISTANT,
                content = branchSummary,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.saveMessage(initialContextMessage)

            selectSession(newSessionId, persist = true)
            onBranched(newSessionId)
        }
    }
}
