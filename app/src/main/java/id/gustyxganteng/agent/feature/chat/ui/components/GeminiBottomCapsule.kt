package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import id.gustyxganteng.agent.core.designsystem.PalapaBlue
import id.gustyxganteng.agent.core.designsystem.PalapaBlueContainer
import id.gustyxganteng.agent.core.designsystem.PalapaBlueOnContainer
import id.gustyxganteng.agent.core.designsystem.PalapaBorder
import id.gustyxganteng.agent.core.designsystem.PalapaGold
import id.gustyxganteng.agent.core.designsystem.PalapaGoldBorder
import id.gustyxganteng.agent.core.designsystem.PalapaGoldButton
import id.gustyxganteng.agent.core.designsystem.PalapaGoldContainer
import id.gustyxganteng.agent.core.designsystem.PalapaOnGoldButton
import id.gustyxganteng.agent.core.designsystem.PalapaSurface
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
    onOpenImageGen: () -> Unit = {},
    onAttachImage: () -> Unit,
    onCaptureCamera: () -> Unit = {},
    onSend: () -> Unit,
    onStopStreaming: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val canSend = inputText.isNotBlank() || attachedImages.isNotEmpty()

    var showModeSheet by remember { mutableStateOf(false) }
    var showAttachmentsSheet by remember { mutableStateOf(false) }

    Surface(
        color = PalapaSurface,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, PalapaBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
            ComposerAttachments(attachedImages, onRemoveAttachedImage)

            BasicTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.onSurface,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(PalapaGold),
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { innerField ->
                    Box {
                        if (inputText.isEmpty()) {
                            Text(
                                text = stringResource(
                                    when (promptMode) {
                                        PromptMode.AGENT -> R.string.prompt_placeholder_agent
                                        PromptMode.ACADEMIC -> R.string.prompt_placeholder_academic
                                        else -> R.string.prompt_placeholder_chat
                                    }
                                ),
                                color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                            )
                        }
                        innerField()
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular (+) Button (Foto 2 modal trigger)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF282D36),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isStreaming) { showAttachmentsSheet = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.modal_attachments_header),
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Prompt Mode Picker Pill (Foto 1 modal trigger)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PalapaGoldContainer,
                    border = BorderStroke(1.dp, PalapaGoldBorder.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = !isStreaming) { showModeSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(promptMode.titleRes),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = PalapaGold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = PalapaGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Quick Web Search Toggle Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isWebSearchActive) PalapaBlueContainer else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggleWebSearch() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = stringResource(R.string.chip_web_search),
                            tint = if (isWebSearchActive) PalapaBlue else Color(0xFF717682),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isWebSearchActive) stringResource(R.string.composer_web_active) else "Web",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isWebSearchActive) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.5.sp
                            ),
                            color = if (isWebSearchActive) PalapaBlueOnContainer else Color(0xFF8A8F9C)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Kirim / Stop Button
                if (isStreaming) {
                    Button(
                        onClick = onStopStreaming,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.cd_stop),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.cd_stop),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                } else {
                    Button(
                        onClick = onSend,
                        enabled = canSend,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PalapaGoldButton,
                            contentColor = PalapaOnGoldButton,
                            disabledContainerColor = Color(0xFF282D36),
                            disabledContentColor = Color(0xFF676C77)
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.composer_send),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        )
                    }
                }
            }

            if (promptMode == PromptMode.AGENT) {
                TextButton(
                    onClick = onOpenWorkspaceManager,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeWorkspacePath?.let { File(it).name.ifEmpty { it } }
                            ?: stringResource(R.string.prompt_mode_agent_requires_folder),
                        color = PalapaGold,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showModeSheet) {
        ModePickerBottomSheet(
            currentMode = promptMode,
            activeWorkspacePath = activeWorkspacePath,
            onSelectMode = { selectedMode ->
                if (selectedMode == PromptMode.CANVAS) {
                    onOpenCanvas()
                } else {
                    onSelectPromptMode(selectedMode)
                    if (selectedMode == PromptMode.IMAGE_GEN) {
                        onOpenImageGen()
                    }
                }
            },
            onDismiss = { showModeSheet = false }
        )
    }

    if (showAttachmentsSheet) {
        AttachmentsToolsBottomSheet(
            isWebSearchActive = isWebSearchActive,
            activeWorkspacePath = activeWorkspacePath,
            onAttachImage = onAttachImage,
            onCaptureCamera = onCaptureCamera,
            onToggleWebSearch = onToggleWebSearch,
            onOpenCanvas = onOpenCanvas,
            onOpenWorkspaceManager = onOpenWorkspaceManager,
            onDismiss = { showAttachmentsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModePickerBottomSheet(
    currentMode: PromptMode,
    activeWorkspacePath: String?,
    onSelectMode: (PromptMode) -> Unit,
    onDismiss: () -> Unit
) {
    val modes = listOf(
        Pair(PromptMode.CHAT, Icons.Rounded.ChatBubbleOutline),
        Pair(PromptMode.AGENT, Icons.Rounded.SmartToy),
        Pair(PromptMode.ACADEMIC, Icons.Rounded.School),
        Pair(PromptMode.IMAGE_GEN, Icons.Rounded.Image),
        Pair(PromptMode.CANVAS, Icons.AutoMirrored.Rounded.ViewQuilt)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PalapaSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(PalapaBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.modal_select_mode_header),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF8E93A0),
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 10.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                modes.forEach { (mode, icon) ->
                    val isSelected = currentMode == mode
                    ModePickerItem(
                        icon = icon,
                        title = stringResource(mode.titleRes),
                        description = stringResource(mode.descRes),
                        isSelected = isSelected,
                        isLocked = mode == PromptMode.AGENT && activeWorkspacePath == null,
                        onClick = {
                            onSelectMode(mode)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModePickerItem(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PalapaGoldContainer else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, PalapaGoldBorder.copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PalapaGold else Color(0xFF9EA3AE),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (isSelected) PalapaGold else Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp
                    ),
                    color = if (isSelected) Color(0xFFD6CAAF) else Color(0xFF8E93A0)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = PalapaGold,
                    modifier = Modifier.size(18.dp)
                )
            } else if (isLocked) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color(0xFF717682),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentsToolsBottomSheet(
    isWebSearchActive: Boolean,
    activeWorkspacePath: String?,
    onAttachImage: () -> Unit,
    onCaptureCamera: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onOpenCanvas: () -> Unit,
    onOpenWorkspaceManager: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PalapaSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(PalapaBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.modal_attachments_header),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF8E93A0),
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 10.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AttachmentActionItem(
                    icon = Icons.Rounded.AttachFile,
                    title = stringResource(R.string.tool_attachment_title),
                    description = stringResource(R.string.tool_attachment_desc),
                    isSelected = false,
                    onClick = {
                        onAttachImage()
                        onDismiss()
                    }
                )

                AttachmentActionItem(
                    icon = Icons.Rounded.CameraAlt,
                    title = stringResource(R.string.tool_camera_title),
                    description = stringResource(R.string.tool_camera_desc),
                    isSelected = false,
                    onClick = {
                        onCaptureCamera()
                        onDismiss()
                    }
                )

                AttachmentActionItem(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.tool_web_search_title),
                    description = stringResource(R.string.tool_web_search_desc),
                    isSelected = isWebSearchActive,
                    onClick = {
                        onToggleWebSearch()
                    }
                )

                AttachmentActionItem(
                    icon = Icons.AutoMirrored.Rounded.ViewQuilt,
                    title = stringResource(R.string.tool_canvas_title),
                    description = stringResource(R.string.tool_canvas_desc),
                    isSelected = false,
                    onClick = {
                        onOpenCanvas()
                        onDismiss()
                    }
                )

                // Bottom Workspace Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF14171E),
                    border = BorderStroke(1.dp, PalapaBorder.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = PalapaGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            val workspaceText = if (activeWorkspacePath != null) {
                                val folderName = File(activeWorkspacePath).name.ifEmpty { activeWorkspacePath }
                                "Folder: $folderName"
                            } else {
                                stringResource(R.string.prompt_mode_agent_requires_folder)
                            }
                            Text(
                                text = workspaceText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                color = Color(0xFF8E93A0),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                onOpenWorkspaceManager()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PalapaGoldButton,
                                contentColor = PalapaOnGoldButton
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    if (activeWorkspacePath != null) R.string.workspace_banner_button_change
                                    else R.string.workspace_banner_button
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentActionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PalapaGoldContainer else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, PalapaGoldBorder.copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PalapaGold else Color(0xFF9EA3AE),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (isSelected) PalapaGold else Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp
                    ),
                    color = if (isSelected) Color(0xFFD6CAAF) else Color(0xFF8E93A0)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = PalapaGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ComposerAttachments(images: List<String>, onRemove: (Int) -> Unit) {
    if (images.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    ) {
        itemsIndexed(images) { index, data ->
            val bitmap = remember(data) {
                runCatching {
                    val bytes = android.util.Base64.decode(data.substringAfter(','), android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }.getOrNull()
            }
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                bitmap?.let {
                    Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                FilledIconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        stringResource(R.string.delete),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
