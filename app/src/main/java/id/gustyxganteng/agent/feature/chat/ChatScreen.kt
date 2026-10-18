package id.gustyxganteng.agent.feature.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiSparkleIcon
import id.gustyxganteng.agent.core.designsystem.M3Error
import id.gustyxganteng.agent.core.designsystem.M3ErrorContainer
import id.gustyxganteng.agent.core.designsystem.M3ExpressiveUserBubbleShape
import id.gustyxganteng.agent.core.designsystem.M3OnError
import id.gustyxganteng.agent.core.designsystem.M3OnPrimary
import id.gustyxganteng.agent.core.designsystem.M3OnPrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3Surface
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHighest
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLow
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.core.markdown.MarkdownContent
import id.gustyxganteng.agent.core.model.CanvasArtifact
import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.model.TokenUsage
import id.gustyxganteng.agent.core.util.ImageCompressor
import id.gustyxganteng.agent.domain.agent.AgentState
import id.gustyxganteng.agent.domain.tools.WorkspacePathResolver
import id.gustyxganteng.agent.feature.canvas.CanvasModalSheet
import id.gustyxganteng.agent.feature.chat.ui.components.CanvasPreviewCard
import id.gustyxganteng.agent.feature.chat.ui.components.GeminiBottomCapsule
import id.gustyxganteng.agent.feature.chat.ui.components.GeminiTopBar
import id.gustyxganteng.agent.feature.chat.ui.components.NeuralSpiderStatsCard
import id.gustyxganteng.agent.feature.chat.ui.components.ThinkingAccordion
import id.gustyxganteng.agent.feature.chat.ui.components.ToolExecutionCard
import id.gustyxganteng.agent.feature.workspace.DiffApprovalDialog
import id.gustyxganteng.agent.feature.workspace.ShellApprovalDialog
import id.gustyxganteng.agent.feature.workspace.WorkspaceManagerBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val density = LocalDensity.current
    var bottomBarHeightDp by remember { mutableStateOf(88.dp) }

    var inputText by remember { mutableStateOf("") }
    val attachedImages = remember { mutableStateListOf<String>() }
    var showWorkspaceManager by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        scope.launch {
            for (uri in uris) {
                try {
                    val base64 = ImageCompressor.compressUriToBase64(context, uri)
                    attachedImages.add(base64)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.image_compress_failed, e.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val uri = tempCameraUri
            if (uri != null) {
                scope.launch {
                    try {
                        val base64 = ImageCompressor.compressUriToBase64(context, uri)
                        attachedImages.add(base64)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.image_compress_failed, e.message ?: ""),
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        tempCameraFile?.delete()
                        tempCameraFile = null
                        tempCameraUri = null
                    }
                }
            }
        } else {
            tempCameraFile?.delete()
            tempCameraFile = null
            tempCameraUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                val file = File.createTempFile("camera_capture_", ".jpg", context.cacheDir)
                tempCameraFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Failed to open camera", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    val launchCamera: () -> Unit = {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            try {
                val file = File.createTempFile("camera_capture_", ".jpg", context.cacheDir)
                tempCameraFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Failed to open camera", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val streamingLength = (uiState.agentState as? AgentState.Streaming)?.content?.length ?: 0
    val reasoningLength = (uiState.agentState as? AgentState.Streaming)?.reasoning?.length ?: 0
    LaunchedEffect(uiState.agentState, streamingLength, reasoningLength) {
        when (uiState.agentState) {
            is AgentState.Streaming,
            is AgentState.ExecutingTool,
            is AgentState.Error -> {
                listState.scrollToItem(uiState.messages.size)
            }
            else -> {}
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp),
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                drawerContainerColor = M3SurfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Brand Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = M3PrimaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.35f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                GeminiSparkleIcon(size = 20.dp, tint = M3Primary)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = M3TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = M3TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // New Chat Button (M3 Expressive Filled Tonal)
                    Button(
                        onClick = {
                            viewModel.createNewSession()
                            scope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = M3PrimaryContainer,
                            contentColor = M3OnPrimaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = M3OnPrimaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.new_chat), fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.chat_history),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = M3Primary
                    )

                    // Sessions List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(uiState.sessions) { session ->
                            val isSelected = session.id == uiState.currentSession?.id
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = session.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isSelected) M3Primary else M3TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    scope.launch { drawerState.close() }
                                },
                                badge = {
                                    IconButton(
                                        onClick = { viewModel.deleteSession(session.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = M3TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = M3SurfaceContainerHighest,
                                    unselectedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = M3OutlineVariant)
                    Spacer(Modifier.height(10.dp))

                    // Permanent Telemetry Card
                    NeuralSpiderStatsCard(
                        stats = uiState.globalUsageStats,
                        isDrawerOpen = drawerState.isOpen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // Settings Navigation Item
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null, tint = M3Primary) },
                        label = { Text(stringResource(R.string.settings_title), color = M3TextPrimary, fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = M3SurfaceContainerHigh),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = M3Surface,
            topBar = {
                GeminiTopBar(
                    selectedModel = uiState.currentSession?.model ?: uiState.appSettings?.selectedModel ?: "agnes-3.0-flash",
                    claudeMaxMode = uiState.appSettings?.claudeMaxMode ?: true,
                    contextStatus = uiState.contextStatus,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onSelectModel = { viewModel.updateSessionModel(it) },
                    onToggleClaudeMaxMode = { viewModel.updateClaudeMaxMode(it) },
                    onCompactContext = {
                        viewModel.compactSession()
                        Toast.makeText(context, context.getString(R.string.compacting_context), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                // Messages / Content List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = bottomBarHeightDp + 20.dp
                    )
                ) {
                    // Expressive Welcome Hero when chat is fresh/empty
                    if (uiState.messages.isEmpty() && uiState.agentState is AgentState.Idle) {
                        item {
                            WelcomeHeroSection(
                                onSelectSuggestion = { suggestionPrompt ->
                                    inputText = suggestionPrompt
                                }
                            )
                        }
                    }

                    // Existing Chat Messages
                    items(uiState.messages) { message ->
                        ChatMessageItem(
                            message = message,
                            modelName = uiState.currentSession?.model ?: "agnes-3.0-flash",
                            allMessages = uiState.messages,
                            onOpenCanvas = { title, lang, content ->
                                viewModel.openCanvas(CanvasArtifact(title = title, language = lang, content = content))
                            },
                            onEditPrompt = { text ->
                                inputText = text
                                Toast.makeText(context, context.getString(R.string.prompt_loaded_for_edit), Toast.LENGTH_SHORT).show()
                            },
                            onReloadMessage = { msg ->
                                viewModel.regenerateResponse(msg)
                                Toast.makeText(context, context.getString(R.string.reloading_response), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Live Streaming indicator
                    if (uiState.agentState is AgentState.Streaming) {
                        val streaming = uiState.agentState as AgentState.Streaming
                        item {
                            var liveTypingSec by remember { mutableFloatStateOf(0.1f) }
                            LaunchedEffect(streaming.isThinking) {
                                if (!streaming.isThinking) {
                                    val start = System.currentTimeMillis()
                                    while (isActive) {
                                        delay(100)
                                        liveTypingSec = ((System.currentTimeMillis() - start) / 1000f).coerceAtLeast(0.1f)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    GeminiSparkleIcon(size = 15.dp, tint = M3Primary)
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = uiState.currentSession?.model ?: "agnes-3.0-flash",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = M3Primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = M3SurfaceContainerHigh,
                                        border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color(0xFF81C995), CircleShape)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            val activityText = if (streaming.isThinking) {
                                                streaming.currentActivity
                                            } else {
                                                val durStr = String.format(java.util.Locale.US, "%.1fs", liveTypingSec)
                                                val baseActivity = streaming.currentActivity.substringBefore(" (")
                                                "$baseActivity ($durStr)"
                                            }
                                            Text(
                                                text = activityText,
                                                fontSize = 11.sp,
                                                color = M3TextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                if (streaming.content.isBlank() && streaming.reasoning.isNullOrBlank()) {
                                    Surface(
                                        color = M3SurfaceContainerLow,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, M3OutlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = streaming.currentActivity,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = M3TextSecondary
                                            )
                                        }
                                    }
                                }

                                if (streaming.isThinking || !streaming.reasoning.isNullOrBlank()) {
                                    ThinkingAccordion(
                                        reasoningContent = streaming.reasoning,
                                        thinkingDurationMs = streaming.thinkingElapsedMs,
                                        isCurrentlyStreaming = streaming.isThinking
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }

                                if (streaming.content.isNotBlank()) {
                                    MarkdownContent(markdown = streaming.content)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val outTokens = (streaming.content.length / 4).coerceAtLeast(1)
                                    val typingSpeed = (outTokens / liveTypingSec).coerceAtLeast(0f)
                                    val statusText = if (streaming.isThinking) {
                                        stringResource(R.string.streaming_thinking_meta, streaming.thinkingTokens)
                                    } else {
                                        val durStr = String.format(java.util.Locale.US, "%.1fs", liveTypingSec)
                                        val speedStr = String.format(java.util.Locale.US, "%.0f tok/s", typingSpeed)
                                        "Nalar: ${streaming.thinkingTokens} tok • Mengetik: $durStr ($outTokens tok • $speedStr)"
                                    }
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        color = M3TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Tool executing status
                    if (uiState.agentState is AgentState.ExecutingTool) {
                        val execState = uiState.agentState as AgentState.ExecutingTool
                        item {
                            ToolExecutionCard(
                                toolName = execState.toolName,
                                arguments = execState.description,
                                isLoading = true,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Agent Error state card
                    if (uiState.agentState is AgentState.Error) {
                        val errorState = uiState.agentState as AgentState.Error
                        item {
                            Surface(
                                color = M3ErrorContainer,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, M3Error.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = M3Error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.error_occurred),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = M3Error
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = errorState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = M3TextPrimary
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { viewModel.dismissError() }) {
                                            Text(stringResource(R.string.dismiss), color = M3TextSecondary)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.retryLastTurn() },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = M3Error, contentColor = M3OnError)
                                        ) {
                                            Text(stringResource(R.string.retry), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sticky Rate Limit Banner
                uiState.rateLimitStatus?.let { rateLimit ->
                    Surface(
                        color = M3ErrorContainer,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, M3Error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomBarHeightDp + 10.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = M3Error)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = rateLimit.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = M3Error
                                )
                            }
                            TextButton(onClick = { viewModel.stopStreaming() }) {
                                Text(stringResource(R.string.cancel), color = M3Error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Floating Input Capsule (Bottom - Android 16 Command Bar)
                val isStreamingActive = uiState.agentState is AgentState.Streaming || uiState.agentState is AgentState.ExecutingTool
                GeminiBottomCapsule(
                    inputText = inputText,
                    onInputTextChanged = { inputText = it },
                    activeWorkspacePath = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath,
                    isWebSearchActive = uiState.appSettings?.webSearchEnabled == true,
                    isAcademicActive = uiState.appSettings?.academicMode == true,
                    isYoloModeActive = uiState.appSettings?.yoloMode == true,
                    isStreaming = isStreamingActive,
                    attachedImages = attachedImages.toList(),
                    onRemoveAttachedImage = { index ->
                        if (index in attachedImages.indices) {
                            attachedImages.removeAt(index)
                        }
                    },
                    onOpenWorkspaceManager = { showWorkspaceManager = true },
                    onToggleWebSearch = { viewModel.toggleWebSearch() },
                    onToggleAcademic = { viewModel.toggleAcademicMode() },
                    onOpenCanvas = {
                        viewModel.openCanvas(
                            CanvasArtifact(
                                title = context.getString(R.string.canvas_scratchpad_title),
                                language = "markdown",
                                content = context.getString(R.string.canvas_scratchpad_content)
                            )
                        )
                    },
                    onToggleYolo = { viewModel.toggleYoloMode() },
                    onAttachImage = { imagePicker.launch("image/*") },
                    onCaptureCamera = launchCamera,
                    onSend = {
                        if (inputText.isNotBlank() || attachedImages.isNotEmpty()) {
                            val promptText = if (inputText.isNotBlank()) inputText else "Analisis gambar yang dilampirkan ini."
                            viewModel.sendMessage(promptText, attachedImages.toList())
                            inputText = ""
                            attachedImages.clear()
                        }
                    },
                    onStopStreaming = { viewModel.stopStreaming() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates ->
                            bottomBarHeightDp = with(density) { coordinates.size.height.toDp() }
                        }
                )
            }
        }
    }

    // Modal Workspace Manager BottomSheet
    if (showWorkspaceManager) {
        WorkspaceManagerBottomSheet(
            currentWorkspacePath = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath,
            recentWorkspaces = uiState.recentWorkspaces,
            onDismiss = { showWorkspaceManager = false },
            onWorkspaceSelected = { path ->
                viewModel.updateActiveWorkspace(path)
                showWorkspaceManager = false
            }
        )
    }

    // Modal Approval Dialog
    if (uiState.agentState is AgentState.AwaitingApproval) {
        val approval = uiState.agentState as AgentState.AwaitingApproval
        if (approval.isShell) {
            ShellApprovalDialog(
                command = approval.details,
                workingDir = approval.targetPath,
                onApprove = { viewModel.approveAction() },
                onReject = { viewModel.rejectAction() }
            )
        } else {
            DiffApprovalDialog(
                toolName = approval.toolName,
                targetPath = approval.targetPath,
                details = approval.details,
                onApprove = { viewModel.approveAction() },
                onReject = { viewModel.rejectAction() }
            )
        }
    }

    // Modal Canvas Sheet
    uiState.activeCanvasArtifact?.let { canvasArtifact ->
        CanvasModalSheet(
            artifact = canvasArtifact,
            workspacePath = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath,
            onDismiss = { viewModel.closeCanvas() },
            onSaveToWorkspace = { path, content ->
                val root = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath ?: "/sdcard"
                val normalizedRoot = WorkspacePathResolver.normalizePath(root)
                File(normalizedRoot, path).parentFile?.mkdirs()
                File(normalizedRoot, path).writeText(content)
            },
            onSendInstructionToAgent = { instruction, currentContent ->
                viewModel.sendMessage("Perbarui dokumen Canvas (${canvasArtifact.title}) dengan instruksi: $instruction\n\nIsi kode sekarang:\n```\n$currentContent\n```")
            }
        )
    }
}

@Composable
private fun WelcomeHeroSection(
    onSelectSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = M3PrimaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.4f)),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                GeminiSparkleIcon(size = 32.dp, tint = M3Primary)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_hero_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp
            ),
            color = M3TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.welcome_hero_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = M3TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Bento Grid Suggestions
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HeroSuggestionCard(
                    icon = Icons.Rounded.Web,
                    title = stringResource(R.string.suggestion_web_title),
                    prompt = stringResource(R.string.suggestion_web_prompt),
                    onSelect = onSelectSuggestion,
                    modifier = Modifier.weight(1f)
                )
                HeroSuggestionCard(
                    icon = Icons.Rounded.FolderOpen,
                    title = stringResource(R.string.suggestion_inspect_title),
                    prompt = stringResource(R.string.suggestion_inspect_prompt),
                    onSelect = onSelectSuggestion,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HeroSuggestionCard(
                    icon = Icons.Rounded.Terminal,
                    title = stringResource(R.string.suggestion_shell_title),
                    prompt = stringResource(R.string.suggestion_shell_prompt),
                    onSelect = onSelectSuggestion,
                    modifier = Modifier.weight(1f)
                )
                HeroSuggestionCard(
                    icon = Icons.Rounded.School,
                    title = stringResource(R.string.suggestion_research_title),
                    prompt = stringResource(R.string.suggestion_research_prompt),
                    onSelect = onSelectSuggestion,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroSuggestionCard(
    icon: ImageVector,
    title: String,
    prompt: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = M3SurfaceContainer,
        border = BorderStroke(1.dp, M3OutlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelect(prompt) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = M3PrimaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = M3Primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = M3TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = M3TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatMessageItem(
    message: Message,
    modelName: String,
    allMessages: List<Message> = emptyList(),
    onOpenCanvas: (title: String, language: String, content: String) -> Unit,
    onEditPrompt: (String) -> Unit = {},
    onReloadMessage: (Message) -> Unit = {}
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    if (message.role == MessageRole.USER) {
        // User message: right-aligned asymmetric M3 Expressive bubble
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = M3ExpressiveUserBubbleShape,
                    color = M3SurfaceContainerHighest,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.widthIn(max = 310.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (message.imagesBase64.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = if (message.content.isNotBlank()) 8.dp else 0.dp)
                            ) {
                                message.imagesBase64.forEach { base64Str ->
                                    val bitmap = remember(base64Str) {
                                        try {
                                            val cleanBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
                                            val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Gambar terlampir",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(1.dp, M3OutlineVariant, RoundedCornerShape(14.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                        if (message.content.isNotBlank()) {
                            SelectionContainer {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = M3TextPrimary,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Micro actions below user message bubble: Copy & Edit
                Row(
                    modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = M3SurfaceContainerHigh,
                        border = BorderStroke(1.dp, M3OutlineVariant),
                        modifier = Modifier.size(28.dp)
                    ) {
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(message.content))
                                Toast.makeText(context, context.getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.cd_copy),
                                tint = M3TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = M3SurfaceContainerHigh,
                        border = BorderStroke(1.dp, M3OutlineVariant),
                        modifier = Modifier.size(28.dp)
                    ) {
                        IconButton(
                            onClick = { onEditPrompt(message.content) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit),
                                tint = M3TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    } else if (message.role == MessageRole.ASSISTANT) {
        // AI response: Full-width elevated layout
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header: Sparkle + Model Name Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    GeminiSparkleIcon(size = 15.dp, tint = M3Primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = modelName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = M3Primary
                    )
                }

                // Thinking Accordion
                if (!message.reasoningContent.isNullOrBlank()) {
                    val tokens = message.tokenUsage?.reasoningTokens
                        ?: ((message.reasoningContent?.length ?: 0) / 4).coerceAtLeast(1)
                    ThinkingAccordion(
                        reasoningContent = message.reasoningContent,
                        thinkingDurationMs = message.thinkingDurationMs,
                        isCurrentlyStreaming = false,
                        thinkingTokens = tokens
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Tool Execution Cards
                message.toolCalls.forEach { toolCall ->
                    ToolExecutionCard(
                        toolName = toolCall.function.name,
                        arguments = toolCall.function.arguments,
                        outputContent = null,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )

                    if (toolCall.function.name == "open_canvas") {
                        val canvasTitle = try {
                            org.json.JSONObject(toolCall.function.arguments).optString("title", context.getString(R.string.canvas_title_default))
                        } catch (e: Exception) {
                            context.getString(R.string.canvas_title_default)
                        }
                        val canvasLang = try {
                            org.json.JSONObject(toolCall.function.arguments).optString("language", "markdown")
                        } catch (e: Exception) {
                            "markdown"
                        }
                        val canvasContent = try {
                            org.json.JSONObject(toolCall.function.arguments).optString("content", "")
                        } catch (e: Exception) {
                            ""
                        }
                        CanvasPreviewCard(
                            title = canvasTitle,
                            language = canvasLang,
                            onOpenCanvas = {
                                onOpenCanvas(canvasTitle, canvasLang, canvasContent)
                            }
                        )
                    }
                }

                // Markdown Body
                if (message.content.isNotBlank()) {
                    MarkdownContent(
                        markdown = message.content,
                        onOpenInCanvas = onOpenCanvas
                    )
                }

                // Inline Canvas Preview Card trigger if full code document is found
                val isCanvasEligible = message.content.contains("<!DOCTYPE html>", ignoreCase = true) ||
                    message.content.contains("<html", ignoreCase = true)
                if (isCanvasEligible && message.toolCalls.none { it.function.name == "open_canvas" }) {
                    val cleanWebDoc = extractCanvasContent(message.content)
                    val webTitle = stringResource(R.string.canvas_web_preview_title)
                    CanvasPreviewCard(
                        title = webTitle,
                        language = "html",
                        onOpenCanvas = {
                            onOpenCanvas(webTitle, "html", cleanWebDoc)
                        }
                    )
                }

                // Token Metadata & Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val thinkTokens = message.tokenUsage?.reasoningTokens?.takeIf { it > 0 }
                        ?: message.reasoningContent?.let { (it.length / 4).coerceAtLeast(1) }
                        ?: 0

                    val effectiveUsage = message.tokenUsage?.let { u ->
                        if (u.reasoningTokens == 0 && thinkTokens > 0) {
                            u.copy(reasoningTokens = thinkTokens)
                        } else u
                    } ?: if (message.content.isNotEmpty() || thinkTokens > 0) {
                        val outTokens = (message.content.length / 4).coerceAtLeast(1)
                        TokenUsage(
                            promptTokens = 0,
                            completionTokens = outTokens,
                            totalTokens = outTokens + thinkTokens,
                            reasoningTokens = thinkTokens
                        )
                    } else null

                    if (effectiveUsage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = M3SurfaceContainerLow,
                            border = BorderStroke(1.dp, M3OutlineVariant)
                        ) {
                            Text(
                                text = effectiveUsage.toCompactString(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = M3TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = M3SurfaceContainerHigh,
                            border = BorderStroke(1.dp, M3OutlineVariant),
                            modifier = Modifier.size(32.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(message.content))
                                    Toast.makeText(context, context.getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy),
                                    tint = M3TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = M3SurfaceContainerHigh,
                            border = BorderStroke(1.dp, M3OutlineVariant),
                            modifier = Modifier.size(32.dp)
                        ) {
                            IconButton(
                                onClick = { onReloadMessage(message) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.cd_reload),
                                    tint = M3TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = M3SurfaceContainerHigh,
                            border = BorderStroke(1.dp, M3OutlineVariant),
                            modifier = Modifier.size(32.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    onOpenCanvas(
                                        context.getString(R.string.canvas_ai_response),
                                        "markdown",
                                        message.content
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = stringResource(R.string.cd_open_in_canvas),
                                    tint = M3TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else if (message.role == MessageRole.TOOL) {
        val matchingToolCall = allMessages.asSequence()
            .flatMap { it.toolCalls.asSequence() }
            .firstOrNull { it.id == message.toolCallId }
        val toolName = matchingToolCall?.function?.name ?: "Tool Result"
        val toolArgs = matchingToolCall?.function?.arguments ?: ""

        ToolExecutionCard(
            toolName = toolName,
            arguments = toolArgs,
            outputContent = message.content,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
        )
    }
}

private fun extractCanvasContent(rawContent: String): String {
    val htmlBlockRegex = """```(?:html|web)?\s*\n([\s\S]*?)```""".toRegex(RegexOption.IGNORE_CASE)
    val match = htmlBlockRegex.find(rawContent)
    if (match != null) {
        return match.groupValues[1].trim()
    }
    val docStart = rawContent.indexOf("<!DOCTYPE", ignoreCase = true)
    val tagStart = if (docStart >= 0) docStart else rawContent.indexOf("<html", ignoreCase = true)
    val tagEnd = rawContent.lastIndexOf("</html>", ignoreCase = true)
    if (tagStart >= 0 && tagEnd > tagStart) {
        return rawContent.substring(tagStart, tagEnd + "</html>".length).trim()
    }
    return rawContent
}
