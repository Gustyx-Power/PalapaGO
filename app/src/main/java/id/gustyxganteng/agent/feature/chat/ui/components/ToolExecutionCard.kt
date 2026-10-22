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
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Terminal
import id.gustyxganteng.agent.core.markdown.MarkdownImageCard
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
            "generate_image" -> {
                val prompt = json.optString("prompt", "Ilustrasi Visual AI")
                val aspect = json.optString("aspect_ratio", "1024x1024")
                ToolCardDetails(
                    badge = "IMAGE GEN",
                    title = prompt,
                    subtitle = "Agnes Image 2.5 Flash ($aspect)"
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

private fun extractImageUrlFromOutput(output: String?): String? {
    if (output.isNullOrBlank()) return null
    val mdMatch = Regex("""!\[.*?\]\(((?:https?://|data:image/)[^\s)]+)\)""").find(output)
    if (mdMatch != null) return mdMatch.groupValues[1]
    val directMatch = Regex("""(https?://platform-outputs\.agnes-ai\.space/[^\s)]+)""").find(output)
    if (directMatch != null) return directMatch.groupValues[1]
    val anyImgMatch = Regex("""(https?://[^\s)]+\.(?:png|jpg|jpeg|webp))""").find(output)
    return anyImgMatch?.groupValues?.get(1)
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
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
                    "generate_image" -> Icons.Rounded.Palette
                    else -> Icons.Rounded.Code
                }

                Icon(imageVector = toolIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(12.dp))

                // Title & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = details.badge.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    details.subtitle?.let { sub ->
                        Text(
                            text = sub,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (sub.contains("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    isError -> {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = stringResource(R.string.tool_execution_error, toolName),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    outputContent != null -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = stringResource(R.string.tool_execution_success),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            val imageUrl = remember(outputContent) { extractImageUrlFromOutput(outputContent) }

            // Tampilkan gambar visual langsung di dalam kartu (selalu terlihat saat selesai)
            if (imageUrl != null) {
                MarkdownImageCard(
                    alt = details.title,
                    url = imageUrl,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // Expanded Visual Diff or Output Log
            AnimatedVisibility(visible = expanded && canExpand) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (details.isDiff && details.searchBlock != null && details.replaceBlock != null) {
                        Text(
                            text = "Perubahan Kode (Diff):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                    // Raw output log (hanya tampil jika bukan gambar visual)
                    if (imageUrl == null) {
                        outputContent?.let { output ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val logScrollState = rememberScrollState()
                                Text(
                                    text = output,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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
}
