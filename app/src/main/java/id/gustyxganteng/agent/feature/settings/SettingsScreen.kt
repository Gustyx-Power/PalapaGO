package id.gustyxganteng.agent.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: AppSettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedModel by remember { mutableStateOf("agnes-3.0-flash") }
    var maxRetries by remember { mutableFloatStateOf(5f) }
    var yoloMode by remember { mutableStateOf(false) }
    var antiAiSlopMode by remember { mutableStateOf(true) }
    var tavilyDeepSearch by remember { mutableStateOf(false) }
    var claudeMaxMode by remember { mutableStateOf(true) }

    LaunchedEffect(settings) {
        settings?.let {
            selectedModel = if (it.selectedModel == "agnes-3.0-pro") "agnes-3.0-flash" else it.selectedModel
            maxRetries = it.maxRetries.toFloat()
            yoloMode = it.yoloMode
            antiAiSlopMode = it.antiAiSlopMode
            tavilyDeepSearch = it.tavilyDeepSearchEnabled
            claudeMaxMode = it.claudeMaxMode
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_close))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModelSelectionCard(
                selectedModel = selectedModel,
                onSelectModel = { selectedModel = it }
            )

            SecurityCard(
                yoloMode = yoloMode,
                onYoloModeChange = { yoloMode = it },
                maxRetries = maxRetries,
                onMaxRetriesChange = { maxRetries = it }
            )

            AntiAiSlopCard(
                antiAiSlopMode = antiAiSlopMode,
                onAntiAiSlopModeChange = { antiAiSlopMode = it }
            )

            WebSearchCard(
                tavilyDeepSearch = tavilyDeepSearch,
                onTavilyDeepSearchChange = { tavilyDeepSearch = it }
            )

            AgentModeCard(
                claudeMaxMode = claudeMaxMode,
                onClaudeMaxModeChange = { claudeMaxMode = it }
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    scope.launch {
                        settingsRepository.updateSelectedModel(selectedModel)
                        settingsRepository.updateMaxRetries(maxRetries.toInt())
                        settingsRepository.updateYoloMode(yoloMode)
                        settingsRepository.updateAntiAiSlopMode(antiAiSlopMode)
                        settingsRepository.updateTavilyDeepSearch(tavilyDeepSearch)
                        settingsRepository.updateClaudeMaxMode(claudeMaxMode)
                        Toast.makeText(context, context.getString(R.string.settings_saved_toast), Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_save_button))
            }
        }
    }
}

@Composable
private fun ModelSelectionCard(
    selectedModel: String,
    onSelectModel: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_model_header), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedModel == "agnes-3.0-flash",
                    onClick = { onSelectModel("agnes-3.0-flash") },
                    label = { Text("agnes-3.0-flash") }
                )
            }

            Text(
                stringResource(R.string.settings_model_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SecurityCard(
    yoloMode: Boolean,
    onYoloModeChange: (Boolean) -> Unit,
    maxRetries: Float,
    onMaxRetriesChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_security_header), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_yolo_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.settings_yolo_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = yoloMode,
                    onCheckedChange = onYoloModeChange
                )
            }

            Text(
                stringResource(R.string.settings_max_retries, maxRetries.toInt()),
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = maxRetries,
                onValueChange = onMaxRetriesChange,
                valueRange = 1f..10f,
                steps = 8
            )
        }
    }
}

@Composable
private fun AntiAiSlopCard(
    antiAiSlopMode: Boolean,
    onAntiAiSlopModeChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_anti_slop_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_anti_slop_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.settings_anti_slop_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = antiAiSlopMode,
                    onCheckedChange = onAntiAiSlopModeChange
                )
            }
        }
    }
}

@Composable
private fun WebSearchCard(
    tavilyDeepSearch: Boolean,
    onTavilyDeepSearchChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_search_header), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_tavily_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.settings_tavily_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = tavilyDeepSearch,
                    onCheckedChange = onTavilyDeepSearchChange
                )
            }
        }
    }
}

@Composable
private fun AgentModeCard(
    claudeMaxMode: Boolean,
    onClaudeMaxModeChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_agent_mode_header), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_claude_max_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.settings_claude_max_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = claudeMaxMode,
                    onCheckedChange = onClaudeMaxModeChange
                )
            }
        }
    }
}
