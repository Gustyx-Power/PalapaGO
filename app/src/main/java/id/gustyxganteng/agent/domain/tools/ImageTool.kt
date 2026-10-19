package id.gustyxganteng.agent.domain.tools

import android.content.Context
import id.gustyxganteng.agent.core.model.FunctionSpec
import id.gustyxganteng.agent.core.model.PropertySchema
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.model.ToolParametersSchema
import id.gustyxganteng.agent.core.network.AgnesApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class ImageTool(
    private val agnesApiService: AgnesApiService,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val context: Context? = null
) {
    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    companion object {
        val DEFINITION = ToolDefinition(
            function = FunctionSpec(
                name = "generate_image",
                description = "Menghasilkan gambar ilustrasi visual, diagram visual konseptual, cover makalah ilmiah, sketsa desain, atau infografis menggunakan engine multimodal Agnes AI (agnes-image-2.5-flash). Gambar yang dihasilkan akan langsung ditampilkan di obrolan chat.",
                parameters = ToolParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "prompt" to PropertySchema(
                            type = "string",
                            description = "Deskripsi visual rinci dan terperinci mengenai gambar yang ingin dihasilkan dalam Bahasa Indonesia atau Inggris (contoh: 'clean flat vector illustration of biological cell mitosis diagram with clear labels, modern educational graphic')."
                        ),
                        "aspect_ratio" to PropertySchema(
                            type = "string",
                            description = "Rasio aspek: '1:1' (1024x1024), '9:16' (1024x1792), '16:9' (1792x1024), '3:4' (768x1024), atau '4:3' (1024x768). Default 1:1.",
                            enum = listOf("1:1", "9:16", "16:9", "3:4", "4:3", "1024x1024", "1024x1792", "1792x1024", "768x1024", "1024x768")
                        )
                    ),
                    required = listOf("prompt")
                )
            )
        )

        fun normalizeDimension(input: String?): String {
            return when (input?.trim()?.lowercase()) {
                "1:1", "square", "persegi" -> "1024x1024"
                "9:16", "portrait", "story", "vertikal", "reel" -> "1024x1792"
                "16:9", "landscape", "lanskap", "banner", "wallpaper" -> "1792x1024"
                "3:4", "poster", "potret" -> "768x1024"
                "4:3", "slide", "presentation", "presentasi" -> "1024x768"
                "1024x1024", "1024x1792", "1792x1024", "768x1024", "1024x768" -> input.trim()
                else -> "1024x1024"
            }
        }
    }

    /**
     * Mengembangkan prompt visual menjadi blueprint tingkat tinggi untuk Mode MAX dan memastikan tipografi tajam.
     */
    fun enhancePrompt(rawPrompt: String, isMaxMode: Boolean): String {
        val trimmed = rawPrompt.trim()
        if (!isMaxMode && trimmed.length > 150) return trimmed

        val lower = trimmed.lowercase()
        val isMangaOrComic = lower.contains("manga") || lower.contains("anime") || lower.contains("komik") || lower.contains("comic") || lower.contains("panel")
        val isWildfireElNino = (lower.contains("el nino") || lower.contains("kebakaran")) && (lower.contains("pemadam") || lower.contains("hutan"))

        val builder = StringBuilder()

        if (isMangaOrComic) {
            if (!lower.contains("panel")) {
                builder.append("Structured multi-panel Japanese manga anime page with 4 distinct rectangular panels separated by clean white gutters (panel borders). ")
            }
            if (isWildfireElNino) {
                builder.append("Panel 1 (Top Wide): devastating Indonesia forest wildfire during severe El Nino drought, orange sky, breaking news banner reading: \"LIVE: KEBAKARAN HUTAN MELUAS\". ")
                builder.append("Panel 2 (Middle Left): heroic Indonesian firefighters in red helmets spraying high-pressure water hoses into roaring inferno. ")
                builder.append("Panel 3 (Middle Right): intense close-up of young weary firefighter face with sweat and soot, eyes reflecting blazing fire, single speech bubble with large bold clearly printed word: \"PADAMKAN!\". ")
                builder.append("Panel 4 (Bottom Wide): stark contrast of corrupt politicians in a lavish luxury dining hall toasting champagne glasses ignoring the crisis, single speech bubble with large bold clearly printed word: \"BERSULANG!\". ")
            } else {
                builder.append(trimmed).append(". ")
            }
            builder.append("Dynamic Japanese manga anime key visual, cel-shaded ink lines, screentone shading, atmospheric volumetric smoke and glowing embers, dramatic chiaroscuro contrast. ")
            builder.append("All text strictly in large bold clearly legible Latin block capital letters: 'LIVE: KEBAKARAN HUTAN MELUAS', 'PADAMKAN!', 'BERSULANG!'. Maximum 1 single word per speech bubble. Absolutely NO multi-line text, NO unquoted filler bubbles, NO illegible scribbles, NO alien runes, zero pseudo-Japanese squiggles, 8k resolution.")
        } else {
            builder.append(trimmed)
            if (isMaxMode) {
                builder.append(", ultra-detailed, cinematic atmospheric lighting, photorealistic depth of field, sharp vectorized typography with clean legible lettering, masterwork composition, 8k resolution.")
            }
        }

        return builder.toString().trim()
    }

    /**
     * Menyimpan gambar yang digenerate langsung ke folder internal aplikasi (filesDir/generated_images).
     * Menghasilkan URI lokal "file:///..." yang permanen, tahan banting, dan tidak bergantung pada CDN sementara.
     */
    suspend fun persistImageLocally(urlOrBase64: String): String = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext urlOrBase64
        try {
            val dir = File(ctx.filesDir, "generated_images").apply { mkdirs() }
            val filename = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.png"
            val targetFile = File(dir, filename)

            if (urlOrBase64.startsWith("data:image/") && urlOrBase64.contains("base64,")) {
                val cleanB64 = urlOrBase64.substringAfter("base64,")
                val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                targetFile.writeBytes(bytes)
                return@withContext "file://${targetFile.absolutePath}"
            } else if (urlOrBase64.startsWith("http://") || urlOrBase64.startsWith("https://")) {
                val request = Request.Builder()
                    .url(urlOrBase64)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    .build()
                downloadClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            targetFile.writeBytes(bytes)
                            return@withContext "file://${targetFile.absolutePath}"
                        }
                    }
                }
            } else if (urlOrBase64.startsWith("file://")) {
                return@withContext urlOrBase64
            }
        } catch (_: Exception) {}
        urlOrBase64
    }

    suspend fun execute(
        argsJson: String,
        baseUrl: String,
        apiKey: String,
        isMaxMode: Boolean = false
    ): String {
        return try {
            val root = json.parseToJsonElement(argsJson).jsonObject
            val rawPrompt = root["prompt"]?.jsonPrimitive?.content ?: return "Error: Parameter 'prompt' wajib diisi."
            val rawSize = root["aspect_ratio"]?.jsonPrimitive?.content
            val size = normalizeDimension(rawSize)
            val effectivePrompt = enhancePrompt(rawPrompt, isMaxMode)

            val result = agnesApiService.generateImage(
                baseUrl = baseUrl,
                apiKey = apiKey,
                prompt = effectivePrompt,
                size = size
            )

            result.fold(
                onSuccess = { rawUrl ->
                    val persistedUrl = persistImageLocally(rawUrl)
                    "![$rawPrompt]($persistedUrl)\n\n*Gambar berhasil digenerate oleh Agnes Image 2.5 Flash ($size)*"
                },
                onFailure = { error ->
                    "Gagal menghasilkan gambar: ${error.message}"
                }
            )
        } catch (e: Exception) {
            "Error eksekusi generate_image: ${e.message}"
        }
    }
}
