package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiError
import id.gustyxganteng.agent.core.designsystem.GeminiOutline
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSurface
import id.gustyxganteng.agent.core.designsystem.GeminiSurfaceVariant
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary
import org.json.JSONObject

private data class ToolCardDetails(
    val badge: String,
    val title: String,
    val subtitle: String? = null,
    val searchBlock: String? = null,
    val replaceBlock: String? = null,
    val isDiff: Boolean = false
)

private fun parseToolDetails(toolName: String, rawArgs: String): ToolCardDetails {
    if (!rawArgs.trim().startsWith("{")) {
        return ToolCardDetails(
            badge = toolName.uppercase(),
            title = rawArgs
        )
    }

    return try {
        val json = JSONObject(rawArgs)
        when (toolName) {
            "write_file" -> {
                val path = json.optString("path", "file")
                val content = json.optString("content", "")
                val lineCount = if (content.isEmpty()) 0 else content.lines().size
                ToolCardDetails(
                    badge = "BUAT FILE",
                    title = path,
                    subtitle = "+$lineCount baris",
                    replaceBlock = content
                )
            }
            "apply_diff" -> {
                val path = json.optString("path", "file")
                val searchBlock = json.optString("search_block", "")
                val replaceBlock = json.optString("replace_block", "")
                val delCount = if (searchBlock.isEmpty()) 0 else searchBlock.lines().size
                val addCount = if (replaceBlock.isEmpty()) 0 else replaceBlock.lines().size
                ToolCardDetails(
                    badge = "REFACTOR",
                    title = path,
                    subtitle = "-$delCount / +$addCount baris",
                    searchBlock = searchBlock,
                    replaceBlock = replaceBlock,
                    isDiff = true
                )
            }
            "execute_shell" -> {
                val cmd = json.optString("command", "")
                ToolCardDetails(
                    badge = "TERMINAL",
                    title = cmd
                )
            }
            "read_file" -> {
                val path = json.optString("path", "file")
                val start = json.optInt("start_line", -1)
                val end = json.optInt("end_line", -1)
                val sub = if (start > 0 && end > 0) "baris $start-$end" else null
                ToolCardDetails(
                    badge = "BACA",
                    title = path,
                    subtitle = sub
                )
            }
            "list_directory" -> {
                val path = json.optString("path", "").ifEmpty { "Root Workspace" }
                ToolCardDetails(
                    badge = "STRUKTUR",
                    title = path
                )
            }
            "search_files" -> {
                val query = json.optString("query", "")
                ToolCardDetails(
                    badge = "CARI KODE",
                    title = "'$query'"
                )
            }
            "web_search" -> {
                val query = json.optString("query", "")
                ToolCardDetails(
                    badge = "WEB SEARCH",
                    title = "'$query'"
                )
            }
            "read_webpage" -> {
                val url = json.optString("url", "")
                ToolCardDetails(
                    badge = "DOKUMENTASI",
                    title = url
                )
            }
            "open_canvas" -> {
                val title = json.optString("title", "Dokumen Canvas")
                ToolCardDetails(
                    badge = "CANVAS",
                    title = title
                )
            }
            else -> ToolCardDetails(
                badge = toolName.uppercase(),
                title = rawArgs.take(50)
            )
        }
    } catch (_: Exception) {
        ToolCardDetails(
            badge = toolName.uppercase(),
            title = rawArgs.take(50)
        )
    }
}

@Composable
fun ToolExecutionCard(
    toolName: String,
    arguments: String,
    outputContent: String? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val details = remember(toolName, arguments) { parseToolDetails(toolName, arguments) }

    val isError = outputContent != null && (
        outputContent.startsWith("Error:", ignoreCase = true) ||
        outputContent.contains("Akses ditolak", ignoreCase = true) ||
        outputContent.contains("Gagal", ignoreCase = true) ||
        outputContent.contains("timeout", ignoreCase = true)
    )

    val canExpand = outputContent != null || details.isDiff || !details.replaceBlock.isNullOrEmpty()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GeminiSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiOutline.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = canExpand) { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tool Category Icon
                val toolIcon = when (toolName) {
                    "execute_shell" -> Icons.Rounded.Terminal
                    "apply_diff" -> Icons.Rounded.Difference
                    "write_file" -> Icons.AutoMirrored.Rounded.NoteAdd
                    "read_file" -> Icons.Rounded.EditNote
                    "list_directory", "search_files" -> Icons.Rounded.FolderOpen
                    "web_search", "read_webpage" -> Icons.Rounded.Language
                    else -> Icons.Rounded.Code
                }

                Icon(
                    imageVector = toolIcon,
                    contentDescription = null,
                    tint = GeminiPrimary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                // Action Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = GeminiSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = details.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = GeminiPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Title & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = GeminiTextPrimary
                    )
                    details.subtitle?.let { sub ->
                        Text(
                            text = sub,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (sub.contains("-")) GeminiError else GeminiTextSecondary
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Status Indicator
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = GeminiPrimary
                        )
                    }
                    isError -> {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = stringResource(R.string.tool_execution_error, toolName),
                            tint = GeminiError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    outputContent != null -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircleOutline,
                            contentDescription = stringResource(R.string.tool_execution_success),
                            tint = GeminiPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expanded Visual Diff or Output Log
            AnimatedVisibility(visible = expanded && canExpand) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Visual Diff viewer for apply_diff
                    if (details.isDiff && details.searchBlock != null && details.replaceBlock != null) {
                        Text(
                            text = "Perubahan Kode (Diff):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GeminiTextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Removed Block
                        if (details.searchBlock.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2B1416),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5C1D1F)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .horizontalScroll(scrollState)
                                ) {
                                    details.searchBlock.lines().forEach { line ->
                                        Text(
                                            text = "- $line",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = GeminiError
                                        )
                                    }
                                }
                            }
                        }

                        // Added Block
                        if (details.replaceBlock.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF142918),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E4624)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .horizontalScroll(scrollState)
                                ) {
                                    details.replaceBlock.lines().forEach { line ->
                                        Text(
                                            text = "+ $line",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFF81C784)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                    }

                    // Raw output log
                    outputContent?.let { output ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GeminiSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val logScrollState = rememberScrollState()
                            Text(
                                text = output,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (isError) GeminiError else GeminiTextSecondary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .horizontalScroll(logScrollState)
                            )
                        }
                    }
                }
            }
        }
    }
}
