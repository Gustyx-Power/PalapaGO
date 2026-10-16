package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiActiveChipContainer
import id.gustyxganteng.agent.core.designsystem.GeminiActiveChipContent
import id.gustyxganteng.agent.core.designsystem.GeminiBackground
import id.gustyxganteng.agent.core.designsystem.GeminiError
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSurface
import id.gustyxganteng.agent.core.designsystem.GeminiSurfaceVariant
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary
import java.io.File

@Composable
fun GeminiBottomCapsule(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    activeWorkspacePath: String?,
    isWebSearchActive: Boolean,
    isYoloModeActive: Boolean,
    isStreaming: Boolean,
    attachedImagesCount: Int,
    onOpenWorkspaceManager: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onOpenCanvas: () -> Unit,
    onToggleYolo: () -> Unit,
    onAttachImage: () -> Unit,
    onSend: () -> Unit,
    onStopStreaming: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = GeminiSurface,
        shape = RoundedCornerShape(28.dp),
        border = null,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
            // Top Row: Chips with LazyRow to prevent edge truncation
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Workspace Chip
                item {
                    val wsLabel = if (activeWorkspacePath != null) {
                        File(activeWorkspacePath).name.ifEmpty { stringResource(R.string.chip_workspace) }
                    } else {
                        stringResource(R.string.chip_select_folder)
                    }

                    FilterChip(
                        selected = activeWorkspacePath != null,
                        onClick = onOpenWorkspaceManager,
                        label = {
                            Text(
                                text = wsLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GeminiSurfaceVariant,
                            labelColor = GeminiTextSecondary,
                            iconColor = GeminiTextSecondary,
                            selectedContainerColor = GeminiActiveChipContainer,
                            selectedLabelColor = GeminiActiveChipContent,
                            selectedLeadingIconColor = GeminiActiveChipContent
                        ),
                        border = null
                    )
                }

                // Web Search Chip
                item {
                    FilterChip(
                        selected = isWebSearchActive,
                        onClick = onToggleWebSearch,
                        label = {
                            Text(
                                text = stringResource(R.string.chip_web_search),
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GeminiSurfaceVariant,
                            labelColor = GeminiTextSecondary,
                            iconColor = GeminiTextSecondary,
                            selectedContainerColor = GeminiActiveChipContainer,
                            selectedLabelColor = GeminiActiveChipContent,
                            selectedLeadingIconColor = GeminiActiveChipContent
                        ),
                        border = null
                    )
                }

                // Canvas Chip
                item {
                    FilterChip(
                        selected = false,
                        onClick = onOpenCanvas,
                        label = {
                            Text(
                                text = stringResource(R.string.chip_canvas),
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DashboardCustomize,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GeminiSurfaceVariant,
                            labelColor = GeminiTextSecondary,
                            iconColor = GeminiTextSecondary
                        ),
                        border = null
                    )
                }

                // YOLO Mode Chip
                item {
                    FilterChip(
                        selected = isYoloModeActive,
                        onClick = onToggleYolo,
                        label = {
                            Text(
                                text = stringResource(R.string.chip_yolo),
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GeminiSurfaceVariant,
                            labelColor = GeminiTextSecondary,
                            iconColor = GeminiTextSecondary,
                            selectedContainerColor = GeminiActiveChipContainer,
                            selectedLabelColor = GeminiActiveChipContent,
                            selectedLeadingIconColor = GeminiActiveChipContent
                        ),
                        border = null
                    )
                }
            }

            // Attached Images Pill Row
            if (attachedImagesCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..attachedImagesCount) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GeminiSurfaceVariant
                        ) {
                            Text(
                                text = "Image $i",
                                style = MaterialTheme.typography.labelSmall,
                                color = GeminiTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Main Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment Button
                IconButton(onClick = onAttachImage) {
                    Icon(
                        imageVector = Icons.Rounded.AddCircleOutline,
                        contentDescription = stringResource(R.string.cd_attach),
                        tint = GeminiTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Text Input
                TextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.input_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GeminiTextSecondary.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = GeminiTextPrimary,
                        unfocusedTextColor = GeminiTextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Send / Stop Button
                if (isStreaming) {
                    IconButton(
                        onClick = onStopStreaming,
                        modifier = Modifier
                            .size(42.dp)
                            .background(GeminiError, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.cd_stop),
                            tint = GeminiBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (inputText.isNotBlank()) GeminiPrimary else GeminiSurfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send),
                            tint = if (inputText.isNotBlank()) GeminiBackground else GeminiTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
