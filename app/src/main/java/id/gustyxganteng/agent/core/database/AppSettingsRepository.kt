package id.gustyxganteng.agent.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.gustyxganteng.agent.core.network.KeyVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gg_agent_settings")

data class GlobalUsageStats(
    val totalRequests: Long = 0L,
    val promptTokens: Long = 0L,
    val completionTokens: Long = 0L,
    val totalTokens: Long = 0L
)

data class AppSettings(
    val apiKey: String,
    val baseUrl: String,
    val selectedModel: String,
    val maxRetries: Int,
    val yoloMode: Boolean,
    val antiAiSlopMode: Boolean = true,
    val tavilyDeepSearchEnabled: Boolean = false,
    val academicMode: Boolean = false,
    val activeWorkspacePath: String?,
    val webSearchEnabled: Boolean,
    val canvasEnabled: Boolean,
    val claudeMaxMode: Boolean = true,
    val activeSessionId: String? = null
)

class AppSettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val MAX_RETRIES = intPreferencesKey("max_retries")
        val YOLO_MODE = booleanPreferencesKey("yolo_mode")
        val ANTI_AI_SLOP_MODE = booleanPreferencesKey("anti_ai_slop_mode")
        val TAVILY_DEEP_SEARCH = booleanPreferencesKey("tavily_deep_search")
        val ACADEMIC_MODE = booleanPreferencesKey("academic_mode")
        val ACTIVE_WORKSPACE_PATH = stringPreferencesKey("active_workspace_path")
        val WEB_SEARCH_ENABLED = booleanPreferencesKey("web_search_enabled")
        val CANVAS_ENABLED = booleanPreferencesKey("canvas_enabled")
        val CLAUDE_MAX_MODE = booleanPreferencesKey("claude_max_mode")
        val ACTIVE_SESSION_ID = stringPreferencesKey("active_session_id")
        val GLOBAL_TOTAL_REQUESTS = longPreferencesKey("global_total_requests")
        val GLOBAL_PROMPT_TOKENS = longPreferencesKey("global_prompt_tokens")
        val GLOBAL_COMPLETION_TOKENS = longPreferencesKey("global_completion_tokens")
        val GLOBAL_TOTAL_TOKENS = longPreferencesKey("global_total_tokens")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val embeddedKey = KeyVault.getAgnesApiKey()
        val storedKey = preferences[PreferencesKeys.API_KEY] ?: ""
        val effectiveKey = embeddedKey.ifBlank { storedKey }

        val embeddedBaseUrl = KeyVault.getAgnesBaseUrl()
        val storedBaseUrl = preferences[PreferencesKeys.BASE_URL] ?: ""
        val effectiveBaseUrl = embeddedBaseUrl.ifBlank { storedBaseUrl.ifBlank { "https://apihub.agnes-ai.com/v1" } }

        AppSettings(
            apiKey = effectiveKey,
            baseUrl = effectiveBaseUrl,
            selectedModel = preferences[PreferencesKeys.SELECTED_MODEL] ?: "agnes-3.0-flash",
            maxRetries = preferences[PreferencesKeys.MAX_RETRIES] ?: 5,
            yoloMode = preferences[PreferencesKeys.YOLO_MODE] ?: false,
            antiAiSlopMode = preferences[PreferencesKeys.ANTI_AI_SLOP_MODE] ?: true,
            tavilyDeepSearchEnabled = preferences[PreferencesKeys.TAVILY_DEEP_SEARCH] ?: false,
            academicMode = preferences[PreferencesKeys.ACADEMIC_MODE] ?: false,
            activeWorkspacePath = preferences[PreferencesKeys.ACTIVE_WORKSPACE_PATH],
            webSearchEnabled = preferences[PreferencesKeys.WEB_SEARCH_ENABLED] ?: true,
            canvasEnabled = preferences[PreferencesKeys.CANVAS_ENABLED] ?: true,
            claudeMaxMode = preferences[PreferencesKeys.CLAUDE_MAX_MODE] ?: true,
            activeSessionId = preferences[PreferencesKeys.ACTIVE_SESSION_ID]
        )
    }

    suspend fun updateApiKey(apiKey: String) {
        context.dataStore.edit { it[PreferencesKeys.API_KEY] = apiKey.trim() }
    }

    suspend fun updateBaseUrl(baseUrl: String) {
        context.dataStore.edit { it[PreferencesKeys.BASE_URL] = baseUrl.trim() }
    }

    suspend fun updateSelectedModel(model: String) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_MODEL] = model }
    }

    suspend fun updateMaxRetries(retries: Int) {
        context.dataStore.edit { it[PreferencesKeys.MAX_RETRIES] = retries.coerceIn(1, 10) }
    }

    suspend fun updateYoloMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.YOLO_MODE] = enabled }
    }

    suspend fun updateAntiAiSlopMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ANTI_AI_SLOP_MODE] = enabled }
    }

    suspend fun updateTavilyDeepSearch(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TAVILY_DEEP_SEARCH] = enabled }
    }

    suspend fun updateAcademicMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ACADEMIC_MODE] = enabled }
    }

    suspend fun updateActiveWorkspace(path: String?) {
        context.dataStore.edit {
            if (path == null) {
                it.remove(PreferencesKeys.ACTIVE_WORKSPACE_PATH)
            } else {
                it[PreferencesKeys.ACTIVE_WORKSPACE_PATH] = path
            }
        }
    }

    suspend fun updateWebSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.WEB_SEARCH_ENABLED] = enabled }
    }

    suspend fun updateCanvasEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CANVAS_ENABLED] = enabled }
    }

    suspend fun updateClaudeMaxMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CLAUDE_MAX_MODE] = enabled }
    }

    suspend fun updateActiveSessionId(sessionId: String?) {
        context.dataStore.edit {
            if (sessionId.isNullOrBlank()) {
                it.remove(PreferencesKeys.ACTIVE_SESSION_ID)
            } else {
                it[PreferencesKeys.ACTIVE_SESSION_ID] = sessionId
            }
        }
    }

    val globalUsageStats: Flow<GlobalUsageStats> = context.dataStore.data.map { preferences ->
        GlobalUsageStats(
            totalRequests = preferences[PreferencesKeys.GLOBAL_TOTAL_REQUESTS] ?: 0L,
            promptTokens = preferences[PreferencesKeys.GLOBAL_PROMPT_TOKENS] ?: 0L,
            completionTokens = preferences[PreferencesKeys.GLOBAL_COMPLETION_TOKENS] ?: 0L,
            totalTokens = preferences[PreferencesKeys.GLOBAL_TOTAL_TOKENS] ?: 0L
        )
    }

    suspend fun recordUsage(promptTokens: Long, completionTokens: Long, totalTokens: Long) {
        context.dataStore.edit { preferences ->
            val currentReq = preferences[PreferencesKeys.GLOBAL_TOTAL_REQUESTS] ?: 0L
            val currentPrompt = preferences[PreferencesKeys.GLOBAL_PROMPT_TOKENS] ?: 0L
            val currentComp = preferences[PreferencesKeys.GLOBAL_COMPLETION_TOKENS] ?: 0L
            val currentTotal = preferences[PreferencesKeys.GLOBAL_TOTAL_TOKENS] ?: 0L

            preferences[PreferencesKeys.GLOBAL_TOTAL_REQUESTS] = currentReq + 1
            preferences[PreferencesKeys.GLOBAL_PROMPT_TOKENS] = currentPrompt + promptTokens.coerceAtLeast(0L)
            preferences[PreferencesKeys.GLOBAL_COMPLETION_TOKENS] = currentComp + completionTokens.coerceAtLeast(0L)
            preferences[PreferencesKeys.GLOBAL_TOTAL_TOKENS] = currentTotal + totalTokens.coerceAtLeast(0L)
        }
    }
}
