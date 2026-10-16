package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Code
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiError
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSurface
import id.gustyxganteng.agent.core.designsystem.GeminiSurfaceVariant
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary

@Composable
fun ToolExecutionCard(
    toolName: String,
    arguments: String,
    outputContent: String? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val isError = outputContent != null && (
        outputContent.startsWith("Error:", ignoreCase = true) ||
        outputContent.contains("Akses ditolak", ignoreCase = true) ||
        outputContent.contains("Gagal", ignoreCase = true) ||
        outputContent.contains("timeout", ignoreCase = true)
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GeminiSurface,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = outputContent != null) { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tool Category Icon
                val toolIcon = when (toolName) {
                    "execute_shell" -> Icons.Rounded.Terminal
                    "list_directory", "read_file", "write_file", "apply_diff", "search_files" -> Icons.Rounded.FolderOpen
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

                // Tool Name & Short args
                Text(
                    text = "$toolName: ${arguments.take(45)}...",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = GeminiTextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                // Status Indicator: Loading vs Error vs Success
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

            // Expanded Output Log
            AnimatedVisibility(visible = expanded && outputContent != null) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GeminiSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = outputContent ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (isError) GeminiError else GeminiTextSecondary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
