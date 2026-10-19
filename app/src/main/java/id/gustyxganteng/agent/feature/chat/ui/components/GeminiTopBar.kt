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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiSparkleIcon
import id.gustyxganteng.agent.core.designsystem.M3OnPrimary
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3Surface
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHighest
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLow
import id.gustyxganteng.agent.core.designsystem.M3Tertiary
import id.gustyxganteng.agent.core.designsystem.M3TertiaryContainer
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tune
import id.gustyxganteng.agent.core.model.AgentExecutionMode
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
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
    isExecutionLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showModelMenu by remember { mutableStateOf(false) }

    LaunchedEffect(isExecutionLocked) {
        if (isExecutionLocked) {
            showModelMenu = false
        }
    }

    val currentMode = executionMode

    val (badgeBg, badgeFg) = when (currentMode) {
        AgentExecutionMode.FAST -> Pair(M3SurfaceContainerHighest, M3TextSecondary)
        AgentExecutionMode.STANDARD -> Pair(M3Primary.copy(alpha = 0.2f), M3Primary)
        AgentExecutionMode.HIGH -> Pair(Color(0xFF2E4D27), Color(0xFFB7F397))
        AgentExecutionMode.SMART -> Pair(Color(0xFF3B3366), Color(0xFFD0BCFF))
        AgentExecutionMode.MAX -> Pair(M3TertiaryContainer, M3Tertiary)
    }

    Surface(
        color = M3Surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation Drawer Toggle with Expressive Tonal Circle Button
                Surface(
                    shape = CircleShape,
                    color = M3SurfaceContainerHigh,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.size(38.dp)
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(R.string.topbar_menu),
                            tint = M3TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Model Selector Pill (Center - M3 Expressive Pill)
                Box {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isExecutionLocked) M3SurfaceContainerHigh.copy(alpha = 0.6f) else M3SurfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (isExecutionLocked) M3OutlineVariant.copy(alpha = 0.5f) else M3OutlineVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GeminiSparkleIcon(
                                size = 14.dp,
                                tint = if (isExecutionLocked) M3Primary.copy(alpha = 0.5f) else M3Primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = if (isExecutionLocked) M3TextPrimary.copy(alpha = 0.6f) else M3TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isExecutionLocked) badgeBg.copy(alpha = 0.5f) else badgeBg
                            ) {
                                Text(
                                    text = currentMode.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp
                                    ),
                                    color = if (isExecutionLocked) badgeFg.copy(alpha = 0.6f) else badgeFg,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExecutionLocked) Icons.Rounded.Lock else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = if (isExecutionLocked) M3TextSecondary.copy(alpha = 0.5f) else M3TextSecondary,
                                modifier = Modifier.size(if (isExecutionLocked) 14.dp else 18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = M3SurfaceContainerHigh,
                        border = BorderStroke(1.dp, M3OutlineVariant)
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
                                AgentExecutionMode.FAST -> M3TextSecondary
                                AgentExecutionMode.STANDARD -> M3Primary
                                AgentExecutionMode.HIGH -> Color(0xFFB7F397)
                                AgentExecutionMode.SMART -> Color(0xFFD0BCFF)
                                AgentExecutionMode.MAX -> M3Tertiary
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
                                            color = if (isSelected) M3Primary else M3TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = M3Primary,
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
                                HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                // Context Meter Pill (Right side - M3 Expressive micro circular gauge)
                if (contextStatus != null) {
                    var showContextSheet by remember { mutableStateOf(false) }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = M3SurfaceContainerHigh,
                        border = BorderStroke(1.dp, M3OutlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { showContextSheet = true }
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
                                    progress = { (contextStatus.percentage / 100f).coerceIn(0.04f, 1f) },
                                    modifier = Modifier.fillMaxSize(),
                                    color = contextStatus.indicatorColor,
                                    trackColor = contextStatus.indicatorColor.copy(alpha = 0.2f),
                                    strokeWidth = 2.2.dp,
                                    strokeCap = StrokeCap.Round
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${ContextMeterManager.formatTokenNumber(contextStatus.usedTokens)} • ${String.format(java.util.Locale.US, "%.1f", contextStatus.percentage)}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = M3TextPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (showContextSheet) {
                        ContextDetailsBottomSheet(
                            status = contextStatus,
                            modelName = "PalapaGO Core",
                            onDismiss = { showContextSheet = false },
                            onCompactContext = {
                                showContextSheet = false
                                onCompactContext()
                            }
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(38.dp))
                }
            }
            HorizontalDivider(color = M3OutlineVariant, thickness = 1.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextDetailsBottomSheet(
    status: ContextMeterStatus,
    modelName: String,
    onDismiss: () -> Unit,
    onCompactContext: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = M3SurfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(M3OutlineVariant)
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
                            color = M3TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.context_window_model, modelName),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = M3TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = M3TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Main Metric & Progress Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = M3SurfaceContainerHigh,
                border = BorderStroke(1.dp, M3OutlineVariant),
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
                                color = M3TextSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${ContextMeterManager.formatFullNumber(status.usedTokens)} tokens",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = M3TextPrimary
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
                            .background(M3SurfaceContainerHighest)
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
                            color = M3TextSecondary
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

            Spacer(Modifier.height(14.dp))

            // Information Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = M3SurfaceContainerLow,
                border = BorderStroke(1.dp, M3OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = M3Primary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.context_compact_info),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        ),
                        color = M3TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action Button: Padatkan Konteks
            Button(
                onClick = onCompactContext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = M3Primary,
                    contentColor = M3OnPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
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

