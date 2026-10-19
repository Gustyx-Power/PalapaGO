package id.gustyxganteng.agent.feature.chat.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLowest
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL

/**
 * Siluet Animasi Pembuatan Sketsa (Menyerupai Tampilan ChatGPT Saat Menghasilkan Gambar).
 * Menampilkan grid dot-matrix berdenyut dengan indikator progress dan label "Membuat sketsa...".
 */
@Composable
fun ImageGeneratingSilhouette(
    modifier: Modifier = Modifier,
    statusText: String = "Membuat sketsa..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DotMatrixPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    var simulatedProgress by remember { mutableIntStateOf(15) }

    LaunchedEffect(Unit) {
        while (simulatedProgress < 96) {
            delay(400)
            simulatedProgress += (2..8).random()
            if (simulatedProgress > 96) simulatedProgress = 96
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF090B10),
        border = BorderStroke(1.dp, M3OutlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(M3Primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        color = M3TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = M3Primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, M3Primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "$simulatedProgress%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = M3Primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Canvas Grid Dot Matrix Berdenyut
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF050608))
                    .border(BorderStroke(1.dp, Color(0xFF161922)), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = 17
                    val rows = 13
                    val stepX = size.width / (cols + 1)
                    val stepY = size.height / (rows + 1)
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val maxDist = kotlin.math.sqrt(centerX * centerX + centerY * centerY)

                    for (c in 1..cols) {
                        for (r in 1..rows) {
                            val x = c * stepX
                            val y = r * stepY
                            val dx = x - centerX
                            val dy = y - centerY
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            val factor = (1f - (dist / maxDist).coerceIn(0f, 1f))

                            val isCenterCluster = dist < 70.dp.toPx()
                            val dotRadius = if (isCenterCluster) 3.2.dp.toPx() else 1.8.dp.toPx()
                            val dotAlpha = (factor * pulseAlpha).coerceIn(0.1f, 1.0f)

                            val dotColor = if (isCenterCluster) {
                                Color(0xFF64FFDA).copy(alpha = dotAlpha)
                            } else {
                                Color(0xFF38BDF8).copy(alpha = dotAlpha * 0.7f)
                            }

                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                // Progress Badge Tengah Menyerupai ChatGPT
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0D111A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFF263045))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                            color = M3Primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Menyintesis Visual...",
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview Gambar Showcase Tunggal Bersih (Menyerupai Tampilan ChatGPT Foto 1).
 * Menampilkan preview gambar penuh, prompt singkat, serta action buttons: Unduh, Perbesar, Salin, Studio.
 */
@Composable
fun CleanImageShowcaseCard(
    prompt: String,
    imageUrl: String,
    modifier: Modifier = Modifier,
    onOpenStudio: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showFullscreen by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Menangani skema base64 secara aman untuk Coil
    val imageModel: Any = remember(imageUrl) {
        if (imageUrl.startsWith("data:image/") && imageUrl.contains("base64,")) {
            try {
                val cleanB64 = imageUrl.substringAfter("base64,")
                android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
            } catch (_: Exception) {
                imageUrl
            }
        } else {
            imageUrl
        }
    }

    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullscreen = false },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                        .build(),
                    contentDescription = prompt,
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
                    onClick = { showFullscreen = false },
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

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = M3SurfaceContainer,
        border = BorderStroke(1.dp, M3OutlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
    ) {
        Column {
            // Wadah Gambar Utama
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 420.dp)
                    .background(M3SurfaceContainerLowest)
                    .clickable { showFullscreen = true },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                        .build(),
                    contentDescription = prompt,
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp),
                                color = M3Primary
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Gagal memuat visual",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                )
            }

            // Bottom Bar: Prompt ringkas & Tombol Aksi (Unduh, Zoom, Salin, Studio)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(M3SurfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = prompt.ifBlank { "Ilustrasi Visual AI" },
                    style = MaterialTheme.typography.bodySmall,
                    color = M3TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Unduh ke Galeri
                    IconButton(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                scope.launch {
                                    val success = saveImageToGallery(context, imageUrl)
                                    isSaving = false
                                    val msg = if (success) "Gambar berhasil disimpan ke Galeri!" else "Gagal menyimpan gambar."
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = M3Primary)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Unduh Gambar",
                                tint = M3TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Tombol Perbesar / Lightbox
                    IconButton(
                        onClick = { showFullscreen = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ZoomIn,
                            contentDescription = "Perbesar Gambar",
                            tint = M3TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tombol Salin Prompt
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(prompt))
                            Toast.makeText(context, "Prompt visual disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Salin Prompt",
                            tint = M3TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Tombol Buka di Image Gen Studio (Opsi ubah rasio)
                    if (onOpenStudio != null) {
                        IconButton(
                            onClick = onOpenStudio,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = "Buka di Studio",
                                tint = M3Primary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Menyimpan gambar ke MediaStore (Galeri Pictures/PalapaGO) secara aman.
 */
private suspend fun saveImageToGallery(context: Context, imageUriOrUrl: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val filename = "PalapaGO_${System.currentTimeMillis()}.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PalapaGO")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext false

        resolver.openOutputStream(uri)?.use { outputStream ->
            if (imageUriOrUrl.startsWith("file://")) {
                val filePath = imageUriOrUrl.removePrefix("file://")
                FileInputStream(File(filePath)).use { input -> input.copyTo(outputStream) }
            } else if (imageUriOrUrl.startsWith("data:image/") && imageUriOrUrl.contains("base64,")) {
                val b64 = imageUriOrUrl.substringAfter("base64,")
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                outputStream.write(bytes)
            } else {
                val connection = URL(imageUriOrUrl).openConnection()
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.getInputStream().use { input -> input.copyTo(outputStream) }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        true
    } catch (_: Exception) {
        false
    }
}
