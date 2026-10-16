package id.gustyxganteng.agent.domain.tools

import id.gustyxganteng.agent.core.model.CanvasArtifact
import id.gustyxganteng.agent.core.model.FunctionSpec
import id.gustyxganteng.agent.core.model.PropertySchema
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.model.ToolExecutionResult
import id.gustyxganteng.agent.core.model.ToolParametersSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class CanvasTool(
    private val onCanvasCreatedOrUpdated: (CanvasArtifact) -> Unit
) {
    companion object {
        val DEFINITION = ToolDefinition(
            function = FunctionSpec(
                name = "open_canvas",
                description = "Membuka lembar Canvas interaktif untuk menampilkan dokumen atau kode program yang dapat diedit langsung oleh pengguna.",
                parameters = ToolParametersSchema(
                    properties = mapOf(
                        "title" to PropertySchema("string", "Judul dokumen atau file Canvas."),
                        "language" to PropertySchema("string", "Bahasa pemrograman (contoh: kotlin, html, markdown, python)."),
                        "content" to PropertySchema("string", "Isi teks atau kode lengkap Canvas."),
                        "file_path" to PropertySchema("string", "Path file opsional jika terhubung ke file lokal workspace.")
                    ),
                    required = listOf("title", "language", "content")
                )
            )
        )
    }

    fun execute(toolCallId: String, args: JsonObject): ToolExecutionResult {
        val title = args["title"]?.jsonPrimitive?.content ?: "Canvas Document"
        val language = args["language"]?.jsonPrimitive?.content ?: "markdown"
        val content = args["content"]?.jsonPrimitive?.content ?: ""
        val filePath = args["file_path"]?.jsonPrimitive?.content

        val artifact = CanvasArtifact(
            title = title,
            language = language,
            content = content,
            associatedFilePath = filePath
        )

        onCanvasCreatedOrUpdated(artifact)

        return ToolExecutionResult(
            toolCallId = toolCallId,
            toolName = "open_canvas",
            stdout = "Canvas '$title' ($language) berhasil dibuat dan ditampilkan ke pengguna.",
            isSuccess = true
        )
    }
}
