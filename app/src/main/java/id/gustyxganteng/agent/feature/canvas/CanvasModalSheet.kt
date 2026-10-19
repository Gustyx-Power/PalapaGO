package id.gustyxganteng.agent.feature.canvas

import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.BorderStroke
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

class ScrollableCanvasWebView(context: Context) : WebView(context) {
    init {
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = true
        isNestedScrollingEnabled = false
        overScrollMode = View.OVER_SCROLL_ALWAYS
        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }
}

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
    var currentContent by remember(artifact) {
        val initial = if (!artifact.associatedFilePath.isNullOrBlank() && !workspacePath.isNullOrBlank()) {
            val file = File(workspacePath, artifact.associatedFilePath)
            if (file.exists() && file.isFile) {
                file.readText()
            } else artifact.content
        } else if (!workspacePath.isNullOrBlank()) {
            val directFile = File(workspacePath, artifact.title)
            val htmlFile = if (!artifact.title.endsWith(".html", ignoreCase = true)) {
                File(workspacePath, "${artifact.title}.html")
            } else null
            when {
                directFile.exists() && directFile.isFile -> directFile.readText()
                htmlFile != null && htmlFile.exists() && htmlFile.isFile -> htmlFile.readText()
                else -> artifact.content
            }
        } else {
            artifact.content
        }
        mutableStateOf(initial)
    }
    var instructionText by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isWeb = remember(artifact.language, currentContent) {
        val lang = artifact.language.lowercase().trim()
        val trimmed = currentContent.trim()
        trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            lang in listOf("html", "js", "javascript", "css", "web", "htm", "svg", "react", "jsx", "tsx", "ts", "typescript", "node", "android", "kotlin", "compose", "url") ||
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
                    html, body {
                        margin: 0;
                        padding: 16px;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        background-color: #0b0c0e;
                        color: #f4f4f5;
                        min-height: 100vh;
                        overflow-y: auto !important;
                        -webkit-overflow-scrolling: touch !important;
                        touch-action: pan-y !important;
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
                    html, body {
                        margin: 0;
                        padding: 16px;
                        font-family: monospace;
                        background: #0e1014;
                        color: #e6edf3;
                        font-size: 13px;
                        line-height: 1.6;
                        overflow-y: auto !important;
                        -webkit-overflow-scrolling: touch !important;
                        touch-action: pan-y !important;
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
        } else {
            val cleanHtml = if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
                trimmed.substringAfter("\n").substringBeforeLast("```").trim()
            } else {
                trimmed
            }

            // Normalize dvh units which evaluate to 0 in embedded Android WebView
            val dvhNormalized = cleanHtml
                .replace(Regex("""(\d+)dvh""", RegexOption.IGNORE_CASE), "$1vh")
                .replace(Regex("""100dvh""", RegexOption.IGNORE_CASE), "100vh")

            val layoutHelper = """
                <style id="canvas-runtime-style">
                  :root { --dvh: 100vh; --vh: 1vh; }
                  html {
                    min-height: 100% !important;
                    width: 100% !important;
                    overflow-x: hidden !important;
                    overflow-y: auto !important;
                    -webkit-overflow-scrolling: touch !important;
                  }
                  body {
                    min-height: 100% !important;
                    width: 100% !important;
                    overflow-x: hidden !important;
                    overflow-y: auto !important;
                    -webkit-overflow-scrolling: touch !important;
                    touch-action: pan-y !important;
                    -webkit-text-size-adjust: 100%;
                  }
                  #app, #root, .app-root {
                    min-height: 100% !important;
                    max-width: 100vw;
                    overflow-y: visible !important;
                  }
                  /* Sleek accessible scrollbar */
                  ::-webkit-scrollbar {
                    width: 6px !important;
                    height: 6px !important;
                  }
                  ::-webkit-scrollbar-track {
                    background: transparent !important;
                  }
                  ::-webkit-scrollbar-thumb {
                    background: rgba(255, 255, 255, 0.25) !important;
                    border-radius: 6px !important;
                  }
                  ::-webkit-scrollbar-thumb:hover {
                    background: rgba(255, 255, 255, 0.45) !important;
                  }
                </style>
                <script id="canvas-runtime-helper">
                  (function() {
                    function normalizeLayout() {
                      var h = window.innerHeight;
                      document.documentElement.style.setProperty('--dvh', h + 'px');
                      document.documentElement.style.setProperty('--vh', (h * 0.01) + 'px');
                      if (document.documentElement) {
                        document.documentElement.style.setProperty('overflow-y', 'auto', 'important');
                      }
                      if (document.body) {
                        document.body.style.setProperty('overflow-y', 'auto', 'important');
                        document.body.style.setProperty('touch-action', 'pan-y', 'important');
                        document.body.style.setProperty('-webkit-overflow-scrolling', 'touch', 'important');
                      }
                      var app = document.getElementById('app') || document.getElementById('root');
                      if (app) {
                        var s = window.getComputedStyle(app);
                        if (s.height === '0px' || s.height === '16px' || parseInt(s.height) <= 20) {
                          app.style.setProperty('height', '100%', 'important');
                          app.style.setProperty('min-height', '100%', 'important');
                        }
                      }
                      var panels = document.querySelectorAll('.panel');
                      panels.forEach(function(p) {
                        var ps = window.getComputedStyle(p);
                        if (ps.display === 'block' && (ps.flexDirection === 'row' || ps.flexWrap === 'wrap')) {
                          p.style.setProperty('display', 'flex', 'important');
                        }
                      });
                      if (typeof window.resizeCanvas === 'function') {
                        try { window.resizeCanvas(); } catch(e){}
                      }
                    }
                    if (document.readyState === 'loading') {
                      document.addEventListener('DOMContentLoaded', normalizeLayout);
                    } else {
                      normalizeLayout();
                    }
                    window.addEventListener('resize', normalizeLayout);
                    window.addEventListener('load', normalizeLayout);
                    setTimeout(normalizeLayout, 100);
                    setTimeout(normalizeLayout, 400);
                  })();
                </script>
            """.trimIndent()

            if (dvhNormalized.contains("<head>", ignoreCase = true)) {
                val headRegex = Regex("""<head>""", RegexOption.IGNORE_CASE)
                val viewportTag = if (!dvhNormalized.contains("viewport", ignoreCase = true)) {
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, minimum-scale=1.0, user-scalable=yes\">\n"
                } else ""
                dvhNormalized.replaceFirst(headRegex, "<head>\n$viewportTag$layoutHelper")
            } else if (dvhNormalized.contains("<html", ignoreCase = true)) {
                val htmlRegex = Regex("""<html[^>]*>""", RegexOption.IGNORE_CASE)
                dvhNormalized.replaceFirst(htmlRegex, "$0\n<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, minimum-scale=1.0, user-scalable=yes\">\n$layoutHelper</head>")
            } else if (dvhNormalized.startsWith("<!DOCTYPE", ignoreCase = true)) {
                dvhNormalized + "\n" + layoutHelper
            } else {
                """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0, user-scalable=yes">
                    <style>
                        *, *::before, *::after { box-sizing: border-box; }
                        html, body {
                            margin: 0;
                            padding: 16px;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                            background-color: #0b0c0e;
                            color: #f4f4f5;
                            line-height: 1.5;
                            overflow-y: auto !important;
                            -webkit-overflow-scrolling: touch !important;
                            touch-action: pan-y !important;
                        }
                    </style>
                    $layoutHelper
                </head>
                <body>
                    $dvhNormalized
                </body>
                </html>
                """.trimIndent()
            }
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
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = id.gustyxganteng.agent.core.designsystem.M3Surface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // TopBar Header
            Surface(
                color = id.gustyxganteng.agent.core.designsystem.M3Surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Tier 1: Navigation & Title & Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close button
                        Surface(
                            shape = CircleShape,
                            color = id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh,
                            border = BorderStroke(1.dp, id.gustyxganteng.agent.core.designsystem.M3OutlineVariant),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_close),
                                    tint = id.gustyxganteng.agent.core.designsystem.M3TextPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // Title & Language (Full available width)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artifact.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = id.gustyxganteng.agent.core.designsystem.M3TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.canvas_language, artifact.language.uppercase()),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = id.gustyxganteng.agent.core.designsystem.M3Primary
                            )
                        }

                        // Actions Row: Browser & Save
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isWeb) {
                                Surface(
                                    shape = CircleShape,
                                    color = id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh,
                                    border = BorderStroke(1.dp, id.gustyxganteng.agent.core.designsystem.M3OutlineVariant),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    IconButton(onClick = {
                                        try {
                                            val trimmed = currentContent.trim()
                                            val isDirectUrl = trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)
                                            val intent = if (isDirectUrl) {
                                                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trimmed)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            } else {
                                                val cacheFile = File(context.cacheDir, "canvas_preview.html")
                                                cacheFile.writeText(formattedHtml)
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                                                Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "text/html")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
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
                                            tint = id.gustyxganteng.agent.core.designsystem.M3TextSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh,
                                border = BorderStroke(1.dp, id.gustyxganteng.agent.core.designsystem.M3OutlineVariant),
                                modifier = Modifier.size(36.dp)
                            ) {
                                IconButton(onClick = { showSaveDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Save,
                                        contentDescription = stringResource(R.string.canvas_save_to_workspace),
                                        tint = id.gustyxganteng.agent.core.designsystem.M3TextSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tier 2: Centered Segmented Control [ Preview | Code ]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh,
                            border = BorderStroke(1.dp, id.gustyxganteng.agent.core.designsystem.M3OutlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Preview Option
                                val previewBg by animateColorAsState(
                                    targetValue = if (selectedTab == CanvasTab.PREVIEW) id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer else Color.Transparent,
                                    label = "previewBg"
                                )
                                val previewContentColor by animateColorAsState(
                                    targetValue = if (selectedTab == CanvasTab.PREVIEW) id.gustyxganteng.agent.core.designsystem.M3OnPrimaryContainer else id.gustyxganteng.agent.core.designsystem.M3TextSecondary,
                                    label = "previewColor"
                                )
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = previewBg,
                                    modifier = Modifier.clickable { selectedTab = CanvasTab.PREVIEW }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Visibility,
                                            contentDescription = null,
                                            tint = previewContentColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = stringResource(R.string.canvas_tab_preview),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = previewContentColor
                                        )
                                    }
                                }

                                // Code Option
                                val codeBg by animateColorAsState(
                                    targetValue = if (selectedTab == CanvasTab.CODE) id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer else Color.Transparent,
                                    label = "codeBg"
                                )
                                val codeContentColor by animateColorAsState(
                                    targetValue = if (selectedTab == CanvasTab.CODE) id.gustyxganteng.agent.core.designsystem.M3OnPrimaryContainer else id.gustyxganteng.agent.core.designsystem.M3TextSecondary,
                                    label = "codeColor"
                                )
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = codeBg,
                                    modifier = Modifier.clickable { selectedTab = CanvasTab.CODE }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Code,
                                            contentDescription = null,
                                            tint = codeContentColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = stringResource(R.string.canvas_tab_code),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = codeContentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = id.gustyxganteng.agent.core.designsystem.M3OutlineVariant, thickness = 1.dp)

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
                                        ScrollableCanvasWebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                            WebView.setWebContentsDebuggingEnabled(true)
                                            isVerticalScrollBarEnabled = true
                                            isHorizontalScrollBarEnabled = true
                                            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                                            overScrollMode = View.OVER_SCROLL_ALWAYS
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
                                        val trimmed = currentContent.trim()
                                        val isDirectUrl = trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)
                                        if (isDirectUrl) {
                                            if (webView.tag != trimmed) {
                                                webView.tag = trimmed
                                                webView.loadUrl(trimmed)
                                            }
                                        } else {
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
                                    .background(id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLowest)
                                    .padding(horizontal = 10.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = lineNumbersText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    color = id.gustyxganteng.agent.core.designsystem.M3TextTertiary
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
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    color = id.gustyxganteng.agent.core.designsystem.M3TextPrimary
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = id.gustyxganteng.agent.core.designsystem.M3Primary
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = id.gustyxganteng.agent.core.designsystem.M3OutlineVariant, thickness = 1.dp)

            // Quick-Edit Prompt Bar (Bottom)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh,
                border = BorderStroke(1.dp, id.gustyxganteng.agent.core.designsystem.M3OutlineVariant),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = instructionText,
                        onValueChange = { instructionText = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.canvas_prompt_placeholder),
                                color = id.gustyxganteng.agent.core.designsystem.M3TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = id.gustyxganteng.agent.core.designsystem.M3TextPrimary, fontSize = 14.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = id.gustyxganteng.agent.core.designsystem.M3Primary
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
                            color = if (instructionText.isNotBlank()) id.gustyxganteng.agent.core.designsystem.M3Primary else id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHighest,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.cd_send),
                                    tint = if (instructionText.isNotBlank()) id.gustyxganteng.agent.core.designsystem.M3OnPrimary else id.gustyxganteng.agent.core.designsystem.M3TextSecondary,
                                    modifier = Modifier.size(17.dp)
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
            shape = RoundedCornerShape(28.dp),
            containerColor = id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh,
            titleContentColor = id.gustyxganteng.agent.core.designsystem.M3TextPrimary,
            textContentColor = id.gustyxganteng.agent.core.designsystem.M3TextSecondary,
            title = {
                Text(
                    text = stringResource(R.string.canvas_save_to_workspace),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                        color = id.gustyxganteng.agent.core.designsystem.M3TextSecondary
                    )

                    OutlinedTextField(
                        value = relativePath,
                        onValueChange = { relativePath = it },
                        label = { Text(stringResource(R.string.canvas_file_path_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = id.gustyxganteng.agent.core.designsystem.M3Primary,
                            unfocusedBorderColor = id.gustyxganteng.agent.core.designsystem.M3OutlineVariant,
                            focusedLabelColor = id.gustyxganteng.agent.core.designsystem.M3Primary,
                            unfocusedLabelColor = id.gustyxganteng.agent.core.designsystem.M3TextSecondary,
                            focusedTextColor = id.gustyxganteng.agent.core.designsystem.M3TextPrimary,
                            unfocusedTextColor = id.gustyxganteng.agent.core.designsystem.M3TextPrimary
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = id.gustyxganteng.agent.core.designsystem.M3Primary,
                        contentColor = id.gustyxganteng.agent.core.designsystem.M3OnPrimary,
                        disabledContainerColor = id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHighest,
                        disabledContentColor = id.gustyxganteng.agent.core.designsystem.M3TextSecondary
                    )
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.cancel), color = id.gustyxganteng.agent.core.designsystem.M3TextSecondary)
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
