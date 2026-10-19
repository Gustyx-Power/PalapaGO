package id.gustyxganteng.agent.feature.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.designsystem.M3OnPrimary
import id.gustyxganteng.agent.core.designsystem.M3OnPrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3Surface
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
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

    LaunchedEffect(settings) {
        settings?.let {
            selectedModel = if (it.selectedModel == "agnes-3.0-pro") "agnes-3.0-flash" else it.selectedModel
            maxRetries = it.maxRetries.toFloat()
            yoloMode = it.yoloMode
            antiAiSlopMode = it.antiAiSlopMode
            tavilyDeepSearch = it.tavilyDeepSearchEnabled
        }
    }

    Scaffold(
        containerColor = M3Surface,
        topBar = {
            Surface(
                color = M3Surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = M3SurfaceContainerHigh,
                            border = BorderStroke(1.dp, M3OutlineVariant),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_close),
                                    tint = M3TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = M3TextPrimary
                        )
                    }
                    HorizontalDivider(color = M3OutlineVariant)
                }
            }
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

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        settingsRepository.updateSelectedModel(selectedModel)
                        settingsRepository.updateMaxRetries(maxRetries.toInt())
                        settingsRepository.updateYoloMode(yoloMode)
                        settingsRepository.updateAntiAiSlopMode(antiAiSlopMode)
                        settingsRepository.updateTavilyDeepSearch(tavilyDeepSearch)
                        Toast.makeText(context, context.getString(R.string.settings_saved_toast), Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = M3Primary,
                    contentColor = M3OnPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_save_button), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCardHeader(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = M3PrimaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.35f)),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = M3Primary, modifier = Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = M3TextPrimary
        )
    }
}

@Composable
private fun ModelSelectionCard(
    selectedModel: String,
    onSelectModel: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainer),
        border = BorderStroke(1.dp, M3OutlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsCardHeader(Icons.Default.Speed, stringResource(R.string.settings_model_header))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedModel == "agnes-3.0-flash",
                    onClick = { onSelectModel("agnes-3.0-flash") },
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("Agnes AI Core (Utama)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = M3PrimaryContainer,
                        selectedLabelColor = M3OnPrimaryContainer
                    ),
                    border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.5f))
                )
                FilterChip(
                    selected = selectedModel == "kimi-k2.8-preview",
                    onClick = { onSelectModel("kimi-k2.8-preview") },
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("Kimi K2.8 (Thinking 40 RPM)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = M3PrimaryContainer,
                        selectedLabelColor = M3OnPrimaryContainer
                    ),
                    border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.5f))
                )
                FilterChip(
                    selected = selectedModel == "deepseek-v4.1-flash",
                    onClick = { onSelectModel("deepseek-v4.1-flash") },
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("DeepSeek v4.1 Flash (12 RPM)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = M3PrimaryContainer,
                        selectedLabelColor = M3OnPrimaryContainer
                    ),
                    border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.5f))
                )
            }

            Text(
                stringResource(R.string.settings_model_desc),
                style = MaterialTheme.typography.bodySmall,
                color = M3TextSecondary
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainer),
        border = BorderStroke(1.dp, M3OutlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsCardHeader(Icons.Default.Security, stringResource(R.string.settings_security_header))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_yolo_title), fontWeight = FontWeight.SemiBold, color = M3TextPrimary)
                    Text(
                        stringResource(R.string.settings_yolo_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = M3TextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = yoloMode,
                    onCheckedChange = onYoloModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = M3Primary,
                        checkedTrackColor = M3PrimaryContainer
                    )
                )
            }

            HorizontalDivider(color = M3OutlineVariant)

            Text(
                stringResource(R.string.settings_max_retries, maxRetries.toInt()),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = M3TextPrimary
            )
            Slider(
                value = maxRetries,
                onValueChange = onMaxRetriesChange,
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = M3Primary,
                    activeTrackColor = M3Primary
                )
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainer),
        border = BorderStroke(1.dp, M3OutlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsCardHeader(Icons.Default.Shield, stringResource(R.string.settings_anti_slop_title))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_anti_slop_title), fontWeight = FontWeight.SemiBold, color = M3TextPrimary)
                    Text(
                        stringResource(R.string.settings_anti_slop_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = M3TextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = antiAiSlopMode,
                    onCheckedChange = onAntiAiSlopModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = M3Primary,
                        checkedTrackColor = M3PrimaryContainer
                    )
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainer),
        border = BorderStroke(1.dp, M3OutlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsCardHeader(Icons.Default.Search, stringResource(R.string.settings_search_header))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_tavily_title), fontWeight = FontWeight.SemiBold, color = M3TextPrimary)
                    Text(
                        stringResource(R.string.settings_tavily_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = M3TextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = tavilyDeepSearch,
                    onCheckedChange = onTavilyDeepSearchChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = M3Primary,
                        checkedTrackColor = M3PrimaryContainer
                    )
                )
            }
        }
    }
}

