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
    val activeCanvasArtifact: CanvasArtifact? = null
)

class ChatViewModel(
    application: Application,
    private val chatRepository: ChatRepository,
    private val settingsRepository: AppSettingsRepository,
    private val orchestrator: AgentOrchestrator,
    private val contextMeterManager: ContextMeterManager,
    private val retryInterceptor: SmartRetryInterceptor
) : AndroidViewModel(application) {

    private val _currentSessionId = MutableStateFlow<String>("")
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val _activeCanvas = MutableStateFlow<CanvasArtifact?>(null)
    private val _rateLimitStatus = MutableStateFlow<RateLimitStatus?>(null)

    private data class SessionContext(
        val sessions: List<ChatSession>,
        val currentId: String,
        val messages: List<Message>,
        val settings: AppSettings
    )

    val uiState: StateFlow<ChatUiState> = combine(
        combine(
            chatRepository.getAllSessions(),
            _currentSessionId,
            _messages,
            settingsRepository.settings
        ) { sessions, currentId, messages, settings ->
            SessionContext(sessions, currentId, messages, settings)
        },
        orchestrator.agentState,
        _activeCanvas,
        _rateLimitStatus
    ) { sessionCtx, agentState, canvas, rateLimit ->
        val currentSession = sessionCtx.sessions.find { it.id == sessionCtx.currentId } ?: sessionCtx.sessions.firstOrNull()
        val totalTokens = sessionCtx.messages.mapNotNull { it.tokenUsage?.totalTokens }.maxOrNull() ?: 0
        val model = currentSession?.model ?: sessionCtx.settings.selectedModel
        val contextStatus = contextMeterManager.calculateStatus(totalTokens, model)

        ChatUiState(
            sessions = sessionCtx.sessions,
            currentSession = currentSession,
            messages = sessionCtx.messages,
            agentState = agentState,
            contextStatus = contextStatus,
            rateLimitStatus = rateLimit,
            appSettings = sessionCtx.settings,
            activeCanvasArtifact = canvas
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    init {
        orchestrator.onCanvasArtifactCreated = { artifact ->
            _activeCanvas.value = artifact
        }

        viewModelScope.launch {
            retryInterceptor.rateLimitEvents.collect { status ->
                _rateLimitStatus.value = if (status.isThrottled) status else null
            }
        }

        viewModelScope.launch {
            chatRepository.getAllSessions().collect { list ->
                if (_currentSessionId.value.isEmpty()) {
                    if (list.isNotEmpty()) {
                        selectSession(list.first().id)
                    } else {
                        createNewSession()
                    }
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            chatRepository.getMessagesForSession(sessionId).collect {
                _messages.value = it
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val settings = uiState.value.appSettings
            val newSession = ChatSession(
                id = UUID.randomUUID().toString(),
                title = "Obrolan Baru",
                model = settings?.selectedModel ?: "agnes-3.0-flash",
                workspacePath = settings?.activeWorkspacePath
            )
            chatRepository.saveSession(newSession)
            selectSession(newSession.id)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = ""
            }
        }
    }

    fun sendMessage(content: String, images: List<String> = emptyList()) {
        val session = uiState.value.currentSession ?: return
        val settings = uiState.value.appSettings ?: return

        viewModelScope.launch {
            val userMsg = Message(
                sessionId = session.id,
                role = MessageRole.USER,
                content = content,
                imagesBase64 = images
            )
            chatRepository.saveMessage(userMsg)

            // Jalankan Foreground Service agar proses tidak di-kill OS
            AgentForegroundService.start(getApplication(), "GG Agent sedang mengeksekusi instruksi...")

            orchestrator.runTurn(
                scope = viewModelScope,
                sessionId = session.id,
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = session.model,
                workspacePath = session.workspacePath ?: settings.activeWorkspacePath,
                yoloMode = settings.yoloMode,
                enableWebSearch = settings.webSearchEnabled,
                useTermuxEnv = true
            )
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
        viewModelScope.launch {
            settingsRepository.updateActiveWorkspace(path)
            val current = uiState.value.currentSession ?: return@launch
            chatRepository.saveSession(current.copy(workspacePath = path))
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
