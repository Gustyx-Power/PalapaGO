package id.gustyxganteng.agent.feature.canvas

import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import id.gustyxganteng.agent.core.markdown.MarkdownContent
import id.gustyxganteng.agent.core.model.CanvasArtifact
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasBottomSheet(
    artifact: CanvasArtifact,
    workspacePath: String?,
    onDismiss: () -> Unit,
    onSaveToWorkspace: (filePath: String, content: String) -> Unit,
    onSendInstructionToAgent: (instruction: String, currentContent: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentContent by remember(artifact) { mutableStateOf(artifact.content) }
    var instructionText by remember { mutableStateOf("") }
    var targetFilePath by remember(artifact) { mutableStateOf(artifact.associatedFilePath ?: "") }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = artifact.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Bahasa: ${artifact.language.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup Canvas")
                }
            }

            // 3-Tab selector
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("1. Editor") },
                    icon = { Icon(Icons.Default.Code, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("2. Preview") },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("3. Sync File") },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        // Tab 1: Editor with Line Numbers & Floating targeted bar
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                // Line Numbers
                                val linesCount = currentContent.lines().size.coerceAtLeast(1)
                                val lineNumbersText = (1..linesCount).joinToString("\n")
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        text = lineNumbersText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }

                                // Code Editor
                                TextField(
                                    value = currentContent,
                                    onValueChange = { currentContent = it },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .horizontalScroll(rememberScrollState()),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                            }

                            // Floating instruction bar
                            Surface(
                                tonalElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = instructionText,
                                        onValueChange = { instructionText = it },
                                        placeholder = { Text("Minta AI edit Canvas ini...", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (instructionText.isNotBlank()) {
                                                onSendInstructionToAgent(instructionText, currentContent)
                                                instructionText = ""
                                                onDismiss()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Kirim Instruksi ke AI",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Tab 2: Live Preview (WebView untuk HTML / Markdown renderer)
                        if (artifact.language.lowercase() in listOf("html", "js", "css", "web")) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = WebViewClient()
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(null, currentContent, "text/html", "utf-8", null)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Render via MarkdownContent
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                MarkdownContent(markdown = currentContent)
                            }
                        }
                    }
                    2 -> {
                        // Tab 3: Local Sync to Workspace
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Simpan Konten Canvas ke File Workspace",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Workspace Aktif: ${workspacePath ?: "Belum dipilih (pilih folder di chat)"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = targetFilePath,
                                onValueChange = { targetFilePath = it },
                                label = { Text("Path Relatif File (misal: src/index.html)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (targetFilePath.isNotBlank()) {
                                        onSaveToWorkspace(targetFilePath, currentContent)
                                        Toast.makeText(context, "Disimpan ke $targetFilePath", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = workspacePath != null && targetFilePath.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Simpan ke Workspace")
                            }
                        }
                    }
                }
            }
        }
    }
}
