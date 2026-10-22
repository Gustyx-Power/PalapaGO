package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.core.database.GlobalUsageStats
import java.util.Locale
import kotlin.math.sqrt
import kotlin.random.Random

private class SpiderParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float
)

@Composable
fun NeuralSpiderStatsCard(
    stats: GlobalUsageStats,
    isDrawerOpen: Boolean,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        onClick = { showDialog = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Hub,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Telemetri Neural",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF102615))
                        .border(1.dp, Color(0xFF184221), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF81C995))
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "AKTIF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C995)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Spider Network Animated Mesh Canvas
            NeuralSpiderCanvas(
                isDrawerOpen = isDrawerOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.height(12.dp))

            // Stats Metric Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem(
                    title = "Konteks",
                    value = formatCompactNumber(stats.promptTokens),
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    title = "Thinking",
                    value = formatCompactNumber(stats.reasoningTokens),
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    title = "Total Token",
                    value = formatCompactNumber(stats.totalTokens),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Akumulatif permanen sistem • Ketuk untuk detail",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Hub,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Detail Telemetri Sistem",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Data telemetri di bawah ini dicatat secara permanen di DataStore lokal dan tidak akan terhapus meskipun sesi percakapan dihapus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TelemetryDetailRow("Total Eksekusi Request", "${stats.totalRequests} kali")
                    TelemetryDetailRow("Prompt Tokens (Konteks Masuk)", String.format(Locale.US, "%,d token", stats.promptTokens))
                    TelemetryDetailRow("Penalaran (Thinking Tokens)", String.format(Locale.US, "%,d token", stats.reasoningTokens))
                    TelemetryDetailRow("Completion Tokens (Keluaran)", String.format(Locale.US, "%,d token", stats.completionTokens))
                    TelemetryDetailRow("Total Token Kumulatif", String.format(Locale.US, "%,d token", stats.totalTokens))
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Tutup", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun TelemetryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TelemetryMetricItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NeuralSpiderCanvas(
    isDrawerOpen: Boolean,
    modifier: Modifier = Modifier
) {
    var frameTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isDrawerOpen) {
        if (!isDrawerOpen) return@LaunchedEffect
        while (true) {
            withInfiniteAnimationFrameMillis { time ->
                frameTime = time
            }
        }
    }

    val spiderAccent = MaterialTheme.colorScheme.primary
    val nodeCount = 18
    val particles = remember {
        val rng = Random(42)
        List(nodeCount) {
            SpiderParticle(
                x = rng.nextFloat() * 300f + 20f,
                y = rng.nextFloat() * 120f + 10f,
                vx = (rng.nextFloat() - 0.5f) * 0.8f,
                vy = (rng.nextFloat() - 0.5f) * 0.8f,
                radius = rng.nextFloat() * 2f + 2.5f
            )
        }
    }

    Canvas(modifier = modifier) {
        val currentTick = frameTime
        val w = size.width
        val h = size.height
        if (w <= 10f || h <= 10f) return@Canvas

        particles.forEach { p ->
            p.x += p.vx
            p.y += p.vy

            if (p.x < 5f) {
                p.x = 5f
                p.vx = -p.vx
            } else if (p.x > w - 5f) {
                p.x = w - 5f
                p.vx = -p.vx
            }

            if (p.y < 5f) {
                p.y = 5f
                p.vy = -p.vy
            } else if (p.y > h - 5f) {
                p.y = h - 5f
                p.vy = -p.vy
            }
        }

        val connectDist = 75.dp.toPx()
        val connectDistSq = connectDist * connectDist

        for (i in 0 until particles.size) {
            val p1 = particles[i]
            for (j in i + 1 until particles.size) {
                val p2 = particles[j]
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                val distSq = dx * dx + dy * dy
                if (distSq < connectDistSq) {
                    val dist = sqrt(distSq)
                    val alpha = (1f - dist / connectDist).coerceIn(0f, 1f) * 0.45f
                    drawLine(
                        color = spiderAccent.copy(alpha = alpha),
                        start = Offset(p1.x, p1.y),
                        end = Offset(p2.x, p2.y),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
        }

        particles.forEach { p ->
            drawCircle(
                color = spiderAccent.copy(alpha = 0.25f),
                radius = p.radius * 2f,
                center = Offset(p.x, p.y)
            )
            drawCircle(
                color = spiderAccent,
                radius = p.radius,
                center = Offset(p.x, p.y)
            )
        }
    }
}

private fun formatCompactNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format(Locale.US, "%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format(Locale.US, "%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
