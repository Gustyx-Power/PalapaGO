package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.M3Error
import id.gustyxganteng.agent.core.designsystem.M3OnError
import id.gustyxganteng.agent.core.designsystem.M3OnPrimary
import id.gustyxganteng.agent.core.designsystem.M3OnPrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHighest
import id.gustyxganteng.agent.core.designsystem.M3Tertiary
import id.gustyxganteng.agent.core.designsystem.M3TertiaryContainer
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiBottomCapsule(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    activeWorkspacePath: String?,
    isWebSearchActive: Boolean,
    isAcademicActive: Boolean = false,
    isYoloModeActive: Boolean,
    isStreaming: Boolean,
    attachedImages: List<String> = emptyList(),
    onRemoveAttachedImage: (Int) -> Unit = {},
    onOpenWorkspaceManager: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onToggleAcademic: () -> Unit = {},
    onOpenCanvas: () -> Unit,
    onToggleYolo: () -> Unit,
    onAttachImage: () -> Unit,
    onCaptureCamera: () -> Unit = {},
    onSend: () -> Unit,
    onStopStreaming: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = M3SurfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, M3OutlineVariant),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 6.dp, end = 6.dp)
        ) {
            // Expressive Action Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
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
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(16.dp),
                        label = {
                            Text(
                                text = wsLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                fontWeight = if (activeWorkspacePath != null) FontWeight.SemiBold else FontWeight.Normal
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
                            containerColor = M3SurfaceContainer,
                            labelColor = M3TextSecondary,
                            iconColor = M3TextSecondary,
                            selectedContainerColor = M3PrimaryContainer,
                            selectedLabelColor = M3OnPrimaryContainer,
                            selectedLeadingIconColor = M3OnPrimaryContainer
                        ),
                        border = BorderStroke(1.dp, if (activeWorkspacePath != null) M3Primary.copy(alpha = 0.4f) else M3OutlineVariant)
                    )
                }

                // Web Search Chip
                item {
                    FilterChip(
                        selected = isWebSearchActive,
                        onClick = onToggleWebSearch,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(16.dp),
                        label = {
                            Text(
                                text = stringResource(R.string.chip_web_search),
                                fontSize = 12.sp,
                                fontWeight = if (isWebSearchActive) FontWeight.SemiBold else FontWeight.Normal
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
                            containerColor = M3SurfaceContainer,
                            labelColor = M3TextSecondary,
                            iconColor = M3TextSecondary,
                            selectedContainerColor = M3PrimaryContainer,
                            selectedLabelColor = M3OnPrimaryContainer,
                            selectedLeadingIconColor = M3OnPrimaryContainer
                        ),
                        border = BorderStroke(1.dp, if (isWebSearchActive) M3Primary.copy(alpha = 0.4f) else M3OutlineVariant)
                    )
                }

                // Academic Research Chip
                item {
                    FilterChip(
                        selected = isAcademicActive,
                        onClick = onToggleAcademic,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(16.dp),
                        label = {
                            Text(
                                text = stringResource(R.string.chip_academic),
                                fontSize = 12.sp,
                                fontWeight = if (isAcademicActive) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = M3SurfaceContainer,
                            labelColor = M3TextSecondary,
                            iconColor = M3TextSecondary,
                            selectedContainerColor = M3PrimaryContainer,
                            selectedLabelColor = M3OnPrimaryContainer,
                            selectedLeadingIconColor = M3OnPrimaryContainer
                        ),
                        border = BorderStroke(1.dp, if (isAcademicActive) M3Primary.copy(alpha = 0.4f) else M3OutlineVariant)
                    )
                }

                // Canvas Scratchpad Chip
                item {
                    FilterChip(
                        selected = false,
                        onClick = onOpenCanvas,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(16.dp),
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
                            containerColor = M3SurfaceContainer,
                            labelColor = M3TextSecondary,
                            iconColor = M3TextSecondary
                        ),
                        border = BorderStroke(1.dp, M3OutlineVariant)
                    )
                }

                // YOLO Mode Chip (Tertiary warm accent)
                item {
                    FilterChip(
                        selected = isYoloModeActive,
                        onClick = onToggleYolo,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(16.dp),
                        label = {
                            Text(
                                text = stringResource(R.string.chip_yolo),
                                fontSize = 12.sp,
                                fontWeight = if (isYoloModeActive) FontWeight.Bold else FontWeight.Normal
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
                            containerColor = M3SurfaceContainer,
                            labelColor = M3TextSecondary,
                            iconColor = M3TextSecondary,
                            selectedContainerColor = M3TertiaryContainer,
                            selectedLabelColor = M3Tertiary,
                            selectedLeadingIconColor = M3Tertiary
                        ),
                        border = BorderStroke(1.dp, if (isYoloModeActive) M3Tertiary.copy(alpha = 0.5f) else M3OutlineVariant)
                    )
                }
            }

            // Attached Images Thumbnail Preview Row
            if (attachedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(attachedImages) { index, base64Str ->
                        val bitmap = remember(base64Str) {
                            try {
                                val cleanBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
                                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(M3SurfaceContainerHighest)
                                .border(1.dp, M3OutlineVariant, RoundedCornerShape(14.dp))
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Lampiran gambar $index",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                    .clickable { onRemoveAttachedImage(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus gambar",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Input Row (Expressive Command Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment Button (Plus icon)
                Surface(
                    shape = CircleShape,
                    color = M3SurfaceContainer,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = onAttachImage,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.cd_attach),
                            tint = M3TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                // Camera Button
                Surface(
                    shape = CircleShape,
                    color = M3SurfaceContainer,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = onCaptureCamera,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = stringResource(R.string.cd_camera),
                            tint = M3TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Text Input
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = M3TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    maxLines = 6,
                    cursorBrush = SolidColor(M3Primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.input_placeholder),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = M3TextSecondary.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                // Morphing Send / Stop FAB with spring physics
                val canSend = inputText.isNotBlank() || attachedImages.isNotEmpty()
                val targetBtnColor = when {
                    isStreaming -> M3Error
                    canSend -> M3Primary
                    else -> M3SurfaceContainerHighest
                }
                val targetContentColor = when {
                    isStreaming -> M3OnError
                    canSend -> M3OnPrimary
                    else -> M3TextSecondary.copy(alpha = 0.5f)
                }

                val buttonColor by animateColorAsState(
                    targetValue = targetBtnColor,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "btnColor"
                )
                val iconColor by animateColorAsState(
                    targetValue = targetContentColor,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "iconColor"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isStreaming || canSend) 1f else 0.92f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "btnScale"
                )

                Surface(
                    shape = CircleShape,
                    color = buttonColor,
                    border = BorderStroke(1.dp, M3OutlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(44.dp)
                        .scale(scale)
                ) {
                    IconButton(
                        onClick = {
                            if (isStreaming) {
                                onStopStreaming()
                            } else if (canSend) {
                                onSend()
                            }
                        },
                        enabled = isStreaming || canSend,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isStreaming) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.cd_stop),
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.cd_send),
                                tint = iconColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
