package id.gustyxganteng.agent.feature.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSparkleIcon
import id.gustyxganteng.agent.core.designsystem.GeminiSurface
import id.gustyxganteng.agent.core.designsystem.GeminiSurfaceVariant
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary

@Composable
fun CanvasAuditInspectionBar(
    auditResult: CanvasAuditResult,
    onReaudit: () -> Unit,
    onFixWithAi: (instruction: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = GeminiSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GeminiSurfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GeminiSparkleIcon(size = 14.dp)
                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.canvas_audit_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = GeminiTextPrimary
                )

                Spacer(Modifier.width(8.dp))

                // Status Pill
                val statusBg = if (auditResult.isValid) Color(0xFF162D1D) else Color(0xFF332714)
                val statusColor = if (auditResult.isValid) Color(0xFF4EBA6F) else Color(0xFFE5A93C)
                val statusText = if (auditResult.isValid) {
                    stringResource(R.string.canvas_audit_verified)
                } else {
                    stringResource(R.string.canvas_audit_warning)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                // Token & Duration Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GeminiSurfaceVariant
                ) {
                    val durationText = String.format("%.1fs", auditResult.durationMs / 1000f)
                    Text(
                        text = stringResource(R.string.canvas_audit_tokens, auditResult.tokensUsed, durationText),
                        color = GeminiPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = GeminiTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expanded Inspection Body
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(
                        color = GeminiSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Audit Summary
                    Text(
                        text = auditResult.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = GeminiTextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // UI & Layout Section
                    Text(
                        text = stringResource(R.string.canvas_audit_ui_title),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GeminiPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    auditResult.uiChecks.forEach { check ->
                        AuditCheckRow(check = check)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Functionality Section
                    Text(
                        text = stringResource(R.string.canvas_audit_func_title),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GeminiPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    auditResult.functionChecks.forEach { check ->
                        AuditCheckRow(check = check)
                    }

                    // Console Logs Section if available
                    if (auditResult.consoleLogs.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.canvas_audit_console_title),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GeminiPrimary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GeminiSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                auditResult.consoleLogs.take(6).forEach { log ->
                                    Text(
                                        text = log,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (log.contains("error", ignoreCase = true)) Color(0xFFE05252) else GeminiTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onReaudit,
                            border = BorderStroke(1.dp, GeminiSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GeminiTextSecondary)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.canvas_audit_rescan_button), fontSize = 11.sp)
                        }

                        if (!auditResult.isValid) {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val failedItems = (auditResult.uiChecks + auditResult.functionChecks)
                                        .filter { !it.passed }
                                        .joinToString(", ") { "${it.title}: ${it.detail}" }
                                    onFixWithAi("Perbaiki masalah berikut pada dokumen Canvas ini: $failedItems")
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GeminiPrimary)
                            ) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.canvas_audit_fix_button), fontSize = 11.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditCheckRow(check: CanvasAuditCheck) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (check.passed) Icons.Rounded.CheckCircleOutline else Icons.Default.Warning,
            contentDescription = null,
            tint = if (check.passed) Color(0xFF4EBA6F) else Color(0xFFE5A93C),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = check.title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = GeminiTextPrimary,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = check.detail,
            style = MaterialTheme.typography.bodySmall,
            color = GeminiTextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
