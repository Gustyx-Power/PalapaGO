package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.model.PromptMode
import java.io.File

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
    Surface(
        color = colors.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            ComposerAttachments(attachedImages, onRemoveAttachedImage)
            BasicTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { field ->
                    Box {
                        if (inputText.isEmpty()) Text(
                            stringResource(when (promptMode) {
                                PromptMode.AGENT -> R.string.prompt_placeholder_agent
                                PromptMode.ACADEMIC -> R.string.prompt_placeholder_academic
                                else -> R.string.prompt_placeholder_chat
                            }), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge
                        )
                        field()
                    }
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ComposerActions(isWebSearchActive, onAttachImage, onCaptureCamera, onToggleWebSearch, onOpenCanvas, onOpenWorkspaceManager)
                ComposerModePicker(promptMode, isStreaming) { mode ->
                    if (mode == PromptMode.CANVAS) onOpenCanvas() else {
                        onSelectPromptMode(mode)
                        if (mode == PromptMode.IMAGE_GEN) onOpenImageGen()
                    }
                }
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    onClick = if (isStreaming) onStopStreaming else onSend,
                    enabled = isStreaming || inputText.isNotBlank() || attachedImages.isNotEmpty(),
                    modifier = Modifier.size(48.dp),
                    shape = if (isStreaming) RoundedCornerShape(16.dp) else CircleShape
                ) {
                    Icon(if (isStreaming) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                        stringResource(if (isStreaming) R.string.cd_stop else R.string.cd_send))
                }
            }
            if (promptMode == PromptMode.AGENT) {
                TextButton(onClick = onOpenWorkspaceManager, modifier = Modifier.fillMaxWidth()) {
                    Text(activeWorkspacePath?.let { File(it).name.ifEmpty { it } }
                        ?: stringResource(R.string.prompt_mode_agent_requires_folder),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ComposerModePicker(mode: PromptMode, locked: Boolean, onSelect: (PromptMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf(
        PromptMode.CHAT to R.string.prompt_mode_chat,
        PromptMode.AGENT to R.string.prompt_mode_agent,
        PromptMode.ACADEMIC to R.string.prompt_mode_academic,
        PromptMode.IMAGE_GEN to R.string.prompt_mode_image_gen,
        PromptMode.CANVAS to R.string.prompt_mode_canvas
    )
    Box {
        TextButton(onClick = { expanded = true }, enabled = !locked) {
            Text(stringResource(modes.first { it.first == mode }.second))
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded && !locked, onDismissRequest = { expanded = false }) {
            modes.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    trailingIcon = { if (mode == value) Icon(Icons.Rounded.Check, null) },
                    onClick = { expanded = false; onSelect(value) }
                )
            }
        }
    }
}

@Composable
private fun ComposerActions(
    webEnabled: Boolean,
    onAttach: () -> Unit,
    onCamera: () -> Unit,
    onWeb: () -> Unit,
    onCanvas: () -> Unit,
    onWorkspace: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Rounded.Add, stringResource(R.string.cd_attach)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                R.string.cd_attach to onAttach,
                R.string.cd_camera to onCamera,
                R.string.chip_web_search to onWeb,
                R.string.prompt_mode_canvas to onCanvas,
                R.string.prompt_mode_agent_requires_folder to onWorkspace
            ).forEach { (label, action) ->
                DropdownMenuItem(text = { Text(stringResource(label)) },
                    trailingIcon = { if (label == R.string.chip_web_search && webEnabled) Icon(Icons.Rounded.Check, null) },
                    onClick = { expanded = false; action() })
            }
        }
    }
}

@Composable
private fun ComposerAttachments(images: List<String>, onRemove: (Int) -> Unit) {
    if (images.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(8.dp)) {
        itemsIndexed(images) { index, data ->
            val bitmap = remember(data) {
                runCatching {
                    val bytes = android.util.Base64.decode(data.substringAfter(','), android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }.getOrNull()
            }
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(20.dp))) {
                bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                FilledIconButton(onClick = { onRemove(index) }, modifier = Modifier.align(Alignment.TopEnd).size(48.dp)) {
                    Icon(Icons.Default.Close, stringResource(R.string.delete))
                }
            }
        }
    }
}
