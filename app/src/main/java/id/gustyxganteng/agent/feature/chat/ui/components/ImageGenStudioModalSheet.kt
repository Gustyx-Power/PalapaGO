package id.gustyxganteng.agent.feature.chat.ui.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop169
import androidx.compose.material.icons.rounded.Crop32
import androidx.compose.material.icons.rounded.Crop54
import androidx.compose.material.icons.rounded.Crop75
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import id.gustyxganteng.agent.core.designsystem.M3Error
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLowest
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.core.model.AgentExecutionMode
import kotlinx.coroutines.launch

data class AspectRatioOption(
    val ratio: String,
    val dimension: String,
    val label: String,
    val icon: ImageVector
)

val ASPECT_RATIO_OPTIONS = listOf(
    AspectRatioOption("1:1", "1024x1024", "Persegi", Icons.Rounded.CropSquare),
    AspectRatioOption("9:16", "1024x1792", "Vertikal", Icons.Rounded.Crop32),
    AspectRatioOption("16:9", "1792x1024", "Lanskap", Icons.Rounded.Crop169),
    AspectRatioOption("3:4", "768x1024", "Potret", Icons.Rounded.Crop54),
    AspectRatioOption("4:3", "1024x768", "Klasik", Icons.Rounded.Crop75)
)

private val QUICK_IDEAS = listOf(
    "Diagram sel biologi mitosis & sitokinesis dengan label tajam",
    "Arsitektur microservices cloud-native modern, flat vector illustration",
    "Infografis langkah algoritma machine learning, clean academic visual",
    "Cover makalah ilmiah AI, minimalist tech illustration high-contrast",
    "Ilustrasi konsep kuantum computing & qubits, modern dark aesthetic"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenStudioModalSheet(
    executionMode: AgentExecutionMode,
    onSelectExecutionMode: (AgentExecutionMode) -> Unit,
    onGenerateImage: suspend (prompt: String, ratio: String) -> Result<String>,
    onSendImageToChat: (markdown: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var promptText by remember { mutableStateOf("") }
    var selectedRatio by remember { mutableStateOf(ASPECT_RATIO_OPTIONS[0]) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedImageUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFullscreenDialog by remember { mutableStateOf(false) }

    val isFastMode = executionMode == AgentExecutionMode.FAST

    if (showFullscreenDialog && generatedImageUrl != null) {
        Dialog(
            onDismissRequest = { showFullscreenDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullscreenDialog = false },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(generatedImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = promptText,
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { showFullscreenDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup",
                        tint = Color.White
                    )
                }
            }
        }
    }

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
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = M3Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Image Gen Studio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = M3TextPrimary
                        )
                        Text(
                            text = "Multimodal Engine • Agnes Image 2.5 Flash",
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

            // Mode Cepat Warning Banner
            if (isFastMode) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF351214),
                    border = BorderStroke(1.dp, Color(0xFF6E2327)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB4AB),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Mode Cepat (FAST) Aktif",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFB4AB)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Mode Cepat difokuskan untuk inferensi teks & kode kilat tanpa beban multimodal. Untuk membuat gambar, silakan beralih ke Mode Standar, Tinggi, Smart, atau Maksimal.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                            color = Color(0xFFFFDAD6)
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onSelectExecutionMode(AgentExecutionMode.STANDARD) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = M3Primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Beralih ke Mode Standar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Prompt Input Field
            Text(
                text = "Deskripsi Visual (Prompt)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = M3TextSecondary,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = {
                    Text(
                        text = "Deskripsikan visual yang ingin dibuat (contoh: Diagram mitosis sel biologi dengan label jelas, flat vector modern)...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = M3TextSecondary.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (promptText.isNotBlank()) {
                        IconButton(onClick = { promptText = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hapus", tint = M3TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = M3SurfaceContainerLowest,
                    unfocusedContainerColor = M3SurfaceContainerLowest,
                    focusedBorderColor = M3Primary,
                    unfocusedBorderColor = M3OutlineVariant,
                    focusedTextColor = M3TextPrimary,
                    unfocusedTextColor = M3TextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Quick Idea Chips
            Text(
                text = "Inspirasi & Template Prompt",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = M3TextSecondary,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QUICK_IDEAS.forEach { idea ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = M3SurfaceContainerHigh,
                        border = BorderStroke(1.dp, M3OutlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { promptText = idea }
                    ) {
                        Text(
                            text = idea,
                            fontSize = 11.5.sp,
                            color = M3TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Aspect Ratio Picker
            Text(
                text = "Pilihan Rasio Aspek",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = M3TextSecondary,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ASPECT_RATIO_OPTIONS.forEach { opt ->
                    val isSelected = selectedRatio.ratio == opt.ratio
                    val cardBg = if (isSelected) M3PrimaryContainer else M3SurfaceContainerHigh
                    val borderCol = if (isSelected) M3Primary else M3OutlineVariant
                    val textColor = if (isSelected) M3Primary else M3TextPrimary

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedRatio = opt }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = opt.icon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = opt.ratio,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = textColor
                            )
                            Text(
                                text = opt.label,
                                fontSize = 9.sp,
                                color = M3TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Generate Button
            Button(
                onClick = {
                    if (promptText.isBlank() || isFastMode || isGenerating) return@Button
                    isGenerating = true
                    errorMessage = null
                    scope.launch {
                        val res = onGenerateImage(promptText.trim(), selectedRatio.dimension)
                        isGenerating = false
                        res.fold(
                            onSuccess = { url ->
                                generatedImageUrl = url
                                Toast.makeText(context, "Gambar visual berhasil dihasilkan!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                errorMessage = err.message ?: "Gagal menghasilkan gambar."
                            }
                        )
                    }
                },
                enabled = promptText.isNotBlank() && !isFastMode && !isGenerating,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = M3Primary,
                    contentColor = Color.Black,
                    disabledContainerColor = M3SurfaceContainerHigh,
                    disabledContentColor = M3TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = Color.Black
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Menghasilkan Gambar Agnes AI...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buat Gambar Visual (${selectedRatio.ratio})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Error display
            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF351214),
                    border = BorderStroke(1.dp, Color(0xFF6E2327)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = M3Error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Live Preview Card
            if (generatedImageUrl != null) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Hasil Visual (${selectedRatio.dimension})",
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
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 360.dp)
                                .background(M3SurfaceContainerLowest)
                                .clickable { showFullscreenDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(generatedImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = promptText,
                                contentScale = ContentScale.Fit,
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(220.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp), color = M3Primary)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            )
                        }

                        // Action Bar below image
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(M3SurfaceContainer)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Download button
                            Button(
                                onClick = {
                                    try {
                                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                                        if (dm != null) {
                                            val filename = "palapago_${System.currentTimeMillis()}.png"
                                            val req = DownloadManager.Request(Uri.parse(generatedImageUrl))
                                                .setTitle("PalapaGO Image Gen")
                                                .setDescription(promptText.take(40))
                                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "PalapaGO/$filename")
                                            dm.enqueue(req)
                                            Toast.makeText(context, "Mulai mengunduh ke Galeri (Pictures/PalapaGO)", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal mengunduh: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = M3PrimaryContainer, contentColor = M3Primary),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Unduh ke Galeri", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Copy Link
                                IconButton(
                                    onClick = {
                                        generatedImageUrl?.let {
                                            clipboardManager.setText(AnnotatedString(it))
                                            Toast.makeText(context, "URL disalin!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin Tautan", tint = M3TextSecondary, modifier = Modifier.size(16.dp))
                                }

                                // Send to Chat
                                IconButton(
                                    onClick = {
                                        generatedImageUrl?.let { url ->
                                            val md = "![$promptText]($url)\n\n*Gambar dihasilkan via Image Gen Studio (${selectedRatio.dimension})*"
                                            onSendImageToChat(md)
                                            Toast.makeText(context, "Gambar dikirim ke Obrolan!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Kirim ke Chat", tint = M3Primary, modifier = Modifier.size(16.dp))
                                }

                                // Browser
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(generatedImageUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Buka Browser", tint = M3TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
