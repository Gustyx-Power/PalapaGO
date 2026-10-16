package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiBackground
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSurfaceVariant
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary
import id.gustyxganteng.agent.domain.context.ContextMeterStatus

@Composable
fun GeminiTopBar(
    selectedModel: String,
    contextStatus: ContextMeterStatus?,
    onOpenDrawer: () -> Unit,
    onSelectModel: (String) -> Unit,
    onCompactContext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showModelMenu by remember { mutableStateOf(false) }

    Surface(
        color = GeminiBackground,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drawer toggle
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.topbar_menu),
                    tint = GeminiTextPrimary
                )
            }

            // Model Selector Pill (Center)
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GeminiSurfaceVariant,
                    modifier = Modifier.clickable { showModelMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedModel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = GeminiPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = GeminiPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.model_fast_desc, "agnes-3.0-flash")) },
                        onClick = {
                            onSelectModel("agnes-3.0-flash")
                            showModelMenu = false
                        }
                    )
                }
            }

            // Context Meter Pill (Right)
            if (contextStatus != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = contextStatus.indicatorColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, contextStatus.indicatorColor.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onCompactContext() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(contextStatus.indicatorColor, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = contextStatus.formattedDisplay,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GeminiTextPrimary
                        )
                    }
                }
            } else {
                Spacer(Modifier.width(40.dp))
            }
        }
    }
}
