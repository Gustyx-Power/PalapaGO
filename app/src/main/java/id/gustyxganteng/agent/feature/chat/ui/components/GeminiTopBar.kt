package id.gustyxganteng.agent.feature.chat.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiSparkleIcon
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tune
import id.gustyxganteng.agent.core.designsystem.PalapaBlue
import id.gustyxganteng.agent.core.designsystem.PalapaBorder
import id.gustyxganteng.agent.core.designsystem.PalapaGold
import id.gustyxganteng.agent.core.designsystem.PalapaSurface
import id.gustyxganteng.agent.core.model.AgentExecutionMode
import id.gustyxganteng.agent.domain.context.ContextMeterManager
import id.gustyxganteng.agent.domain.context.ContextMeterStatus

@Composable
fun GeminiTopBar(
    selectedModel: String,
    executionMode: AgentExecutionMode = AgentExecutionMode.FAST,
    contextStatus: ContextMeterStatus?,
    onOpenDrawer: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectExecutionMode: (AgentExecutionMode) -> Unit = {},
    onCompactContext: () -> Unit,
    onBranchSession: () -> Unit = {},
    onNewSession: () -> Unit = {},
    isExecutionLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showModelMenu by remember { mutableStateOf(false) }
    var showContextSheet by remember { mutableStateOf(false) }

    LaunchedEffect(isExecutionLocked) {
        if (isExecutionLocked) {
            showModelMenu = false
        }
    }

    val currentMode = executionMode

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Row 1: App Header Bar (Menu - Palapa GO - Baru)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = stringResource(R.string.topbar_menu),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.topbar_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = onNewSession,
                    colors = ButtonDefaults.textButtonColors(contentColor = PalapaGold)
                ) {
                    Text(
                        text = stringResource(R.string.topbar_new),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    )
                }
            }

            // Row 2: Sub-toolbar controls (Model Selector on Left, Context Gauge on Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Model Selector Pill (Left)
                Box {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PalapaSurface,
                        border = BorderStroke(1.dp, PalapaBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (isExecutionLocked) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.execution_mode_locked_toast),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    showModelMenu = true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedModel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = if (isExecutionLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isExecutionLocked) Icons.Rounded.Lock else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = if (isExecutionLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(if (isExecutionLocked) 14.dp else 18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        val menuItems = listOf(
                            Triple(AgentExecutionMode.FAST, R.string.model_fast_desc, Icons.Rounded.Bolt),
                            Triple(AgentExecutionMode.STANDARD, R.string.model_standard_desc, Icons.Rounded.Tune),
                            Triple(AgentExecutionMode.HIGH, R.string.model_high_desc, Icons.AutoMirrored.Rounded.TrendingUp),
                            Triple(AgentExecutionMode.SMART, R.string.model_smart_desc, Icons.Rounded.Psychology),
                            Triple(AgentExecutionMode.MAX, R.string.model_max_desc, Icons.Rounded.AutoAwesome)
                        )

                        menuItems.forEachIndexed { index, (mode, descRes, icon) ->
                            val isSelected = mode == currentMode
                            val itemTint = when (mode) {
                                AgentExecutionMode.FAST -> MaterialTheme.colorScheme.onSurfaceVariant
                                AgentExecutionMode.STANDARD -> MaterialTheme.colorScheme.primary
                                AgentExecutionMode.HIGH -> Color(0xFFB7F397)
                                AgentExecutionMode.SMART -> Color(0xFFD0BCFF)
                                AgentExecutionMode.MAX -> MaterialTheme.colorScheme.tertiary
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = itemTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(descRes),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectModel("agnes-3.0-flash")
                                    onSelectExecutionMode(mode)
                                    showModelMenu = false
                                }
                            )

                            if (index < menuItems.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                // Context Meter Pill (Right)
                val percent = contextStatus?.percentage ?: 0f
                val indicatorColor = contextStatus?.indicatorColor ?: PalapaBlue
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PalapaSurface,
                    border = BorderStroke(1.dp, PalapaBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (contextStatus != null) {
                                showContextSheet = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { (percent / 100f).coerceIn(0.04f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = indicatorColor,
                                trackColor = indicatorColor.copy(alpha = 0.25f),
                                strokeWidth = 2.2.dp,
                                strokeCap = StrokeCap.Round
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.0f", percent)}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (showContextSheet && contextStatus != null) {
                ContextDetailsBottomSheet(
                    status = contextStatus,
                    modelName = "PalapaGO Core",
                    onDismiss = { showContextSheet = false },
                    onCompactContext = {
                        showContextSheet = false
                        onCompactContext()
                    },
                    onBranchSession = {
                        showContextSheet = false
                        onBranchSession()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextDetailsBottomSheet(
    status: ContextMeterStatus,
    modelName: String,
    onDismiss: () -> Unit,
    onCompactContext: () -> Unit,
    onBranchSession: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = status.indicatorColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, status.indicatorColor.copy(alpha = 0.3f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PieChart,
                                contentDescription = null,
                                tint = status.indicatorColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.context_window_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.context_window_model, modelName),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Main Metric & Progress Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.context_tokens_used),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${ContextMeterManager.formatFullNumber(status.usedTokens)} tokens",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = status.indicatorColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, status.indicatorColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", status.percentage)}%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = status.indicatorColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Expressive Linear Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((status.percentage / 100f).coerceIn(0.01f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(status.indicatorColor)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stringResource(R.string.context_tokens_total)}: ${ContextMeterManager.formatTokenNumber(status.maxCapacity)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val remaining = (status.maxCapacity - status.usedTokens).coerceAtLeast(0)
                        Text(
                            text = "${stringResource(R.string.context_tokens_remaining)}: ${ContextMeterManager.formatTokenNumber(remaining)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = status.indicatorColor
                        )
                    }
                }
            }

            if (status.sessionTotalTokens > 0L) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KONSUMSI TOKEN SESI INI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${ContextMeterManager.formatFullNumber(status.sessionTotalTokens.toInt())} tokens",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Input", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = ContextMeterManager.formatTokenNumber(status.sessionPromptTokens.toInt()),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF64FFDA)
                                )
                            }
                            Column {
                                Text("Penalaran", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = ContextMeterManager.formatTokenNumber(status.sessionReasoningTokens.toInt()),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFFFD180)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Output", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = ContextMeterManager.formatTokenNumber(status.sessionCompletionTokens.toInt()),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE879F9)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Information Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.CallSplit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.context_branch_info),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Action Button 1: Cabangkan ke Obrolan Baru (Fresh Context Window)
            Button(
                onClick = onBranchSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.CallSplit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.context_branch_button),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Action Button 2: Padatkan Konteks Sesi Ini
            OutlinedButton(
                onClick = onCompactContext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.context_compact_button),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
