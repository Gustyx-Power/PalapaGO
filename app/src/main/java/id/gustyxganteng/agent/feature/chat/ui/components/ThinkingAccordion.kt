package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSparkleIcon
import id.gustyxganteng.agent.core.designsystem.GeminiSurfaceVariant
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ThinkingAccordion(
    reasoningContent: String?,
    thinkingDurationMs: Long?,
    isCurrentlyStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (reasoningContent.isNullOrBlank() && !isCurrentlyStreaming) return

    var expanded by remember { mutableStateOf(false) }

    // Live ticking timer for real-time streaming feedback
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

    // Spinning animation while actively thinking
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

    val durationSec = if (isCurrentlyStreaming) {
        String.format(java.util.Locale.US, "%.1f", liveElapsedSec)
    } else {
        val finalMs = thinkingDurationMs ?: (liveElapsedSec * 1000).toLong().coerceAtLeast(800)
        String.format(java.util.Locale.US, "%.1f", finalMs / 1000.0)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GeminiSurfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sparkle Icon (spinning if actively streaming thinking)
                GeminiSparkleIcon(
                    size = 16.dp,
                    tint = GeminiPrimary,
                    modifier = if (isCurrentlyStreaming) Modifier.rotate(rotation) else Modifier
                )
                Spacer(Modifier.width(8.dp))

                val headerText = if (isCurrentlyStreaming) {
                    "${stringResource(R.string.thinking_in_progress)} (${durationSec}s)"
                } else {
                    stringResource(R.string.thinking_done, durationSec)
                }

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = GeminiPrimary
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = GeminiTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    val detailText = if (!reasoningContent.isNullOrBlank()) {
                        reasoningContent
                    } else if (isCurrentlyStreaming) {
                        stringResource(R.string.thinking_analyzing_placeholder)
                    } else {
                        stringResource(R.string.thinking_done_no_detail)
                    }
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = GeminiTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
