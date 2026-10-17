package id.gustyxganteng.agent.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gg_agent_settings")

data class AppSettings(
    val apiKey: String,
    val baseUrl: String,
    val selectedModel: String,
    val maxRetries: Int,
    val yoloMode: Boolean,
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
        val ACTIVE_WORKSPACE_PATH = stringPreferencesKey("active_workspace_path")
        val WEB_SEARCH_ENABLED = booleanPreferencesKey("web_search_enabled")
        val CANVAS_ENABLED = booleanPreferencesKey("canvas_enabled")
        val CLAUDE_MAX_MODE = booleanPreferencesKey("claude_max_mode")
        val ACTIVE_SESSION_ID = stringPreferencesKey("active_session_id")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            apiKey = preferences[PreferencesKeys.API_KEY] ?: "",
            baseUrl = preferences[PreferencesKeys.BASE_URL] ?: "https://apihub.agnes-ai.com/v1",
            selectedModel = preferences[PreferencesKeys.SELECTED_MODEL] ?: "agnes-3.0-flash",
            maxRetries = preferences[PreferencesKeys.MAX_RETRIES] ?: 5,
            yoloMode = preferences[PreferencesKeys.YOLO_MODE] ?: false,
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
}
