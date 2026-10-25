package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.gustyxganteng.agent.core.designsystem.PalapaBlue
import id.gustyxganteng.agent.core.designsystem.PalapaGold
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun PalapaEdgeShimmer(
    triggerKey: Any?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val animProgress = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        animProgress.snapTo(0f)
        glowAlpha.snapTo(0f)

        coroutineScope {
            launch {
                glowAlpha.animateTo(1f, animationSpec = tween(350, easing = LinearEasing))
                glowAlpha.animateTo(0f, animationSpec = tween(850, delayMillis = 1100, easing = FastOutSlowInEasing))
            }
            launch {
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2100, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    if (glowAlpha.value <= 0.01f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val alpha = glowAlpha.value
        val progress = animProgress.value

        val focalY = progress * h
        val beamLength = h * 0.38f

        val edgeRails = listOf(0f to 1f, w to -1f)

        edgeRails.forEach { (xPos, dir) ->
            val startY = (focalY - beamLength).coerceAtLeast(0f)
            val endY = (focalY + beamLength).coerceAtMost(h)

            if (endY > startY) {
                // 1. Soft Vertical Ambient Edge Rail
                val railBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        PalapaBlue.copy(alpha = 0.4f * alpha),
                        PalapaGold.copy(alpha = 0.85f * alpha),
                        PalapaBlue.copy(alpha = 0.4f * alpha),
                        Color.Transparent
                    ),
                    startY = startY,
                    endY = endY
                )
                val railWidth = 4.dp.toPx()
                val left = if (dir > 0) 0f else w - railWidth
                drawRect(
                    brush = railBrush,
                    topLeft = Offset(left, startY),
                    size = Size(railWidth, endY - startY)
                )

                // 2. Primary Glowing Dot with Radial Light Aura
                val dotRadius = 4.5.dp.toPx()
                val auraRadius = 16.dp.toPx()

                val auraBrush = Brush.radialGradient(
                    colors = listOf(
                        PalapaGold.copy(alpha = 0.8f * alpha),
                        PalapaGold.copy(alpha = 0.25f * alpha),
                        Color.Transparent
                    ),
                    center = Offset(xPos, focalY),
                    radius = auraRadius
                )
                drawCircle(
                    brush = auraBrush,
                    radius = auraRadius,
                    center = Offset(xPos, focalY)
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.95f * alpha),
                    radius = dotRadius,
                    center = Offset(xPos, focalY)
                )

                // 3. Trailing and Leading Micro-Dots (Shimmer Dot Stream)
                val trailingOffset1 = 36.dp.toPx()
                val trailingY1 = focalY - trailingOffset1
                if (trailingY1 in 0f..h) {
                    drawCircle(
                        color = PalapaBlue.copy(alpha = 0.8f * alpha),
                        radius = 2.8.dp.toPx(),
                        center = Offset(xPos, trailingY1)
                    )
                }

                val trailingOffset2 = 66.dp.toPx()
                val trailingY2 = focalY - trailingOffset2
                if (trailingY2 in 0f..h) {
                    drawCircle(
                        color = PalapaGold.copy(alpha = 0.6f * alpha),
                        radius = 2.2.dp.toPx(),
                        center = Offset(xPos, trailingY2)
                    )
                }

                val leadOffset = 30.dp.toPx()
                val leadY = focalY + leadOffset
                if (leadY in 0f..h) {
                    drawCircle(
                        color = PalapaGold.copy(alpha = 0.7f * alpha),
                        radius = 2.6.dp.toPx(),
                        center = Offset(xPos, leadY)
                    )
                }
            }
        }
    }
}
