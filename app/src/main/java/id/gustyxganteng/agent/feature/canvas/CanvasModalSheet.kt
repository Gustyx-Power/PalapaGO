package id.gustyxganteng.agent.feature.canvas

import android.content.Intent
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiActiveChipContainer
import id.gustyxganteng.agent.core.designsystem.GeminiActiveChipContent
import id.gustyxganteng.agent.core.designsystem.GeminiBackground
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSurface
import id.gustyxganteng.agent.core.designsystem.GeminiSurfaceVariant
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary
import id.gustyxganteng.agent.core.markdown.MarkdownContent
import id.gustyxganteng.agent.core.model.CanvasArtifact

enum class CanvasTab {
    PREVIEW,
    CODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasModalSheet(
    artifact: CanvasArtifact,
    workspacePath: String?,
    onDismiss: () -> Unit,
    onSaveToWorkspace: (filePath: String, content: String) -> Unit,
    onSendInstructionToAgent: (instruction: String, currentContent: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(CanvasTab.PREVIEW) }
    var currentContent by remember(artifact) { mutableStateOf(artifact.content) }
    var instructionText by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isWeb = remember(artifact.language, currentContent) {
        val lang = artifact.language.lowercase().trim()
        lang in listOf("html", "js", "javascript", "css", "web", "htm", "svg", "react", "jsx", "tsx", "ts", "typescript", "node", "android", "kotlin", "compose") ||
            currentContent.contains("<!DOCTYPE html>", ignoreCase = true) ||
            currentContent.contains("<html", ignoreCase = true) ||
            currentContent.contains("<svg", ignoreCase = true) ||
            currentContent.contains("import React", ignoreCase = true) ||
            currentContent.contains("from 'react'", ignoreCase = true) ||
            currentContent.contains("from \"react\"", ignoreCase = true) ||
            currentContent.contains("function App(", ignoreCase = true) ||
            currentContent.contains("const App =", ignoreCase = true) ||
            currentContent.contains("@Composable", ignoreCase = true) ||
            currentContent.contains("androidx.compose", ignoreCase = true) ||
            currentContent.contains("class MainActivity", ignoreCase = true) ||
            (currentContent.contains("</") && currentContent.contains(">"))
    }

    val formattedHtml = remember(currentContent, artifact.language) {
        val trimmed = currentContent.trim()
        val lang = artifact.language.lowercase().trim()
        val isReact = lang in listOf("react", "jsx", "tsx") ||
            trimmed.contains("import React", ignoreCase = true) ||
            trimmed.contains("from 'react'", ignoreCase = true) ||
            trimmed.contains("from \"react\"", ignoreCase = true) ||
            trimmed.contains("function App(", ignoreCase = true) ||
            trimmed.contains("const App =", ignoreCase = true)

        val isNodeConsole = (lang in listOf("node", "javascript", "js") || trimmed.contains("console.log(")) &&
            !trimmed.startsWith("<!DOCTYPE", ignoreCase = true) &&
            !trimmed.startsWith("<html", ignoreCase = true) &&
            !trimmed.contains("<body", ignoreCase = true) &&
            !isReact

        val isAndroid = (lang in listOf("android", "kotlin", "compose") ||
            trimmed.contains("@Composable", ignoreCase = true) ||
            trimmed.contains("androidx.compose", ignoreCase = true) ||
            trimmed.contains("class MainActivity", ignoreCase = true) ||
            (trimmed.contains("fun ") && trimmed.contains("Screen(") && trimmed.contains("Modifier"))) &&
            !trimmed.startsWith("<!DOCTYPE", ignoreCase = true) &&
            !trimmed.startsWith("<html", ignoreCase = true) &&
            !isReact

        if (isReact) {
            val sanitizedJsx = trimmed
                .replace(Regex("""import\s+.*?from\s+['"].*?['"];?"""), "")
                .replace(Regex("""export\s+default\s+"""), "")
                .replace(Regex("""export\s+"""), "")

            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>React Live Preview</title>
                <script src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
                <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
                <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
                <script src="https://cdn.tailwindcss.com"></script>
                <style>
                    *, *::before, *::after { box-sizing: border-box; }
                    body {
                        margin: 0;
                        padding: 16px;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        background-color: #0b0c0e;
                        color: #f4f4f5;
                        min-height: 100vh;
                    }
                </style>
            </head>
            <body>
                <div id="root"></div>
                <script type="text/babel">
                    const { useState, useEffect, useRef, useMemo, useCallback } = React;
                    $sanitizedJsx
                    try {
                        const targetComponent = typeof App !== 'undefined' ? App : (typeof Main !== 'undefined' ? Main : null);
                        if (targetComponent) {
                            ReactDOM.createRoot(document.getElementById('root')).render(React.createElement(targetComponent));
                        }
                    } catch (e) {
                        console.error("React Render Error: " + e.message);
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (isNodeConsole) {
            val escapedCode = trimmed
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("\$", "\\\$")

            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Node / JS Live Execution</title>
                <style>
                    *, *::before, *::after { box-sizing: border-box; }
                    body {
                        margin: 0;
                        padding: 16px;
                        font-family: monospace;
                        background: #0e1014;
                        color: #e6edf3;
                        font-size: 13px;
                        line-height: 1.6;
                    }
                    .header {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        padding-bottom: 10px;
                        margin-bottom: 14px;
                        border-bottom: 1px solid #23252c;
                        color: #7ee787;
                        font-weight: bold;
                    }
                    .terminal {
                        white-space: pre-wrap;
                        word-break: break-all;
                    }
                    .line-log { color: #f0f6fc; margin: 2px 0; }
                    .line-err { color: #ff7b72; margin: 2px 0; }
                    .line-info { color: #79c0ff; margin: 2px 0; }
                </style>
            </head>
            <body>
                <div class="header">● Node.js / JS Live Runtime</div>
                <div id="terminal" class="terminal"></div>
                <script>
                    const terminal = document.getElementById('terminal');
                    function appendLog(msg, cls) {
                        const div = document.createElement('div');
                        div.className = cls;
                        div.textContent = msg;
                        terminal.appendChild(div);
                    }
                    console.log = (...args) => appendLog(args.map(a => typeof a === 'object' ? JSON.stringify(a, null, 2) : String(a)).join(' '), 'line-log');
                    console.error = (...args) => appendLog('Error: ' + args.map(a => String(a)).join(' '), 'line-err');
                    console.warn = (...args) => appendLog('Warn: ' + args.map(a => String(a)).join(' '), 'line-info');
                    console.info = (...args) => appendLog('Info: ' + args.map(a => String(a)).join(' '), 'line-info');

                    try {
                        eval(`$escapedCode`);
                    } catch (err) {
                        console.error(err.message);
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (isAndroid) {
            AndroidSimulatorRenderer.render(artifact.title, trimmed)
        } else if (!trimmed.startsWith("<!DOCTYPE", ignoreCase = true) && !trimmed.startsWith("<html", ignoreCase = true)) {
            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    *, *::before, *::after {
                        box-sizing: border-box;
                    }
                    body {
                        margin: 0;
                        padding: 16px;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background-color: #ffffff;
                        color: #111111;
                        line-height: 1.5;
                    }
                </style>
            </head>
            <body>
                $trimmed
            </body>
            </html>
            """.trimIndent()
        } else if (!trimmed.contains("viewport", ignoreCase = true) && trimmed.contains("<head>", ignoreCase = true)) {
            trimmed.replace("<head>", "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        } else {
            trimmed
        }
    }

    val consoleLogs = remember { mutableStateListOf<String>() }
    var auditDurationMs by remember { mutableStateOf(1200L) }
    var auditRefreshKey by remember { mutableStateOf(0) }
    val auditResult = remember(formattedHtml, consoleLogs.size, auditRefreshKey) {
        CanvasWebAuditor.audit(formattedHtml, consoleLogs.toList(), auditDurationMs)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = GeminiBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // TopBar Header
            Surface(
                color = GeminiSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = GeminiTextPrimary
                        )
                    }

                    // Title & Language
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = artifact.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = GeminiTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.canvas_language, artifact.language.uppercase()),
                            style = MaterialTheme.typography.labelSmall,
                            color = GeminiPrimary
                        )
                    }

                    // Segmented Button [ Preview | Code ]
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GeminiSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Preview Option
                            val previewBg by animateColorAsState(
                                targetValue = if (selectedTab == CanvasTab.PREVIEW) GeminiActiveChipContainer else Color.Transparent,
                                label = "previewBg"
                            )
                            val previewContentColor by animateColorAsState(
                                targetValue = if (selectedTab == CanvasTab.PREVIEW) GeminiActiveChipContent else GeminiTextSecondary,
                                label = "previewColor"
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = previewBg,
                                modifier = Modifier.clickable { selectedTab = CanvasTab.PREVIEW }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        tint = previewContentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.canvas_tab_preview),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = previewContentColor
                                    )
                                }
                            }

                            // Code Option
                            val codeBg by animateColorAsState(
                                targetValue = if (selectedTab == CanvasTab.CODE) GeminiActiveChipContainer else Color.Transparent,
                                label = "codeBg"
                            )
                            val codeContentColor by animateColorAsState(
                                targetValue = if (selectedTab == CanvasTab.CODE) GeminiActiveChipContent else GeminiTextSecondary,
                                label = "codeColor"
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = codeBg,
                                modifier = Modifier.clickable { selectedTab = CanvasTab.CODE }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Code,
                                        contentDescription = null,
                                        tint = codeContentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.canvas_tab_code),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = codeContentColor
                                    )
                                }
                            }
                        }
                    }

                    // Open in Browser button
                    if (isWeb) {
                        IconButton(onClick = {
                            try {
                                val cacheFile = File(context.cacheDir, "canvas_preview.html")
                                cacheFile.writeText(formattedHtml)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "text/html")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.canvas_open_browser_failed, e.message ?: ""),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInBrowser,
                                contentDescription = stringResource(R.string.canvas_open_in_browser),
                                tint = GeminiPrimary
                            )
                        }
                    }

                    // Save to Workspace button
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = stringResource(R.string.canvas_save_to_workspace),
                            tint = GeminiPrimary
                        )
                    }
                }
            }

            HorizontalDivider(color = GeminiSurfaceVariant.copy(alpha = 0.5f))

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    CanvasTab.PREVIEW -> {
                        if (isWeb) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                CanvasAuditInspectionBar(
                                    auditResult = auditResult,
                                    onReaudit = {
                                        auditRefreshKey++
                                        auditDurationMs = (900L..1600L).random()
                                    },
                                    onFixWithAi = { instruction ->
                                        onSendInstructionToAgent(instruction, currentContent)
                                    }
                                )

                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            setBackgroundColor(android.graphics.Color.WHITE)
                                            @Suppress("DEPRECATION")
                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                databaseEnabled = true
                                                useWideViewPort = true
                                                loadWithOverviewMode = true
                                                allowContentAccess = true
                                                allowFileAccess = true
                                                allowFileAccessFromFileURLs = true
                                                allowUniversalAccessFromFileURLs = true
                                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                                builtInZoomControls = true
                                                displayZoomControls = false
                                                setSupportZoom(true)
                                                javaScriptCanOpenWindowsAutomatically = true
                                                mediaPlaybackRequiresUserGesture = false
                                                cacheMode = WebSettings.LOAD_DEFAULT
                                            }
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                                    val msg = consoleMessage?.message()
                                                    if (!msg.isNullOrBlank()) {
                                                        val formatted = "${consoleMessage.messageLevel()}: $msg (line ${consoleMessage.lineNumber()})"
                                                        if (!consoleLogs.contains(formatted)) {
                                                            consoleLogs.add(formatted)
                                                        }
                                                    }
                                                    return super.onConsoleMessage(consoleMessage)
                                                }
                                            }
                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                    return false
                                                }
                                            }
                                        }
                                    },
                                    update = { webView ->
                                        if (webView.tag != formattedHtml) {
                                            webView.tag = formattedHtml
                                            val baseUrl = if (!workspacePath.isNullOrBlank()) {
                                                val dir = File(workspacePath)
                                                if (dir.exists()) "file://${dir.absolutePath}/" else "https://localhost/"
                                            } else {
                                                "https://localhost/"
                                            }
                                            webView.loadDataWithBaseURL(baseUrl, formattedHtml, "text/html", "UTF-8", null)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                if (artifact.language.lowercase() == "markdown" || artifact.language.isBlank()) {
                                    MarkdownContent(markdown = currentContent)
                                } else {
                                    MarkdownContent(markdown = "```${artifact.language}\n$currentContent\n```")
                                }
                            }
                        }
                    }
                    CanvasTab.CODE -> {
                        val linesCount = currentContent.lines().size.coerceAtLeast(1)
                        val lineNumbersText = (1..linesCount).joinToString("\n")
                        val verticalScrollState = rememberScrollState()
                        val horizontalScrollState = rememberScrollState()

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(verticalScrollState)
                        ) {
                            // Line Numbers Column
                            Box(
                                modifier = Modifier
                                    .background(GeminiSurface)
                                    .padding(horizontal = 10.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = lineNumbersText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = GeminiTextSecondary.copy(alpha = 0.6f)
                                )
                            }

                            // Code Text Field
                            TextField(
                                value = currentContent,
                                onValueChange = { currentContent = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(horizontalScrollState),
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = GeminiTextPrimary
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = GeminiPrimary
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = GeminiSurfaceVariant.copy(alpha = 0.5f))

            // Quick-Edit Prompt Bar (Bottom)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = GeminiSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = instructionText,
                        onValueChange = { instructionText = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.canvas_prompt_placeholder),
                                color = GeminiTextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = GeminiTextPrimary, fontSize = 14.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = GeminiPrimary
                        )
                    )

                    IconButton(
                        onClick = {
                            if (instructionText.isNotBlank()) {
                                onSendInstructionToAgent(instructionText, currentContent)
                                instructionText = ""
                                onDismiss()
                            }
                        },
                        enabled = instructionText.isNotBlank()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (instructionText.isNotBlank()) GeminiPrimary else GeminiSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.cd_send),
                                    tint = if (instructionText.isNotBlank()) GeminiBackground else GeminiTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Save to Workspace Dialog
    if (showSaveDialog) {
        var relativePath by remember {
            val defaultExt = when (artifact.language.lowercase()) {
                "html", "web" -> "html"
                "js", "javascript" -> "js"
                "css" -> "css"
                "kotlin", "kt" -> "kt"
                "python", "py" -> "py"
                "json" -> "json"
                "markdown", "md" -> "md"
                else -> "txt"
            }
            val defaultName = if (!artifact.associatedFilePath.isNullOrBlank()) {
                artifact.associatedFilePath
            } else {
                val sanitized = artifact.title.lowercase().replace("\\s+".toRegex(), "_")
                "$sanitized.$defaultExt"
            }
            mutableStateOf(defaultName)
        }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = GeminiSurface,
            titleContentColor = GeminiTextPrimary,
            textContentColor = GeminiTextSecondary,
            title = {
                Text(
                    text = stringResource(R.string.canvas_save_to_workspace),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.canvas_active_workspace,
                            workspacePath ?: stringResource(R.string.canvas_no_workspace)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = GeminiTextSecondary
                    )

                    OutlinedTextField(
                        value = relativePath,
                        onValueChange = { relativePath = it },
                        label = { Text(stringResource(R.string.canvas_file_path_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiPrimary,
                            unfocusedBorderColor = GeminiSurfaceVariant,
                            focusedLabelColor = GeminiPrimary,
                            unfocusedLabelColor = GeminiTextSecondary,
                            focusedTextColor = GeminiTextPrimary,
                            unfocusedTextColor = GeminiTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (relativePath.isNotBlank()) {
                            onSaveToWorkspace(relativePath, currentContent)
                            Toast.makeText(
                                context,
                                context.getString(R.string.canvas_saved_success, relativePath),
                                Toast.LENGTH_SHORT
                            ).show()
                            showSaveDialog = false
                        }
                    },
                    enabled = workspacePath != null && relativePath.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeminiPrimary,
                        contentColor = GeminiBackground,
                        disabledContainerColor = GeminiSurfaceVariant,
                        disabledContentColor = GeminiTextSecondary
                    )
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.cancel), color = GeminiTextSecondary)
                }
            }
        )
    }
}

/**
 * Backward compatibility alias for CanvasBottomSheet -> CanvasModalSheet
 */
@Composable
fun CanvasBottomSheet(
    artifact: CanvasArtifact,
    workspacePath: String?,
    onDismiss: () -> Unit,
    onSaveToWorkspace: (filePath: String, content: String) -> Unit,
    onSendInstructionToAgent: (instruction: String, currentContent: String) -> Unit,
    modifier: Modifier = Modifier
) {
    CanvasModalSheet(
        artifact = artifact,
        workspacePath = workspacePath,
        onDismiss = onDismiss,
        onSaveToWorkspace = onSaveToWorkspace,
        onSendInstructionToAgent = onSendInstructionToAgent,
        modifier = modifier
    )
}
