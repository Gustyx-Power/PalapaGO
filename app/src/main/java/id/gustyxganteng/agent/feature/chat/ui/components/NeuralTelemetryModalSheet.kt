package id.gustyxganteng.agent.feature.chat.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.core.database.GlobalUsageStats
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLowest
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.core.model.AgentExecutionMode
import id.gustyxganteng.agent.domain.context.ContextMeterStatus
import java.util.Locale
import kotlin.math.sqrt
import kotlin.random.Random

private class TelemetryParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralTelemetryModalSheet(
    stats: GlobalUsageStats,
    contextStatus: ContextMeterStatus?,
    activeModel: String,
    executionMode: AgentExecutionMode,
    totalSessionsCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = M3SurfaceContainer,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = M3PrimaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.4f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Hub,
                                contentDescription = null,
                                tint = M3Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Telemetri Neural & Sistem",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = M3TextPrimary
                        )
                        Text(
                            text = "Pusat Metrik & Analitik Kognitif PalapaGO",
                            style = MaterialTheme.typography.labelSmall,
                            color = M3TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup",
                        tint = M3TextSecondary
                    )
                }
            }

            // Animated Neural Canvas
            AnimatedNeuralMesh(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(M3SurfaceContainerLowest)
                    .border(1.dp, M3OutlineVariant, RoundedCornerShape(16.dp))
            )

            Spacer(Modifier.height(16.dp))

            // Context Window Health Card
            if (contextStatus != null) {
                val usageFraction = (contextStatus.usedTokens.toFloat() / contextStatus.maxCapacity.toFloat()).coerceIn(0f, 1f)
                val progressColor = when {
                    usageFraction > 0.85f -> Color(0xFFFFB4AB)
                    usageFraction > 0.65f -> Color(0xFFFFD54F)
                    else -> M3Primary
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = M3SurfaceContainerHigh,
                    border = BorderStroke(1.dp, M3OutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Memory,
                                    contentDescription = null,
                                    tint = M3Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Context Window Sesi Ini",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = M3TextPrimary
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.1f%%", contextStatus.percentage),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = progressColor
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { usageFraction },
                            color = progressColor,
                            trackColor = M3SurfaceContainerLowest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terpakai: ${String.format(Locale.US, "%,d", contextStatus.usedTokens)} token",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = M3TextSecondary
                            )
                            Text(
                                text = "Kapasitas: ${String.format(Locale.US, "%,d", contextStatus.maxCapacity)} token",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = M3TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // Metric Cards Grid
            Text(
                text = "Akumulasi Token Permanen",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = M3TextSecondary,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryCard(
                    title = "Konteks Masuk",
                    value = formatNumber(stats.promptTokens),
                    subtitle = "Prompt & File In",
                    icon = Icons.Rounded.Storage,
                    modifier = Modifier.weight(1f)
                )
                TelemetryCard(
                    title = "Sintesis Keluar",
                    value = formatNumber(stats.completionTokens),
                    subtitle = "Completion Out",
                    icon = Icons.Rounded.Speed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryCard(
                    title = "Total Token",
                    value = formatNumber(stats.totalTokens),
                    subtitle = "Kumulatif Sistem",
                    icon = Icons.Rounded.Hub,
                    modifier = Modifier.weight(1f)
                )
                TelemetryCard(
                    title = "Total Request",
                    value = "${stats.totalRequests} kali",
                    subtitle = "$totalSessionsCount Sesi Obrolan",
                    icon = Icons.Rounded.ElectricBolt,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Multi-Engine Architecture Status
            Text(
                text = "Arsitektur Multi-Engine & Sidecar",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = M3TextSecondary,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = M3SurfaceContainerHigh,
                border = BorderStroke(1.dp, M3OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    EngineStatusRow(
                        name = "Agnes AI (Core Brain)",
                        detail = "Model: $activeModel • Mode: ${executionMode.name}",
                        status = "AKTIF",
                        statusColor = Color(0xFF81C995)
                    )
                    HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    EngineStatusRow(
                        name = "Groq Sidecar (Hardware Acceleration)",
                        detail = "Model: openai/gpt-oss-20b (~1.000 t/s) • Truncation Guard",
                        status = "AKTIF",
                        statusColor = Color(0xFF81C995)
                    )
                    HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    EngineStatusRow(
                        name = "Agnes Image 2.5 Flash",
                        detail = "Multimodal Vision & Aspect Ratio Studio",
                        status = if (executionMode == AgentExecutionMode.FAST) "OFF (Mode Cepat)" else "SIAP",
                        statusColor = if (executionMode == AgentExecutionMode.FAST) Color(0xFFFFB4AB) else Color(0xFF81C995)
                    )
                    HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    EngineStatusRow(
                        name = "Gemini Multimodal Sidecar",
                        detail = "Visual Compression & Fallback Reasoning",
                        status = "STANDBY",
                        statusColor = M3Primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TelemetryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = M3SurfaceContainerHigh,
        border = BorderStroke(1.dp, M3OutlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = M3TextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = M3Primary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                ),
                color = M3TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = M3TextSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EngineStatusRow(
    name: String,
    detail: String,
    status: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = M3TextPrimary
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = M3TextSecondary
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = statusColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
        ) {
            Text(
                text = status,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun AnimatedNeuralMesh(modifier: Modifier = Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { time -> frameTime = time }
        }
    }

    val nodeCount = 20
    val particles = remember {
        val rng = Random(42)
        List(nodeCount) {
            TelemetryParticle(
                x = rng.nextFloat() * 300f + 20f,
                y = rng.nextFloat() * 120f + 10f,
                vx = (rng.nextFloat() - 0.5f) * 0.9f,
                vy = (rng.nextFloat() - 0.5f) * 0.9f,
                radius = rng.nextFloat() * 2f + 2.5f
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 10f || h <= 10f) return@Canvas

        particles.forEach { p ->
            p.x += p.vx
            p.y += p.vy
            if (p.x < 5f) { p.x = 5f; p.vx = -p.vx }
            else if (p.x > w - 5f) { p.x = w - 5f; p.vx = -p.vx }
            if (p.y < 5f) { p.y = 5f; p.vy = -p.vy }
            else if (p.y > h - 5f) { p.y = h - 5f; p.vy = -p.vy }
        }

        val connectDist = 80.dp.toPx()
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
                        color = M3Primary.copy(alpha = alpha),
                        start = Offset(p1.x, p1.y),
                        end = Offset(p2.x, p2.y),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
        }

        particles.forEach { p ->
            drawCircle(
                color = M3Primary.copy(alpha = 0.25f),
                radius = p.radius * 2f,
                center = Offset(p.x, p.y)
            )
            drawCircle(
                color = M3Primary,
                radius = p.radius,
                center = Offset(p.x, p.y)
            )
        }
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format(Locale.US, "%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format(Locale.US, "%,d", number)
        else -> number.toString()
    }
}
