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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.material.icons.rounded.Hub
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
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
import id.gustyxganteng.agent.core.designsystem.M3ExpressiveUserBubbleShape
import id.gustyxganteng.agent.core.markdown.MarkdownContent
import id.gustyxganteng.agent.core.model.AgentExecutionMode
import id.gustyxganteng.agent.core.model.CanvasArtifact
import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.model.PromptMode
import id.gustyxganteng.agent.core.model.TokenUsage
import id.gustyxganteng.agent.core.util.ImageCompressor
import id.gustyxganteng.agent.domain.agent.AgentState
import id.gustyxganteng.agent.domain.tools.WorkspacePathResolver
import id.gustyxganteng.agent.feature.canvas.CanvasModalSheet
import id.gustyxganteng.agent.feature.chat.ui.components.CanvasPreviewCard
import id.gustyxganteng.agent.feature.chat.ui.components.CleanImageShowcaseCard
import id.gustyxganteng.agent.feature.chat.ui.components.GeminiBottomCapsule
import id.gustyxganteng.agent.feature.chat.ui.components.GeminiTopBar
import id.gustyxganteng.agent.feature.chat.ui.components.ImageGeneratingSilhouette
import id.gustyxganteng.agent.feature.chat.ui.components.ImageGenStudioModalSheet
import id.gustyxganteng.agent.feature.chat.ui.components.NeuralSpiderStatsCard
import id.gustyxganteng.agent.feature.chat.ui.components.NeuralTelemetryModalSheet
import id.gustyxganteng.agent.feature.chat.ui.components.ThinkingAccordion
import id.gustyxganteng.agent.feature.chat.ui.components.ToolExecutionCard
import id.gustyxganteng.agent.feature.workspace.DiffApprovalDialog
import id.gustyxganteng.agent.feature.workspace.ShellApprovalDialog
import id.gustyxganteng.agent.feature.workspace.WorkspaceManagerBottomSheet
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
    var shouldFollowOutput by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val density = LocalDensity.current
    var bottomBarHeightDp by remember { mutableStateOf(88.dp) }

    var inputText by remember { mutableStateOf("") }
    val attachedImages = remember { mutableStateListOf<String>() }
    var showWorkspaceManager by remember { mutableStateOf(false) }
    var showNeuralTelemetrySheet by remember { mutableStateOf(false) }
    var showImageGenStudioSheet by remember { mutableStateOf(false) }

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

    // A user drag disables auto-follow; touching the bottom re-enables it.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) shouldFollowOutput = false
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { !listState.canScrollForward }.collect { atBottom ->
            if (atBottom) shouldFollowOutput = true
        }
    }
    LaunchedEffect(uiState.currentSession?.id) {
        shouldFollowOutput = true
    }

    // New message event: snap to it instantly so the list never flashes the top.
    LaunchedEffect(uiState.messages.size) {
        if (shouldFollowOutput && uiState.messages.isNotEmpty()) {
            listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    // While the response grows, keep the bottom in view by scrolling only forward.
    val streamingState = uiState.agentState as? AgentState.Streaming
    val streamingContentLen = streamingState?.content?.length ?: 0
    val streamingReasoningLen = streamingState?.reasoning?.length ?: 0
    LaunchedEffect(streamingContentLen, streamingReasoningLen) {
        if (shouldFollowOutput) listState.scrollBy(Float.MAX_VALUE)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp),
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                GeminiSparkleIcon(size = 26.dp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.new_chat), fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.chat_history),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
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
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    unselectedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))

                    // Image Gen Studio Navigation Item
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text(stringResource(R.string.image_gen_studio_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showImageGenStudioSheet = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Telemetri Neural Navigation Item
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text(stringResource(R.string.telemetry_neural_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showNeuralTelemetrySheet = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Settings Navigation Item
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text(stringResource(R.string.settings_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                val isExecutionLocked = uiState.agentState !is AgentState.Idle && uiState.agentState !is AgentState.Error
                GeminiTopBar(
                    selectedModel = uiState.currentSession?.model ?: uiState.appSettings?.selectedModel ?: "agnes-3.0-flash",
                    executionMode = uiState.appSettings?.executionMode ?: AgentExecutionMode.FAST,
                    contextStatus = uiState.contextStatus,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onSelectModel = { viewModel.updateSessionModel(it) },
                    onSelectExecutionMode = { viewModel.updateExecutionMode(it) },
                    onCompactContext = {
                        viewModel.compactSession()
                        Toast.makeText(context, context.getString(R.string.compacting_context), Toast.LENGTH_SHORT).show()
                    },
                    onBranchSession = {
                        Toast.makeText(context, context.getString(R.string.branching_context_progress), Toast.LENGTH_SHORT).show()
                        viewModel.branchCurrentSession {
                            Toast.makeText(context, context.getString(R.string.session_branched_toast), Toast.LENGTH_SHORT).show()
                        }
                    },
                    isExecutionLocked = isExecutionLocked
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
                        bottom = bottomBarHeightDp + 16.dp
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
                            modelName = stringResource(R.string.app_name),
                            allMessages = uiState.messages,
                            isAgentRunning = uiState.agentState !is AgentState.Idle,
                            onOpenCanvas = { title, lang, content, filePath ->
                                viewModel.openCanvas(CanvasArtifact(title = title, language = lang, content = content, associatedFilePath = filePath))
                            },
                            onEditPrompt = { text ->
                                inputText = text
                                Toast.makeText(context, context.getString(R.string.prompt_loaded_for_edit), Toast.LENGTH_SHORT).show()
                            },
                            onReloadMessage = { msg ->
                                viewModel.regenerateResponse(msg)
                                Toast.makeText(context, context.getString(R.string.reloading_response), Toast.LENGTH_SHORT).show()
                            },
                            onOpenImageStudio = { showImageGenStudioSheet = true }
                        )
                    }

                    // Live Streaming indicator
                    if (uiState.agentState is AgentState.Streaming) {
                        val streaming = uiState.agentState as AgentState.Streaming
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    GeminiSparkleIcon(size = 15.dp, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = streaming.currentActivity.substringBefore(" ("),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (streaming.content.isBlank() && streaming.reasoning.isNullOrBlank()) {
                                    Text(
                                        text = streaming.currentActivity,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 22.dp, top = 2.dp, bottom = 8.dp)
                                    )
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

                            }
                        }
                    }

                    // Tool executing status
                    if (uiState.agentState is AgentState.ExecutingTool) {
                        val execState = uiState.agentState as AgentState.ExecutingTool
                        if (execState.toolName != "generate_image") {
                            item {
                                ToolExecutionCard(
                                    toolName = execState.toolName,
                                    arguments = execState.description,
                                    isLoading = true,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Agent Error state card
                    if (uiState.agentState is AgentState.Error) {
                        val errorState = uiState.agentState as AgentState.Error
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.error_occurred),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = errorState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { viewModel.dismissError() }) {
                                            Text(stringResource(R.string.dismiss), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.retryLastTurn() },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
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
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
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
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = rateLimit.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            TextButton(onClick = { viewModel.stopStreaming() }) {
                                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Floating Input Capsule (Bottom - Android 16 Command Bar)
                val isStreamingActive = uiState.agentState is AgentState.Streaming || uiState.agentState is AgentState.ExecutingTool
                GeminiBottomCapsule(
                    inputText = inputText,
                    onInputTextChanged = { inputText = it },
                    promptMode = uiState.appSettings?.promptMode ?: PromptMode.CHAT,
                    onSelectPromptMode = { mode ->
                        viewModel.updatePromptMode(mode)
                        if (mode == PromptMode.AGENT) {
                            val activeWs = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath
                            if (activeWs.isNullOrBlank()) {
                                showWorkspaceManager = true
                            }
                        }
                    },
                    activeWorkspacePath = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath,
                    isWebSearchActive = uiState.appSettings?.webSearchEnabled == true,
                    isStreaming = isStreamingActive,
                    attachedImages = attachedImages.toList(),
                    onRemoveAttachedImage = { index ->
                        if (index in attachedImages.indices) {
                            attachedImages.removeAt(index)
                        }
                    },
                    onOpenWorkspaceManager = { showWorkspaceManager = true },
                    onToggleWebSearch = { viewModel.toggleWebSearch() },
                    onOpenCanvas = {
                        viewModel.openCanvas(
                            CanvasArtifact(
                                title = context.getString(R.string.canvas_scratchpad_title),
                                language = "markdown",
                                content = context.getString(R.string.canvas_scratchpad_content)
                            )
                        )
                    },
                    onOpenImageGen = {
                        showImageGenStudioSheet = true
                    },
                    onAttachImage = { imagePicker.launch("image/*") },
                    onCaptureCamera = launchCamera,
                    onSend = {
                        shouldFollowOutput = true
                        val activeWs = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath
                        val currentMode = uiState.appSettings?.promptMode ?: PromptMode.CHAT
                        if (currentMode == PromptMode.AGENT && activeWs.isNullOrBlank()) {
                            Toast.makeText(context, context.getString(R.string.prompt_mode_agent_requires_folder), Toast.LENGTH_LONG).show()
                            showWorkspaceManager = true
                        } else if (inputText.isNotBlank() || attachedImages.isNotEmpty()) {
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

    // Modal Neural Telemetry Sheet
    if (showNeuralTelemetrySheet) {
        NeuralTelemetryModalSheet(
            stats = uiState.globalUsageStats,
            contextStatus = uiState.contextStatus,
            activeModel = uiState.currentSession?.model ?: uiState.appSettings?.selectedModel ?: "agnes-3.0-flash",
            executionMode = uiState.appSettings?.executionMode ?: AgentExecutionMode.STANDARD,
            totalSessionsCount = uiState.sessions.size,
            onDismiss = { showNeuralTelemetrySheet = false }
        )
    }

    // Modal Image Gen Studio Sheet
    if (showImageGenStudioSheet) {
        ImageGenStudioModalSheet(
            executionMode = uiState.appSettings?.executionMode ?: AgentExecutionMode.FAST,
            onSelectExecutionMode = { viewModel.updateExecutionMode(it) },
            onGenerateImage = { prompt, ratio ->
                viewModel.generateImageDirect(prompt, ratio)
            },
            onSendImageToChat = { markdown ->
                viewModel.insertAssistantMessage(markdown)
            },
            onDismiss = { showImageGenStudioSheet = false }
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
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                GeminiSparkleIcon(size = 48.dp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_hero_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.welcome_hero_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelect(prompt) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    isAgentRunning: Boolean = false,
    onOpenCanvas: (title: String, language: String, content: String, filePath: String?) -> Unit,
    onEditPrompt: (String) -> Unit = {},
    onReloadMessage: (Message) -> Unit = {},
    onOpenImageStudio: (() -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    if (message.role == MessageRole.USER) {
        // User message: right-aligned asymmetric M3 Expressive bubble
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = M3ExpressiveUserBubbleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
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
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
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
                                        color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(28.dp)
                    ) {
                        IconButton(
                            onClick = { onEditPrompt(message.content) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    } else if (message.role == MessageRole.ASSISTANT) {
        val imageToolCall = message.toolCalls.firstOrNull { it.function.name == "generate_image" }
        val hasMarkdownImage = message.content.contains("![") && message.content.contains("](")

        if (imageToolCall != null || hasMarkdownImage) {
            val matchingToolMsg = allMessages.firstOrNull { it.role == MessageRole.TOOL && it.toolCallId == imageToolCall?.id }
            val rawImageSource = matchingToolMsg?.content ?: message.content
            val imageUrl = extractImageUrl(rawImageSource)

            val promptFromArgs = try {
                if (imageToolCall != null) {
                    org.json.JSONObject(imageToolCall.function.arguments).optString("prompt", "")
                } else ""
            } catch (_: Exception) { "" }

            val altFromMarkdown = Regex("""!\[(.*?)\]""").find(rawImageSource)?.groupValues?.getOrNull(1)
            val displayPrompt = promptFromArgs.ifBlank { altFromMarkdown ?: "Ilustrasi Visual AI" }
            val textWithoutImage = message.content.replace(Regex("""!\[.*?\]\(.*?\)"""), "").trim()

            // Jika murni pesan visual atau dihasilkan oleh image tool
            if (imageUrl != null && (textWithoutImage.isBlank() || imageToolCall != null)) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            GeminiSparkleIcon(size = 15.dp, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        CleanImageShowcaseCard(
                            prompt = displayPrompt,
                            imageUrl = imageUrl,
                            modifier = Modifier.padding(vertical = 4.dp),
                            onOpenStudio = onOpenImageStudio
                        )

                        if (textWithoutImage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            MarkdownContent(
                                markdown = textWithoutImage,
                                onOpenInCanvas = { t, l, c -> onOpenCanvas(t, l, c, null) }
                            )
                        }
                    }
                }
                return
            } else if (imageToolCall != null && imageUrl == null) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            GeminiSparkleIcon(size = 15.dp, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isAgentRunning) {
                            ImageGeneratingSilhouette(
                                statusText = "Membuat sketsa...",
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Generasi gambar terputus atau gagal dimuat.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { onReloadMessage(message) }) {
                                        Text("Coba Lagi", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
                return
            }
        }

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
                    GeminiSparkleIcon(size = 15.dp, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = modelName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
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
                        val canvasFilePath = try {
                            org.json.JSONObject(toolCall.function.arguments).optString("file_path", "").takeIf { it.isNotBlank() }
                        } catch (e: Exception) {
                            null
                        }
                        CanvasPreviewCard(
                            title = canvasTitle,
                            language = canvasLang,
                            onOpenCanvas = {
                                onOpenCanvas(canvasTitle, canvasLang, canvasContent, canvasFilePath)
                            }
                        )
                    }
                }

                // Markdown Body
                if (message.content.isNotBlank()) {
                    MarkdownContent(
                        markdown = message.content,
                        onOpenInCanvas = { t, l, c -> onOpenCanvas(t, l, c, null) }
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
                            onOpenCanvas(webTitle, "html", cleanWebDoc, null)
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
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = effectiveUsage.toCompactString(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(32.dp)
                        ) {
                            IconButton(
                                onClick = { onReloadMessage(message) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.cd_reload),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(32.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    onOpenCanvas(
                                        context.getString(R.string.canvas_ai_response),
                                        "markdown",
                                        message.content,
                                        null
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = stringResource(R.string.cd_open_in_canvas),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

        // Sembunyikan kartu tool mentah untuk generate_image, karena Assistant message sudah merender CleanImageShowcaseCard
        if (toolName == "generate_image") {
            return
        }

        ToolExecutionCard(
            toolName = toolName,
            arguments = toolArgs,
            outputContent = message.content,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
        )
    }
}

private fun extractImageUrl(content: String): String? {
    val mdMatch = Regex("""!\[.*?\]\((https?://[^\s)]+|file://[^\s)]+|data:image/[^\s)]+)\)""").find(content)
    if (mdMatch != null) return mdMatch.groupValues[1]
    val rawMatch = Regex("""(https?://[^\s)]+\.(?:png|jpg|jpeg|webp)|file://[^\s)]+\.(?:png|jpg|jpeg|webp)|data:image/[^\s)]+)""", RegexOption.IGNORE_CASE).find(content)
    return rawMatch?.value
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
