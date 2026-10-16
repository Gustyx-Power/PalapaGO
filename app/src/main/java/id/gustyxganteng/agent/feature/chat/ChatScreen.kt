package id.gustyxganteng.agent.feature.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiBackground
import id.gustyxganteng.agent.core.designsystem.GeminiError
import id.gustyxganteng.agent.core.designsystem.GeminiErrorContainer
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSparkleIcon
import id.gustyxganteng.agent.core.designsystem.GeminiSurface
import id.gustyxganteng.agent.core.designsystem.GeminiSurfaceVariant
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary
import id.gustyxganteng.agent.core.markdown.MarkdownContent
import id.gustyxganteng.agent.core.model.CanvasArtifact
import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.util.ImageCompressor
import id.gustyxganteng.agent.domain.agent.AgentState
import id.gustyxganteng.agent.domain.tools.WorkspacePathResolver
import id.gustyxganteng.agent.feature.canvas.CanvasModalSheet
import id.gustyxganteng.agent.feature.chat.ui.components.CanvasPreviewCard
import id.gustyxganteng.agent.feature.chat.ui.components.GeminiBottomCapsule
import id.gustyxganteng.agent.feature.chat.ui.components.GeminiTopBar
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
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    val attachedImages = remember { mutableStateListOf<String>() }
    var showWorkspaceManager by remember { mutableStateOf(false) }

    // Image Picker Launcher
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

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = GeminiSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GeminiSparkleIcon(size = 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GeminiTextPrimary
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = GeminiTextSecondary
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            viewModel.createNewSession()
                            scope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeminiSurfaceVariant,
                            contentColor = GeminiPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = GeminiPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.new_chat))
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.chat_history),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GeminiPrimary
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(uiState.sessions) { session ->
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = session.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = GeminiTextPrimary
                                    )
                                },
                                selected = session.id == uiState.currentSession?.id,
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    scope.launch { drawerState.close() }
                                },
                                badge = {
                                    IconButton(
                                        onClick = { viewModel.deleteSession(session.id) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = GeminiTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = GeminiSurfaceVariant,
                                    unselectedContainerColor = GeminiSurface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = GeminiSurfaceVariant)
                    Spacer(Modifier.height(8.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null, tint = GeminiPrimary) },
                        label = { Text(stringResource(R.string.settings_and_byok), color = GeminiTextPrimary) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = GeminiBackground,
            topBar = {
                GeminiTopBar(
                    selectedModel = uiState.currentSession?.model ?: uiState.appSettings?.selectedModel ?: "agnes-3.0-flash",
                    contextStatus = uiState.contextStatus,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onSelectModel = { viewModel.updateSessionModel(it) },
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
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatMessageItem(
                            message = message,
                            modelName = uiState.currentSession?.model ?: "agnes-3.0-flash",
                            onOpenCanvas = { title, lang, content ->
                                viewModel.openCanvas(CanvasArtifact(title = title, language = lang, content = content))
                            }
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
                                    GeminiSparkleIcon(size = 14.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = uiState.currentSession?.model ?: "agnes-3.0-flash",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GeminiPrimary
                                    )
                                }

                                if (!streaming.reasoning.isNullOrBlank()) {
                                    ThinkingAccordion(
                                        reasoningContent = streaming.reasoning,
                                        thinkingDurationMs = streaming.thinkingElapsedMs,
                                        isCurrentlyStreaming = true
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

                // Sticky Rate Limit Banner
                uiState.rateLimitStatus?.let { rateLimit ->
                    Surface(
                        color = GeminiErrorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 110.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = GeminiError)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = rateLimit.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GeminiError
                                )
                            }
                            TextButton(onClick = { viewModel.stopStreaming() }) {
                                Text(stringResource(R.string.cancel), color = GeminiError)
                            }
                        }
                    }
                }

                // Floating Input Capsule (Bottom)
                val isStreamingActive = uiState.agentState is AgentState.Streaming || uiState.agentState is AgentState.ExecutingTool
                GeminiBottomCapsule(
                    inputText = inputText,
                    onInputTextChanged = { inputText = it },
                    activeWorkspacePath = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath,
                    isWebSearchActive = uiState.appSettings?.webSearchEnabled == true,
                    isYoloModeActive = uiState.appSettings?.yoloMode == true,
                    isStreaming = isStreamingActive,
                    attachedImagesCount = attachedImages.size,
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
                    onToggleYolo = { viewModel.toggleYoloMode() },
                    onAttachImage = { imagePicker.launch("image/*") },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText, attachedImages.toList())
                            inputText = ""
                            attachedImages.clear()
                        }
                    },
                    onStopStreaming = { viewModel.stopStreaming() },
                    modifier = Modifier.align(Alignment.BottomCenter)
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
fun ChatMessageItem(
    message: Message,
    modelName: String,
    onOpenCanvas: (title: String, language: String, content: String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    if (message.role == MessageRole.USER) {
        // User message: right-aligned capsule with GeminiSurfaceVariant
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 4.dp, bottomEnd = 24.dp, bottomStart = 24.dp),
                color = GeminiSurfaceVariant,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = GeminiTextPrimary
                    )
                }
            }
        }
    } else if (message.role == MessageRole.ASSISTANT) {
        // AI response: Full-width without bubble background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Header: Sparkle + Model Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                GeminiSparkleIcon(size = 15.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = GeminiPrimary
                )
            }

            // Thinking Accordion
            if (!message.reasoningContent.isNullOrBlank()) {
                ThinkingAccordion(
                    reasoningContent = message.reasoningContent,
                    thinkingDurationMs = message.thinkingDurationMs
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
                // Token details
                message.tokenUsage?.let { usage ->
                    Text(
                        text = usage.toCompactString(),
                        fontSize = 11.sp,
                        color = GeminiTextSecondary
                    )
                } ?: Spacer(Modifier.width(1.dp))

                Row {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(message.content))
                            Toast.makeText(context, context.getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.cd_copy),
                            tint = GeminiTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onOpenCanvas(
                                context.getString(R.string.canvas_ai_response),
                                "markdown",
                                message.content
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = stringResource(R.string.cd_open_in_canvas),
                            tint = GeminiTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    } else if (message.role == MessageRole.TOOL) {
        // Tool Result display
        ToolExecutionCard(
            toolName = message.toolCallId ?: "tool",
            arguments = "",
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

