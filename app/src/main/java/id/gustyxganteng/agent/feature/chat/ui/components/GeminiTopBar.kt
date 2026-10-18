package id.gustyxganteng.agent.feature.chat.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.domain.context.ContextMeterManager
import id.gustyxganteng.agent.domain.context.ContextMeterStatus

@Composable
fun GeminiTopBar(
    selectedModel: String,
    claudeMaxMode: Boolean = true,
    contextStatus: ContextMeterStatus?,
    onOpenDrawer: () -> Unit,
    onSelectModel: (String) -> Unit,
    onToggleClaudeMaxMode: (Boolean) -> Unit = {},
    onCompactContext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showModelMenu by remember { mutableStateOf(false) }

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
                        color = M3SurfaceContainerHigh,
                        border = BorderStroke(1.dp, M3OutlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { showModelMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GeminiSparkleIcon(size = 14.dp, tint = M3Primary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = selectedModel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = M3TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (claudeMaxMode) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = M3TertiaryContainer
                                ) {
                                    Text(
                                        text = "MAX",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp
                                        ),
                                        color = M3Tertiary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = M3TextSecondary,
                                modifier = Modifier.size(18.dp)
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
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = M3Tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        stringResource(R.string.model_claude_max_desc, "agnes-3.0-flash"),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = {
                                onSelectModel("agnes-3.0-flash")
                                onToggleClaudeMaxMode(true)
                                showModelMenu = false
                            }
                        )
                        HorizontalDivider(color = M3OutlineVariant)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = M3Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        stringResource(R.string.model_standard_desc, "agnes-3.0-flash"),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = {
                                onSelectModel("agnes-3.0-flash")
                                onToggleClaudeMaxMode(false)
                                showModelMenu = false
                            }
                        )
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
                            modelName = selectedModel,
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

