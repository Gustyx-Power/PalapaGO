package id.gustyxganteng.agent.feature.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.gustyxganteng.agent.core.database.AppSettings
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.database.ChatRepository
import id.gustyxganteng.agent.core.model.CanvasArtifact
import id.gustyxganteng.agent.core.model.ChatSession
import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.network.RateLimitStatus
import id.gustyxganteng.agent.core.network.SmartRetryInterceptor
import id.gustyxganteng.agent.core.service.AgentForegroundService
import id.gustyxganteng.agent.domain.agent.AgentOrchestrator
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val currentSession: ChatSession? = null,
    val messages: List<Message> = emptyList(),
    val agentState: AgentState = AgentState.Idle,
    val contextStatus: ContextMeterStatus? = null,
    val rateLimitStatus: RateLimitStatus? = null,
    val appSettings: AppSettings? = null,
    val activeCanvasArtifact: CanvasArtifact? = null,
    val recentWorkspaces: List<WorkspaceEntity> = emptyList()
)

class ChatViewModel(
    application: Application,
    private val chatRepository: ChatRepository,
    private val settingsRepository: AppSettingsRepository,
    private val workspaceDao: WorkspaceDao,
    private val orchestrator: AgentOrchestrator,
    private val contextMeterManager: ContextMeterManager,
    private val retryInterceptor: SmartRetryInterceptor
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
        orchestrator.agentState,
        _activeCanvas,
        _rateLimitStatus
    ) { sessionCtx, agentState, canvas, rateLimit ->
        val currentSession = sessionCtx.sessions.find { it.id == sessionCtx.currentId } ?: sessionCtx.sessions.firstOrNull()
        val totalTokens = sessionCtx.messages.mapNotNull { it.tokenUsage?.totalTokens }.maxOrNull() ?: 0
        val rawModel = currentSession?.model ?: sessionCtx.settings.selectedModel
        val model = if (rawModel == "agnes-3.0-pro") "agnes-3.0-flash" else rawModel
        val contextStatus = contextMeterManager.calculateStatus(totalTokens, model, sessionCtx.settings.claudeMaxMode)

        ChatUiState(
            sessions = sessionCtx.sessions,
            currentSession = currentSession,
            messages = sessionCtx.messages,
            agentState = agentState,
            contextStatus = contextStatus,
            rateLimitStatus = rateLimit,
            appSettings = sessionCtx.settings,
            activeCanvasArtifact = canvas,
            recentWorkspaces = sessionCtx.recentWorkspaces
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
                    val savedId = settings.activeSessionId
                    val matchedSession = sessions.find { it.id == savedId }
                    if (matchedSession != null) {
                        selectSession(matchedSession.id, persist = false)
                    } else if (sessions.isNotEmpty()) {
                        selectSession(sessions.first().id, persist = true)
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

        viewModelScope.launch {
            AgentForegroundService.start(getApplication(), getApplication<Application>().getString(R.string.service_notification_text))
            orchestrator.runTurn(
                scope = viewModelScope,
                sessionId = session.id,
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = if (session.model == "agnes-3.0-pro") "agnes-3.0-flash" else session.model,
                workspacePath = session.workspacePath ?: settings.activeWorkspacePath,
                yoloMode = settings.yoloMode,
                enableWebSearch = settings.webSearchEnabled,
                useTermuxEnv = true,
                claudeMaxMode = settings.claudeMaxMode
            )
        }
    }

    fun dismissError() {
        orchestrator.clearError()
    }

    fun sendMessage(content: String, images: List<String> = emptyList()) {
        val session = uiState.value.currentSession ?: return
        val settings = uiState.value.appSettings ?: return

        orchestrator.clearError()

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
                workspacePath = session.workspacePath ?: settings.activeWorkspacePath,
                yoloMode = settings.yoloMode,
                enableWebSearch = settings.webSearchEnabled,
                useTermuxEnv = true,
                claudeMaxMode = settings.claudeMaxMode
            )
        }
    }

    fun updateClaudeMaxMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateClaudeMaxMode(enabled)
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
}
