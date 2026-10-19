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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.core.model.PromptMode
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiBottomCapsule(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    promptMode: PromptMode,
    onSelectPromptMode: (PromptMode) -> Unit,
    activeWorkspacePath: String?,
    isWebSearchActive: Boolean,
    isStreaming: Boolean,
    attachedImages: List<String> = emptyList(),
    onRemoveAttachedImage: (Int) -> Unit = {},
    onOpenWorkspaceManager: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onOpenCanvas: () -> Unit,
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
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            // Gemini-Style Segmented Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PromptModeSegment(
                    icon = Icons.AutoMirrored.Rounded.Chat,
                    label = stringResource(R.string.prompt_mode_chat),
                    isSelected = promptMode == PromptMode.CHAT,
                    onClick = { onSelectPromptMode(PromptMode.CHAT) },
                    modifier = Modifier.weight(1f)
                )

                PromptModeSegment(
                    icon = Icons.Rounded.Terminal,
                    label = stringResource(R.string.prompt_mode_agent),
                    isSelected = promptMode == PromptMode.AGENT,
                    onClick = {
                        if (activeWorkspacePath == null) {
                            onOpenWorkspaceManager()
                        }
                        onSelectPromptMode(PromptMode.AGENT)
                    },
                    modifier = Modifier.weight(1f)
                )

                PromptModeSegment(
                    icon = Icons.Rounded.School,
                    label = stringResource(R.string.prompt_mode_academic),
                    isSelected = promptMode == PromptMode.ACADEMIC,
                    onClick = { onSelectPromptMode(PromptMode.ACADEMIC) },
                    modifier = Modifier.weight(1f)
                )

                PromptModeSegment(
                    icon = Icons.Rounded.DashboardCustomize,
                    label = stringResource(R.string.prompt_mode_canvas),
                    isSelected = false,
                    onClick = onOpenCanvas,
                    modifier = Modifier.weight(1f)
                )
            }

            // Agent Mode Mandatory Folder Indicator Banner
            if (promptMode == PromptMode.AGENT) {
                Spacer(Modifier.height(4.dp))
                val hasWorkspace = !activeWorkspacePath.isNullOrBlank()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasWorkspace) M3PrimaryContainer.copy(alpha = 0.45f) else Color(0xFF3E1C1C),
                    border = BorderStroke(
                        1.dp,
                        if (hasWorkspace) M3Primary.copy(alpha = 0.4f) else Color(0xFFF87171).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenWorkspaceManager() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = if (hasWorkspace) M3Primary else Color(0xFFF87171),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        val folderText = if (hasWorkspace) {
                            File(activeWorkspacePath!!).name.ifEmpty { activeWorkspacePath }
                        } else {
                            stringResource(R.string.prompt_mode_agent_requires_folder)
                        }
                        Text(
                            text = folderText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = if (hasWorkspace) M3TextPrimary else Color(0xFFFCA5A5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (hasWorkspace) "Ubah" else "Pilih Sekarang",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (hasWorkspace) M3Primary else Color(0xFFF87171)
                        )
                    }
                }
            }

            // Attached Images Thumbnail Preview Row
            if (attachedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
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

            // Main Input Command Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment Button (Plus icon)
                Surface(
                    shape = CircleShape,
                    color = M3SurfaceContainer,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.size(38.dp)
                ) {
                    IconButton(
                        onClick = onAttachImage,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.cd_attach),
                            tint = M3TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(Modifier.width(5.dp))

                // Camera Button
                Surface(
                    shape = CircleShape,
                    color = M3SurfaceContainer,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.size(38.dp)
                ) {
                    IconButton(
                        onClick = onCaptureCamera,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = stringResource(R.string.cd_camera),
                            tint = M3TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(5.dp))

                // Web Search Quick Toggle Button
                val webTint = if (isWebSearchActive) M3Primary else M3TextSecondary
                val webBg = if (isWebSearchActive) M3PrimaryContainer.copy(alpha = 0.5f) else M3SurfaceContainer
                Surface(
                    shape = CircleShape,
                    color = webBg,
                    border = BorderStroke(1.dp, if (isWebSearchActive) M3Primary.copy(alpha = 0.45f) else M3OutlineVariant),
                    modifier = Modifier.size(38.dp)
                ) {
                    IconButton(
                        onClick = onToggleWebSearch,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = stringResource(R.string.chip_web_search),
                            tint = webTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(7.dp))

                // Text Input with Mode-Adaptive Placeholder
                val currentPlaceholder = when (promptMode) {
                    PromptMode.CHAT -> stringResource(R.string.prompt_placeholder_chat)
                    PromptMode.AGENT -> stringResource(R.string.prompt_placeholder_agent)
                    PromptMode.ACADEMIC -> stringResource(R.string.prompt_placeholder_academic)
                    PromptMode.CANVAS -> stringResource(R.string.input_placeholder)
                }

                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = M3TextPrimary,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp
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
                                    text = currentPlaceholder,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                                    color = M3TextSecondary.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(Modifier.width(7.dp))

                // Morphing Send / Stop FAB
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
                        .size(42.dp)
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
                                modifier = Modifier.size(19.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.cd_send),
                                tint = iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptModeSegment(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) M3PrimaryContainer else M3SurfaceContainer
    val contentColor = if (isSelected) M3OnPrimaryContainer else M3TextSecondary
    val borderColor = if (isSelected) M3Primary.copy(alpha = 0.55f) else M3OutlineVariant.copy(alpha = 0.4f)
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = fontWeight,
                    fontSize = 11.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
