package id.gustyxganteng.agent.feature.workspace

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.database.entity.WorkspaceEntity
import id.gustyxganteng.agent.core.designsystem.GeminiActiveChipContainer
import id.gustyxganteng.agent.core.designsystem.GeminiActiveChipContent
import id.gustyxganteng.agent.core.designsystem.GeminiBackground
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSurface
import id.gustyxganteng.agent.core.designsystem.GeminiSurfaceVariant
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary
import id.gustyxganteng.agent.domain.tools.WorkspacePathResolver
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceManagerBottomSheet(
    currentWorkspacePath: String?,
    recentWorkspaces: List<WorkspaceEntity>,
    onDismiss: () -> Unit,
    onWorkspaceSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingSelectedPath by remember { mutableStateOf<String?>(null) }

    // Native Document Tree Folder Picker
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val resolvedPath = WorkspacePathResolver.resolveTreeUriToPath(context, treeUri)
            if (resolvedPath != null) {
                if (WorkspacePathResolver.hasAllFilesAccess()) {
                    onWorkspaceSelected(resolvedPath)
                    onDismiss()
                } else {
                    pendingSelectedPath = resolvedPath
                    showPermissionDialog = true
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GeminiSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = GeminiPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.workspace_manager_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GeminiTextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = GeminiTextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 1. Primary Button: Pick from Storage
            Button(
                onClick = { folderPicker.launch(null) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeminiPrimary,
                    contentColor = GeminiBackground
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.workspace_pick_folder),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))

            // 2. Secondary Button: Create New Project Folder
            OutlinedButton(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GeminiPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.workspace_create_new),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(20.dp))

            // 3. Recent Workspaces
            Text(
                text = stringResource(R.string.workspace_recent_title),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = GeminiPrimary
            )
            Spacer(Modifier.height(8.dp))

            if (recentWorkspaces.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    items(recentWorkspaces) { ws ->
                        val isSelected = ws.rootPath == currentWorkspacePath
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GeminiActiveChipContainer else GeminiSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (WorkspacePathResolver.hasAllFilesAccess()) {
                                        onWorkspaceSelected(ws.rootPath)
                                        onDismiss()
                                    } else {
                                        pendingSelectedPath = ws.rootPath
                                        showPermissionDialog = true
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) GeminiActiveChipContent else GeminiTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ws.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) GeminiActiveChipContent else GeminiTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = ws.rootPath,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) GeminiActiveChipContent.copy(alpha = 0.8f) else GeminiTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GeminiSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.canvas_no_workspace),
                        style = MaterialTheme.typography.bodySmall,
                        color = GeminiTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Dialog Create New Project Folder
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = GeminiSurface,
            title = {
                Text(
                    text = stringResource(R.string.workspace_create_new),
                    color = GeminiTextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.workspace_folder_name_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = GeminiTextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text(stringResource(R.string.workspace_folder_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            val createdDir = WorkspacePathResolver.createDefaultProjectFolder(newFolderName)
                            val path = createdDir.absolutePath
                            showCreateDialog = false
                            if (WorkspacePathResolver.hasAllFilesAccess()) {
                                onWorkspaceSelected(path)
                                onDismiss()
                            } else {
                                pendingSelectedPath = path
                                showPermissionDialog = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiPrimary, contentColor = GeminiBackground)
                ) {
                    Text(stringResource(R.string.workspace_select_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(R.string.cancel), color = GeminiTextSecondary)
                }
            }
        )
    }

    // Permission Dialog for All Files Access
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = GeminiSurface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = GeminiPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.storage_permission_title),
                    color = GeminiTextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.storage_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GeminiTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        context.startActivity(WorkspacePathResolver.getAllFilesAccessIntent(context))
                        pendingSelectedPath?.let {
                            onWorkspaceSelected(it)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiPrimary, contentColor = GeminiBackground)
                ) {
                    Text(stringResource(R.string.storage_permission_grant))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel), color = GeminiTextSecondary)
                }
            }
        )
    }
}
