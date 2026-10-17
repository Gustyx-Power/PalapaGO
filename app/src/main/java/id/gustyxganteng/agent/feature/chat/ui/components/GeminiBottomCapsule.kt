package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
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

@OptIn(ExperimentalMaterial3Api::class)
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GeminiSurfaceVariant),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
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
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            label = {
                                Text(
                                    text = wsLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
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
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            label = {
                                Text(
                                    text = stringResource(R.string.chip_web_search),
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
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
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            label = {
                                Text(
                                    text = stringResource(R.string.chip_canvas),
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.DashboardCustomize,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
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
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            label = {
                                Text(
                                    text = stringResource(R.string.chip_yolo),
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
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
            }

            // Attached Images Pill Row
            if (attachedImagesCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..attachedImagesCount) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GeminiSurfaceVariant
                        ) {
                            Text(
                                text = "Image $i",
                                style = MaterialTheme.typography.labelSmall,
                                color = GeminiTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Main Input Row
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment Button
                    IconButton(
                        onClick = onAttachImage,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircleOutline,
                            contentDescription = stringResource(R.string.cd_attach),
                            tint = GeminiTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Text Input
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputTextChanged,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = GeminiTextPrimary,
                            fontSize = 14.sp
                        ),
                        maxLines = 4,
                        cursorBrush = SolidColor(GeminiPrimary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.input_placeholder),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        color = GeminiTextSecondary.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(Modifier.width(6.dp))

                    // Send / Stop Button
                    if (isStreaming) {
                        IconButton(
                            onClick = onStopStreaming,
                            modifier = Modifier
                                .size(34.dp)
                                .background(GeminiError, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.cd_stop),
                                tint = GeminiBackground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onSend,
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (inputText.isNotBlank()) GeminiPrimary else GeminiSurfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.cd_send),
                                tint = if (inputText.isNotBlank()) GeminiBackground else GeminiTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
