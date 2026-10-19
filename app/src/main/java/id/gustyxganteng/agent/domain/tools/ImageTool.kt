package id.gustyxganteng.agent.domain.tools

import id.gustyxganteng.agent.core.model.FunctionSpec
import id.gustyxganteng.agent.core.model.PropertySchema
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.model.ToolParametersSchema
import id.gustyxganteng.agent.core.network.AgnesApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ImageTool(
    private val agnesApiService: AgnesApiService,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
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
                            description = "Dimensi gambar: '1024x1024' (persegi), '1024x1792' (potret), atau '1792x1024' (lanskap). Default 1024x1024.",
                            enum = listOf("1024x1024", "1024x1792", "1792x1024")
                        )
                    ),
                    required = listOf("prompt")
                )
            )
        )
    }

    suspend fun execute(
        argsJson: String,
        baseUrl: String,
        apiKey: String
    ): String {
        return try {
            val root = json.parseToJsonElement(argsJson).jsonObject
            val prompt = root["prompt"]?.jsonPrimitive?.content ?: return "Error: Parameter 'prompt' wajib diisi."
            val size = root["aspect_ratio"]?.jsonPrimitive?.content ?: "1024x1024"

            val result = agnesApiService.generateImage(
                baseUrl = baseUrl,
                apiKey = apiKey,
                prompt = prompt,
                size = size
            )

            result.fold(
                onSuccess = { url ->
                    "![${prompt.take(60)}]($url)\n\n*Gambar berhasil digenerate oleh Agnes Image 2.5 Flash ($size)*\n[Buka Resolusi Penuh]($url)"
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
