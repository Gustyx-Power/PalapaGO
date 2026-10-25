package id.gustyxganteng.agent.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.core.designsystem.M3TextTertiary
import id.gustyxganteng.agent.core.designsystem.PalapaBackground
import id.gustyxganteng.agent.core.designsystem.PalapaBorder
import id.gustyxganteng.agent.core.designsystem.PalapaGold
import id.gustyxganteng.agent.core.designsystem.PalapaSurface

@Composable
fun NetworkOfflineOverlay(
    isOnline: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = fadeIn(tween(300)) + slideInVertically(tween(350)) { -it / 2 },
        exit = fadeOut(tween(300)) + slideOutVertically(tween(350)) { -it / 2 },
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "offline_pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PalapaBackground.copy(alpha = 0.94f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PalapaSurface,
                border = BorderStroke(1.dp, PalapaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.WifiOff,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.offline_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = M3TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.offline_desc),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        ),
                        color = M3TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(pulseAlpha)
                                .background(Color(0xFFF59E0B), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Auto-reconnecting...",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = M3TextTertiary
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PalapaGold.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PalapaGold.copy(alpha = 0.08f),
                            contentColor = PalapaGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.offline_retry_button),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
