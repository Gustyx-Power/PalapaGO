package id.gustyxganteng.agent.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R


@Composable
fun DiffApprovalDialog(
    toolName: String,
    targetPath: String?,
    details: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.approval_diff_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.approval_action, toolName),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (targetPath != null) {
                    Text(
                        text = stringResource(R.string.approval_target, targetPath),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        RenderDiffText(details)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.approve), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.reject))
            }
        }
    )
}

@Composable
fun ShellApprovalDialog(
    command: String,
    workingDir: String?,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.approval_shell_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.approval_shell_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "$ $command",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.run), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun RenderDiffText(text: String) {
    Column {
        text.lines().forEach { line ->
            val (bgColor, textColor) = when {
                line.startsWith("+") || line.contains("GANTI DENGAN") -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) to Color(0xFF81C995)
                line.startsWith("-") -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.error
                else -> Color.Transparent to MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = textColor
                )
            }
        }
    }
}
