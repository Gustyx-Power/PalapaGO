package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.core.database.GlobalUsageStats

import id.gustyxganteng.agent.core.model.AgentExecutionMode
import id.gustyxganteng.agent.domain.context.ContextMeterManager
import id.gustyxganteng.agent.domain.context.ContextMeterStatus
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class SphereNode3D(
    val nx: Float,
    val ny: Float,
    val nz: Float,
    val color: Color,
    val baseRadius: Float,
    val isClusterCore: Boolean = false,
    val clusterId: Int = 0
)

private data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val z: Float,
    val scale: Float,
    val alpha: Float,
    val color: Color,
    val baseRadius: Float,
    val isClusterCore: Boolean
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

    // Perhitungan & Totalisasi Komprehensif
    val promptTokens = stats.promptTokens
    val completionTokens = stats.completionTokens
    val reasoningTokens = stats.reasoningTokens
    val totalTokens = stats.totalTokens
    val totalRequests = stats.totalRequests

    val promptPct = if (totalTokens > 0L) (promptTokens.toDouble() / totalTokens.toDouble() * 100.0) else 0.0
    val thinkPct = if (totalTokens > 0L) (reasoningTokens.toDouble() / totalTokens.toDouble() * 100.0) else 0.0
    val compPct = if (totalTokens > 0L) (completionTokens.toDouble() / totalTokens.toDouble() * 100.0) else 0.0
    val avgTokensPerReq = if (totalRequests > 0L) (totalTokens / totalRequests) else 0L
    val ioRatio = if (completionTokens > 0L) String.format(Locale.US, "%.1f : 1", promptTokens.toDouble() / completionTokens.toDouble()) else "1 : 1"

    val currentUsed = contextStatus?.usedTokens ?: 0
    val currentMax = contextStatus?.maxCapacity ?: 150_000
    val currentRemaining = (currentMax - currentUsed).coerceAtLeast(0)
    val currentPct = contextStatus?.percentage ?: 0f

    // Totalisasi Node Kognitif & Edge Synapse Berdasarkan Metrik Riil
    val computedNodes = (totalRequests * 18 + (currentUsed / 60) + 140).toInt().coerceIn(140, 99999)
    val computedEdges = (computedNodes * 2.2f).toInt()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                    Icon(
                        imageVector = Icons.Rounded.Hub,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Telemetri Neural & Sistem",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Visualisasi 3D Kognitif & Kalkulasi Token Realtime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3D Spherical Neural Knowledge Globe (Animated + Touch Interactive)
            SphericalNeuralGlobeCard(
                totalNodes = computedNodes,
                totalEdges = computedEdges,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                sessionsCount = totalSessionsCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )

            Spacer(Modifier.height(18.dp))

            // Section 1: Kalkulasi & Totalisasi Akumulasi Token
            Text(
                text = "Kalkulasi & Totalisasi Akumulasi Token",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header: Eyebrow + Request Counter Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL TOKEN KUMULATIF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "$totalRequests Request",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Hero Token Counter
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = String.format(Locale.US, "%,d", totalTokens),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "token",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Multi-Segment Distribution Bar
                    val promptFraction = if (totalTokens > 0L) (promptTokens.toFloat() / totalTokens.toFloat()) else 0.33f
                    val thinkFraction = if (totalTokens > 0L) (reasoningTokens.toFloat() / totalTokens.toFloat()) else 0.33f
                    val compFraction = if (totalTokens > 0L) (completionTokens.toFloat() / totalTokens.toFloat()) else 0.34f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(promptFraction.coerceAtLeast(0.02f))
                                .fillMaxSize()
                                .background(Color(0xFF64FFDA))
                        )
                        if (thinkFraction > 0.005f) {
                            Box(
                                modifier = Modifier
                                    .weight(thinkFraction.coerceAtLeast(0.02f))
                                    .fillMaxSize()
                                    .background(Color(0xFFFFD180))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(compFraction.coerceAtLeast(0.02f))
                                .fillMaxSize()
                                .background(Color(0xFFE879F9))
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Structured Breakdown Ledger
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            TokenLedgerRow(
                                label = "Konteks (Prompt)",
                                sublabel = "Instruksi pengguna & riwayat percakapan",
                                value = promptTokens,
                                percentage = promptPct,
                                color = Color(0xFF64FFDA)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 12.dp))
                            TokenLedgerRow(
                                label = "Penalaran (Thinking)",
                                sublabel = "Monolog kognitif & rantai pemikiran internal",
                                value = reasoningTokens,
                                percentage = thinkPct,
                                color = Color(0xFFFFD180)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 12.dp))
                            TokenLedgerRow(
                                label = "Sintesis (Output)",
                                sublabel = "Hasil respons teks, kode & parameter tool",
                                value = completionTokens,
                                percentage = compPct,
                                color = Color(0xFFE879F9)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Symmetrical Footer Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "RATA-RATA / REQ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%,d", avgTokensPerReq)} tok",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "RASIO INPUT : OUTPUT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = ioRatio,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Section 2: Context Window Sesi Aktif
            Text(
                text = "Kapasitas & Kesehatan Context Window",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val progressFraction = (currentUsed.toFloat() / currentMax.toFloat()).coerceIn(0f, 1f)
                    val progressColor = when {
                        currentPct > 85f -> Color(0xFFFFB4AB)
                        currentPct > 60f -> Color(0xFFFFD54F)
                        else -> Color(0xFF64FFDA)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Memory, contentDescription = null, tint = progressColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Sesi Aktif Saat Ini",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%.2f%%", currentPct),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            color = progressColor
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Terpakai", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format(Locale.US, "%,d", currentUsed)} token",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sisa Bebas", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format(Locale.US, "%,d", currentRemaining)} token",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Maksimal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format(Locale.US, "%,d", currentMax)} token",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (contextStatus != null && contextStatus.sessionTotalTokens > 0L) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
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
                                        text = "${String.format(Locale.US, "%,d", contextStatus.sessionTotalTokens)} token",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "In: ${ContextMeterManager.formatTokenNumber(contextStatus.sessionPromptTokens.toInt())}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF64FFDA)
                                    )
                                    Text(
                                        text = "Think: ${ContextMeterManager.formatTokenNumber(contextStatus.sessionReasoningTokens.toInt())}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFFD180)
                                    )
                                    Text(
                                        text = "Out: ${ContextMeterManager.formatTokenNumber(contextStatus.sessionCompletionTokens.toInt())}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFE879F9)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Section 3: Status Pipeline Ensemble Kolaboratif
            Text(
                text = "Pipeline Ensemble Kolaboratif (Koding Bersama)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    EngineStatusRow(
                        name = "Gemini 3.8 Flash (Chief Architect)",
                        detail = "Role: Blueprint Arsitektur, Math/Physics Modelling & State Machine",
                        status = "AKTIF (Planner)",
                        statusColor = Color(0xFF64FFDA)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    EngineStatusRow(
                        name = "Agnes AI (Lead Fullstack Synthesizer)",
                        detail = "Role: Sintesis Kode Produksi, Canvas Artifacts & Tool Loop ($activeModel)",
                        status = "AKTIF (Coder)",
                        statusColor = Color(0xFF64FFDA)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    EngineStatusRow(
                        name = "Xemarux Router (Next-Gen Deep Thinking)",
                        detail = "Role: Kimi-k2.8-preview (40 RPM) & DeepSeek-v4.1-flash (12 RPM)",
                        status = "AKTIF (Co-Reasoner)",
                        statusColor = Color(0xFF64FFDA)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    EngineStatusRow(
                        name = "Groq LPU Sidecar (Fast Quality Auditor)",
                        detail = "Role: Pre-Flight Linting (<250ms), Anti-Placeholder & Syntax Guard",
                        status = "AKTIF (Auditor)",
                        statusColor = Color(0xFF64FFDA)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    EngineStatusRow(
                        name = "Agnes Image 2.5 Flash (Visual Studio)",
                        detail = "Role: Generasi Aset Visual & Komposisi Sinematik (5 Rasio)",
                        status = if (executionMode == AgentExecutionMode.FAST) "OFF (Mode Cepat)" else "SIAP",
                        statusColor = if (executionMode == AgentExecutionMode.FAST) Color(0xFFFFB4AB) else Color(0xFF64FFDA)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    EngineStatusRow(
                        name = "Resilient Mesh Failover Router",
                        detail = "Role: Kaskade Otomatis (Agnes 1-2 → Xemarux Kimi/DeepSeek → Gemini → Groq)",
                        status = "STANDBY",
                        statusColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SphericalNeuralGlobeCard(
    totalNodes: Int,
    totalEdges: Int,
    promptTokens: Long,
    completionTokens: Long,
    sessionsCount: Int,
    modifier: Modifier = Modifier
) {
    // Rotasi Otomatis Hardware-Accelerated
    val infiniteTransition = rememberInfiniteTransition(label = "sphere_rotation")
    val autoYaw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_yaw"
    )

    // Rotasi Manual dengan Sentuhan/Drag
    var manualYaw by remember { mutableFloatStateOf(0f) }
    var manualPitch by remember { mutableFloatStateOf(10f) }

    val currentYaw = autoYaw + manualYaw
    val currentPitch = manualPitch

    // Titik Bola 3D Fibonacci + Cluster Kognitif Padat (Menyerupai Gambar Referensi)
    val nodes = remember {
        val list = mutableListOf<SphereNode3D>()
        val n = 220
        val phi = (1.0 + sqrt(5.0)) / 2.0

        for (i in 0 until n) {
            val y = 1f - (i.toFloat() / (n - 1).toFloat()) * 2f
            val radAtY = sqrt((1f - y * y).coerceAtLeast(0f))
            val theta = (2.0 * Math.PI * i / phi).toFloat()

            val x = (cos(theta) * radAtY)
            val py = y
            val z = (sin(theta) * radAtY)

            val color = when {
                py < -0.3f && x > 0.2f -> Color(0xFF64FFDA) // Cyan / Mint
                py > 0.25f && x > 0.15f -> Color(0xFFE879F9) // Magenta / Purple
                x < -0.25f -> Color(0xFF38BDF8)             // Electric Blue
                y > 0.4f -> Color(0xFFFCD34D)               // Amber Gold
                else -> Color(0xFF818CF8)                   // Indigo
            }

            list.add(SphereNode3D(x, py, z, color, baseRadius = 2.4f))
        }

        // Cluster 1 (Kanan-Atas: Inti Terang Putih & Magenta, Menyerupai Referensi)
        val c1x = 0.45f; val c1y = -0.38f; val c1z = 0.45f
        for (i in 0 until 28) {
            val rx = c1x + (Random.nextFloat() - 0.5f) * 0.24f
            val ry = c1y + (Random.nextFloat() - 0.5f) * 0.24f
            val rz = c1z + (Random.nextFloat() - 0.5f) * 0.24f
            list.add(SphereNode3D(rx, ry, rz, Color(0xFFFFFFFF), baseRadius = 3.6f, isClusterCore = true, clusterId = 1))
        }

        // Cluster 2 (Kanan-Bawah: Inti Terang Putih & Cyan, Menyerupai Referensi)
        val c2x = 0.42f; val c2y = 0.40f; val c2z = 0.42f
        for (i in 0 until 28) {
            val rx = c2x + (Random.nextFloat() - 0.5f) * 0.24f
            val ry = c2y + (Random.nextFloat() - 0.5f) * 0.24f
            val rz = c2z + (Random.nextFloat() - 0.5f) * 0.24f
            list.add(SphereNode3D(rx, ry, rz, Color(0xFFE0F7FA), baseRadius = 3.6f, isClusterCore = true, clusterId = 2))
        }

        // Cluster 3 (Kiri-Tengah: Inti Hijau Turquoise)
        val c3x = -0.42f; val c3y = 0.02f; val c3z = 0.40f
        for (i in 0 until 22) {
            val rx = c3x + (Random.nextFloat() - 0.5f) * 0.22f
            val ry = c3y + (Random.nextFloat() - 0.5f) * 0.22f
            val rz = c3z + (Random.nextFloat() - 0.5f) * 0.22f
            list.add(SphereNode3D(rx, ry, rz, Color(0xFF64FFDA), baseRadius = 3.2f, isClusterCore = true, clusterId = 3))
        }

        // Filamen Horizontal Ekuator (Menghubungkan Cluster Seperti Gambar Referensi)
        for (i in 0 until 25) {
            val t = (i.toFloat() / 24f) * 2f - 1f // -1 to 1
            val hx = t * 0.85f
            val hy = (Random.nextFloat() - 0.5f) * 0.12f
            val hz = sqrt((1f - hx * hx - hy * hy).coerceAtLeast(0.04f))
            list.add(SphereNode3D(hx, hy, hz, Color(0xFF64FFDA), baseRadius = 2.2f, isClusterCore = false, clusterId = 4))
        }

        list
    }

    // Precomputed Edges Antar Node Terdekat untuk 120 FPS
    val precomputedEdges = remember(nodes) {
        val edgeList = mutableListOf<Pair<Int, Int>>()
        val maxDistSq = 0.38f * 0.38f
        for (i in nodes.indices) {
            val p1 = nodes[i]
            for (j in i + 1 until nodes.size) {
                val p2 = nodes[j]
                val dx = p1.nx - p2.nx
                val dy = p1.ny - p2.ny
                val dz = p1.nz - p2.nz
                val distSq = dx * dx + dy * dy + dz * dz
                if (distSq < maxDistSq) {
                    edgeList.add(Pair(i, j))
                }
            }
        }
        edgeList
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Kanvas Rendering 3D Interaktif Penuh
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            manualYaw += dragAmount.x * 0.5f
                            manualPitch = (manualPitch - dragAmount.y * 0.5f).coerceIn(-75f, 75f)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                if (w <= 10f || h <= 10f) return@Canvas

                val centerX = w / 2f
                val centerY = h / 2f

                // Skala Bola 3D Proporsional Mengisi Ruang Kartu Secara Megah
                val sphereRadius = minOf(w, h) * 0.36f
                val cameraDist = sphereRadius * 2.3f
                val fov = sphereRadius * 2.5f

                val radY = Math.toRadians(currentYaw.toDouble()).toFloat()
                val cosY = cos(radY)
                val sinY = sin(radY)

                val radP = Math.toRadians(currentPitch.toDouble()).toFloat()
                val cosP = cos(radP)
                val sinP = sin(radP)

                // Proyeksikan seluruh titik 3D ke 2D
                val projected = nodes.map { p ->
                    val ox = p.nx * sphereRadius
                    val oy = p.ny * sphereRadius
                    val oz = p.nz * sphereRadius

                    val x1 = ox * cosY + oz * sinY
                    val z1 = -ox * sinY + oz * cosY

                    val y2 = oy * cosP - z1 * sinP
                    val z2 = oy * sinP + z1 * cosP
                    val x2 = x1

                    val scale = fov / (cameraDist + z2)
                    val projX = centerX + x2 * scale
                    val projY = centerY + y2 * scale
                    val depthAlpha = ((z2 + sphereRadius) / (2f * sphereRadius)).coerceIn(0.12f, 1f)

                    ProjectedPoint(
                        screenX = projX,
                        screenY = projY,
                        z = z2,
                        scale = scale,
                        alpha = depthAlpha,
                        color = p.color,
                        baseRadius = p.baseRadius,
                        isClusterCore = p.isClusterCore
                    )
                }

                // 1. Gambar Garis Sinapsis (Edges) di Latar
                precomputedEdges.forEach { edge ->
                    val pt1 = projected[edge.first]
                    val pt2 = projected[edge.second]
                    val avgZ = (pt1.z + pt2.z) / 2f
                    val lineAlpha = ((avgZ + sphereRadius) / (2f * sphereRadius)).coerceIn(0.06f, 0.85f) * 0.42f
                    val lineColor = if (pt1.isClusterCore || pt2.isClusterCore) Color(0xFF64FFDA) else Color(0xFF38BDF8)

                    drawLine(
                        color = lineColor.copy(alpha = lineAlpha),
                        start = Offset(pt1.screenX, pt1.screenY),
                        end = Offset(pt2.screenX, pt2.screenY),
                        strokeWidth = if (avgZ > 0) 1.25.dp.toPx() else 0.75.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 2. Gambar Nodes dengan Glow Bloom & Depth
                projected.forEach { pt ->
                    if (pt.isClusterCore) {
                        // Halo Cahaya Multi-Level yang Sangat Terang Menyerupai Referensi
                        drawCircle(
                            color = pt.color.copy(alpha = 0.10f * pt.alpha),
                            radius = 24f * pt.scale,
                            center = Offset(pt.screenX, pt.screenY)
                        )
                        drawCircle(
                            color = pt.color.copy(alpha = 0.28f * pt.alpha),
                            radius = 12f * pt.scale,
                            center = Offset(pt.screenX, pt.screenY)
                        )
                        drawCircle(
                            color = pt.color.copy(alpha = 0.75f * pt.alpha),
                            radius = 6f * pt.scale,
                            center = Offset(pt.screenX, pt.screenY)
                        )
                    }

                    // Inti Node
                    drawCircle(
                        color = pt.color.copy(alpha = pt.alpha),
                        radius = (pt.baseRadius * pt.scale).coerceAtLeast(1.2f),
                        center = Offset(pt.screenX, pt.screenY)
                    )
                }
            }

            // Top Status Bar Overlay (Menyerupai Gambar Referensi)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Neural Memory Graph",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp
                        ),
                        color = Color(0xFF64FFDA)
                    )
                    Text(
                        text = "${String.format(Locale.US, "%,d", totalNodes)} nodes / ${String.format(Locale.US, "%,d", totalEdges)} edges",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tombol Reset Rotasi
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                    modifier = Modifier.size(32.dp)
                ) {
                    IconButton(
                        onClick = { manualYaw = 0f; manualPitch = 10f },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset Rotasi",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom Filter Badges Overlay (Menyerupai Filter di Gambar Referensi)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NeuralFilterPill(label = "Konteks", count = formatNumber(promptTokens), color = Color(0xFF64FFDA))
                NeuralFilterPill(label = "Sintesis", count = formatNumber(completionTokens), color = Color(0xFFE879F9))
                NeuralFilterPill(label = "Memori", count = "$sessionsCount Sesi", color = Color(0xFF38BDF8))
                NeuralFilterPill(label = "Ensemble", count = "Agnes+Kimi+Gemini+Groq", color = Color(0xFFFCD34D))
            }
        }
    }
}

@Composable
private fun NeuralFilterPill(
    label: String,
    count: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F131D).copy(alpha = 0.92f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$label: $count",
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TokenLedgerRow(
    label: String,
    sublabel: String,
    value: Long,
    percentage: Double,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = String.format(Locale.US, "%,d", value),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f%%", percentage),
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                )
            }
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format(Locale.US, "%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format(Locale.US, "%,d", number)
        else -> number.toString()
    }
}
