package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Error
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.M3Error
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLowest
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.core.util.JsonRepairUtil
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
    val sanitized = JsonRepairUtil.repairOrSanitize(rawArgs)
    if (!sanitized.trim().startsWith("{")) {
        return ToolCardDetails(
            badge = toolName.uppercase(),
            title = rawArgs
        )
    }

    return try {
        val json = JSONObject(sanitized)
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
        shape = RoundedCornerShape(16.dp),
        color = M3SurfaceContainer,
        border = BorderStroke(1.dp, M3OutlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = canExpand) { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tool Category Icon container (M3 Expressive Squircle)
                val toolIcon = when (toolName) {
                    "execute_shell" -> Icons.Rounded.Terminal
                    "apply_diff" -> Icons.Rounded.Difference
                    "write_file" -> Icons.AutoMirrored.Rounded.NoteAdd
                    "read_file" -> Icons.Rounded.EditNote
                    "list_directory", "search_files" -> Icons.Rounded.FolderOpen
                    "web_search", "read_webpage" -> Icons.Rounded.Language
                    else -> Icons.Rounded.Code
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = M3PrimaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.3f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = toolIcon,
                            contentDescription = null,
                            tint = M3Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Action Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = M3SurfaceContainerHigh,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = details.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = M3Primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Title & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = M3TextPrimary
                    )
                    details.subtitle?.let { sub ->
                        Text(
                            text = sub,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (sub.contains("-")) M3Error else M3TextSecondary
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
                            color = M3Primary
                        )
                    }
                    isError -> {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = stringResource(R.string.tool_execution_error, toolName),
                            tint = M3Error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    outputContent != null -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = stringResource(R.string.tool_execution_success),
                            tint = M3Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expanded Visual Diff or Output Log
            AnimatedVisibility(visible = expanded && canExpand) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (details.isDiff && details.searchBlock != null && details.replaceBlock != null) {
                        Text(
                            text = "Perubahan Kode (Diff):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = M3TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Removed Block
                        if (details.searchBlock.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF261012),
                                border = BorderStroke(1.dp, Color(0xFF4A181A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .horizontalScroll(scrollState)
                                ) {
                                    details.searchBlock.lines().forEach { line ->
                                        Text(
                                            text = "- $line",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            color = Color(0xFFFFB4AB)
                                        )
                                    }
                                }
                            }
                        }

                        // Added Block
                        if (details.replaceBlock.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF102615),
                                border = BorderStroke(1.dp, Color(0xFF184221)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .horizontalScroll(scrollState)
                                ) {
                                    details.replaceBlock.lines().forEach { line ->
                                        Text(
                                            text = "+ $line",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            color = Color(0xFF81C995)
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
                            shape = RoundedCornerShape(10.dp),
                            color = M3SurfaceContainerLowest,
                            border = BorderStroke(1.dp, M3OutlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val logScrollState = rememberScrollState()
                            Text(
                                text = output,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = if (isError) M3Error else M3TextSecondary,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .horizontalScroll(logScrollState)
                            )
                        }
                    }
                }
            }
        }
    }
}
