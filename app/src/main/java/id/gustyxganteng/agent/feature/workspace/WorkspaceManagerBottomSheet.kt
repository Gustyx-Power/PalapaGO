package id.gustyxganteng.agent.feature.workspace

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.database.entity.WorkspaceEntity
import id.gustyxganteng.agent.core.designsystem.M3OnPrimary
import id.gustyxganteng.agent.core.designsystem.M3OnPrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHighest
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLow
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.domain.tools.WorkspacePathResolver

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
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = M3SurfaceContainerHigh,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = M3OutlineVariant,
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = M3PrimaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.35f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = M3Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.workspace_manager_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = M3TextPrimary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = M3SurfaceContainer,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = M3TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Primary Action Button (M3 Expressive Filled)
            Button(
                onClick = { folderPicker.launch(null) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = M3Primary,
                    contentColor = M3OnPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.workspace_pick_folder),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // Secondary Action Button (M3 Expressive Outlined)
            OutlinedButton(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = M3Primary),
                border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.5f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.workspace_create_new),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // Recent Workspaces Section
            Text(
                text = stringResource(R.string.workspace_recent_title),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = M3Primary
            )
            Spacer(Modifier.height(10.dp))

            if (recentWorkspaces.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(210.dp)) {
                    items(recentWorkspaces) { ws ->
                        val isSelected = ws.rootPath == currentWorkspacePath
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) M3PrimaryContainer else M3SurfaceContainer,
                            border = BorderStroke(1.dp, if (isSelected) M3Primary.copy(alpha = 0.6f) else M3OutlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
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
                                    tint = if (isSelected) M3OnPrimaryContainer else M3TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ws.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) M3OnPrimaryContainer else M3TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = ws.rootPath,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) M3OnPrimaryContainer.copy(alpha = 0.8f) else M3TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = M3OnPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = M3SurfaceContainerLow,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.canvas_no_workspace),
                        style = MaterialTheme.typography.bodySmall,
                        color = M3TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // Create New Folder Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = M3SurfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.workspace_create_new),
                    color = M3TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.workspace_folder_name_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = M3TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text(stringResource(R.string.workspace_folder_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = M3Primary,
                            unfocusedBorderColor = M3OutlineVariant,
                            focusedLabelColor = M3Primary,
                            unfocusedLabelColor = M3TextSecondary
                        )
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = M3Primary, contentColor = M3OnPrimary)
                ) {
                    Text(stringResource(R.string.workspace_select_button), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(R.string.cancel), color = M3TextSecondary)
                }
            }
        )
    }

    // All Files Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = M3SurfaceContainerHigh,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = M3Primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.storage_permission_title),
                    color = M3TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.storage_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = M3TextSecondary
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = M3Primary, contentColor = M3OnPrimary)
                ) {
                    Text(stringResource(R.string.storage_permission_grant), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel), color = M3TextSecondary)
                }
            }
        )
    }
}
