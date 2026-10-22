package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiSparkleIcon
import id.gustyxganteng.agent.core.markdown.MarkdownContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ThinkingAccordion(
    reasoningContent: String?,
    thinkingDurationMs: Long?,
    isCurrentlyStreaming: Boolean = false,
    thinkingTokens: Int? = null,
    modifier: Modifier = Modifier
) {
    if (reasoningContent.isNullOrBlank() && !isCurrentlyStreaming) return

    var expanded by remember { mutableStateOf(false) }
    var liveElapsedSec by remember { mutableFloatStateOf(0.1f) }

    LaunchedEffect(isCurrentlyStreaming) {
        if (isCurrentlyStreaming) {
            val start = System.currentTimeMillis()
            while (isActive) {
                delay(100)
                liveElapsedSec = ((System.currentTimeMillis() - start) / 1000f).coerceAtLeast(0.1f)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sparkle_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "arrowRotation"
    )

    val durationSec = if (isCurrentlyStreaming) {
        String.format(java.util.Locale.US, "%.1f", liveElapsedSec)
    } else {
        val finalMs = thinkingDurationMs ?: (liveElapsedSec * 1000).toLong().coerceAtLeast(800)
        String.format(java.util.Locale.US, "%.1f", finalMs / 1000.0)
    }

    val effectiveTokens = if (thinkingTokens != null && thinkingTokens > 0) {
        thinkingTokens
    } else if (!reasoningContent.isNullOrBlank()) {
        (reasoningContent.length / 4).coerceAtLeast(1)
    } else {
        0
    }

    fun formatTokens(tokens: Int): String {
        return if (tokens >= 1000) {
            String.format(java.util.Locale.US, "%.1fk", tokens / 1000.0)
        } else {
            tokens.toString()
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded }
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                GeminiSparkleIcon(
                    size = 16.dp,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = if (isCurrentlyStreaming) Modifier.rotate(rotation) else Modifier
                )
                Spacer(Modifier.width(8.dp))

                val headerText = if (isCurrentlyStreaming) {
                    stringResource(R.string.thinking_in_progress)
                } else {
                    stringResource(R.string.thinking_done_no_detail)
                }

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(8.dp))

                // Expressive duration / token capsule pill
                run {
                    val badgeText = if (effectiveTokens > 0) {
                        "${durationSec}s • ${formatTokens(effectiveTokens)} tok"
                    } else {
                        "${durationSec}s"
                    }
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(arrowRotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    if (!reasoningContent.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
                            MarkdownContent(markdown = reasoningContent, modifier = Modifier.fillMaxWidth())
                        }
                    } else if (isCurrentlyStreaming) {
                        Text(
                            text = stringResource(R.string.thinking_analyzing_placeholder),
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
