package id.gustyxganteng.agent.feature.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.gustyxganteng.agent.core.markdown.MarkdownContent
import id.gustyxganteng.agent.core.model.CanvasArtifact
import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.model.ToolCall
import id.gustyxganteng.agent.core.util.ImageCompressor
import id.gustyxganteng.agent.domain.agent.AgentState
import id.gustyxganteng.agent.feature.canvas.CanvasBottomSheet
import id.gustyxganteng.agent.feature.workspace.DiffApprovalDialog
import id.gustyxganteng.agent.feature.workspace.ShellApprovalDialog
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
    var showModelMenu by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var folderInputText by remember { mutableStateOf("") }

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
                    Toast.makeText(context, "Gagal mengompres gambar: ${e.message}", Toast.LENGTH_SHORT).show()
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
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Text(
                        text = "GG Agent",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Android AI Workspace & Coding",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.createNewSession()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Percakapan Baru")
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Riwayat Sesi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(uiState.sessions) { session ->
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = session.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                                            contentDescription = "Hapus",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }

                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Pengaturan & BYOK") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // TopBar Dinamis ala Gemini
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }

                        // Model Selector Pill (Tengah)
                        Box {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { showModelMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.currentSession?.model ?: "agnes-3.0-flash",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("agnes-3.0-flash (Cepat)") },
                                    onClick = {
                                        viewModel.updateSessionModel("agnes-3.0-flash")
                                        showModelMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("agnes-3.0-pro (Kuat & Cerdas)") },
                                    onClick = {
                                        viewModel.updateSessionModel("agnes-3.0-pro")
                                        showModelMenu = false
                                    }
                                )
                            }
                        }

                        // Context Meter Pill (Kanan)
                        uiState.contextStatus?.let { status ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = status.indicatorColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, status.indicatorColor.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    viewModel.compactSession()
                                    Toast.makeText(context, "Meringkas konteks percakapan...", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(status.indicatorColor, CircleShape)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = status.formattedDisplay,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
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
                        .padding(bottom = 140.dp)
                ) {
                    items(uiState.messages) { message ->
                        MessageItem(
                            message = message,
                            onOpenInCanvas = { title, lang, content ->
                                viewModel.openCanvas(CanvasArtifact(title = title, language = lang, content = content))
                            }
                        )
                    }

                    // Live Streaming indicator
                    if (uiState.agentState is AgentState.Streaming) {
                        val streamingState = uiState.agentState as AgentState.Streaming
                        item {
                            StreamingItem(
                                content = streamingState.content,
                                reasoning = streamingState.reasoning,
                                thinkingDurationMs = streamingState.thinkingElapsedMs
                            )
                        }
                    }

                    // Tool executing status card
                    if (uiState.agentState is AgentState.ExecutingTool) {
                        val execState = uiState.agentState as AgentState.ExecutingTool
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "⚡ ${execState.description}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Sticky Rate Limit Banner
                uiState.rateLimitStatus?.let { rateLimit ->
                    Surface(
                        color = Color(0xFFFFF3CD),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEEBA)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 124.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFF856404))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = rateLimit.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF856404)
                                )
                            }
                            TextButton(onClick = { viewModel.stopStreaming() }) {
                                Text("Batal", color = Color(0xFF721C24))
                            }
                        }
                    }
                }

                // Floating Input Capsule (Bawah)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Top Row: Toggle Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Workspace Chip
                                val currentWs = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath
                                FilterChip(
                                    selected = currentWs != null,
                                    onClick = { showFolderDialog = true },
                                    label = {
                                        Text(
                                            text = if (currentWs != null) File(currentWs).name else "Pilih Folder",
                                            maxLines = 1
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )

                                // Web Search Chip
                                FilterChip(
                                    selected = uiState.appSettings?.webSearchEnabled == true,
                                    onClick = { viewModel.toggleWebSearch() },
                                    label = { Text("Web Search") },
                                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )

                                // Canvas Chip
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.openCanvas(
                                            CanvasArtifact(
                                                title = "Scratchpad",
                                                language = "markdown",
                                                content = "# Canvas Baru\nTulis ide atau kode di sini..."
                                            )
                                        )
                                    },
                                    label = { Text("Canvas") },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )

                                // YOLO Mode Chip
                                FilterChip(
                                    selected = uiState.appSettings?.yoloMode == true,
                                    onClick = { viewModel.toggleYoloMode() },
                                    label = { Text("⚡ YOLO") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFF9800),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            // Attached Images Preview Row
                            if (attachedImages.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    attachedImages.forEachIndexed { idx, _ ->
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color.Gray, RoundedCornerShape(8.dp))
                                        ) {
                                            Text(
                                                "Img ${idx + 1}",
                                                modifier = Modifier.align(Alignment.Center),
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Main Input Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { imagePicker.launch("image/*") }
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Lampiran",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = { Text("Tanyakan apa saja atau perintahkan agent...") },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 5,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )

                                val isStreaming = uiState.agentState is AgentState.Streaming || uiState.agentState is AgentState.ExecutingTool
                                IconButton(
                                    onClick = {
                                        if (isStreaming) {
                                            viewModel.stopStreaming()
                                        } else if (inputText.isNotBlank()) {
                                            viewModel.sendMessage(inputText, attachedImages.toList())
                                            inputText = ""
                                            attachedImages.clear()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.Send,
                                        contentDescription = if (isStreaming) "Stop" else "Kirim",
                                        tint = if (isStreaming) Color.Red else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Folder Selector Dialog
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Tautkan Workspace Lokal") },
            text = {
                Column {
                    Text("Ketik path direktori lokal yang ingin diakses oleh Agent (contoh: /sdcard/Projects/MyApp):", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = folderInputText,
                        onValueChange = { folderInputText = it },
                        label = { Text("Path Direktori") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderInputText.isNotBlank()) {
                            viewModel.updateActiveWorkspace(folderInputText.trim())
                            showFolderDialog = false
                        }
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Batal")
                }
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

    // Modal Canvas BottomSheet
    uiState.activeCanvasArtifact?.let { canvasArtifact ->
        CanvasBottomSheet(
            artifact = canvasArtifact,
            workspacePath = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath,
            onDismiss = { viewModel.closeCanvas() },
            onSaveToWorkspace = { path, content ->
                val root = uiState.currentSession?.workspacePath ?: uiState.appSettings?.activeWorkspacePath ?: "/sdcard"
                File(root, path).parentFile?.mkdirs()
                File(root, path).writeText(content)
            },
            onSendInstructionToAgent = { instruction, currentContent ->
                viewModel.sendMessage("Perbarui dokumen Canvas (${canvasArtifact.title}) dengan instruksi: $instruction\n\nIsi kode sekarang:\n```\n$currentContent\n```")
            }
        )
    }
}

@Composable
fun MessageItem(
    message: Message,
    onOpenInCanvas: (title: String, language: String, content: String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    if (message.role == MessageRole.USER) {
        // User message: right-aligned capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    } else if (message.role == MessageRole.ASSISTANT) {
        // AI response: full width
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Collapsible Thinking Accordion
            if (!message.reasoningContent.isNullOrBlank()) {
                var expanded by remember { mutableStateOf(false) }
                val durationSec = (message.thinkingDurationMs ?: 1500) / 1000.0

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Berpikir selama ${String.format(java.util.Locale.US, "%.1f", durationSec)} dtk",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (expanded) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = message.reasoningContent,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Main Markdown Response
            if (message.content.isNotBlank()) {
                MarkdownContent(
                    markdown = message.content,
                    onOpenInCanvas = onOpenInCanvas
                )
            }

            // Tool Execution Cards
            message.toolCalls.forEach { toolCall ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (toolCall.function.name) {
                                "execute_shell" -> Icons.Default.Terminal
                                "read_file", "write_file", "apply_diff", "list_directory" -> Icons.Default.FolderOpen
                                else -> Icons.Default.Code
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${toolCall.function.name}: ${toolCall.function.arguments.take(50)}...",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Footer Response
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
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } ?: Spacer(Modifier.width(1.dp))

                // Actions: Copy, Canvas
                Row {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "Teks disalin!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            onOpenInCanvas("Respon AI", "markdown", message.content)
                        }
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "Buka di Canvas", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    } else if (message.role == MessageRole.TOOL) {
        // Tool result pill
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Hasil eksekusi: ${message.content.take(80)}...",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StreamingItem(
    content: String,
    reasoning: String?,
    thinkingDurationMs: Long?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (!reasoning.isNullOrBlank()) {
            val sec = (thinkingDurationMs ?: 0) / 1000.0
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Berpikir selama ${String.format(java.util.Locale.US, "%.1f", sec)} dtk...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (content.isNotBlank()) {
            MarkdownContent(markdown = content)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Sedang merespons...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun Modifier.widthIn(max: androidx.compose.ui.unit.Dp): Modifier = this.then(
    Modifier.padding(end = 0.dp)
)
