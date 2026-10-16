package id.gustyxganteng.agent.domain.tools

import id.gustyxganteng.agent.core.model.FunctionSpec
import id.gustyxganteng.agent.core.model.PropertySchema
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.model.ToolExecutionResult
import id.gustyxganteng.agent.core.model.ToolParametersSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException

class FileSystemTool {

    companion object {
        val DEFINITIONS: List<ToolDefinition> = listOf(
            ToolDefinition(
                function = FunctionSpec(
                    name = "list_directory",
                    description = "Menampilkan daftar file dan direktori dalam workspace. Mengabaikan folder seperti .git, node_modules, dan build.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "path" to PropertySchema("string", "Path relatif terhadap root workspace. Kosongkan untuk root."),
                            "recursive" to PropertySchema("boolean", "Set true untuk menyusuri subdirektori (maks 3 tingkat kedalaman).")
                        )
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "read_file",
                    description = "Membaca isi file teks di dalam workspace dengan nomor baris.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "path" to PropertySchema("string", "Path file relatif terhadap workspace root."),
                            "start_line" to PropertySchema("integer", "Baris awal yang ingin dibaca (1-indexed)."),
                            "end_line" to PropertySchema("integer", "Baris akhir yang ingin dibaca (1-indexed).")
                        ),
                        required = listOf("path")
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "write_file",
                    description = "Menulis atau menimpa konten teks ke file di dalam workspace.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "path" to PropertySchema("string", "Path file relatif terhadap workspace root."),
                            "content" to PropertySchema("string", "Konten teks lengkap yang akan ditulis.")
                        ),
                        required = listOf("path", "content")
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "apply_diff",
                    description = "Mengganti blok kode spesifik dalam file tanpa perlu menulis ulang seluruh isi file.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "path" to PropertySchema("string", "Path file relatif terhadap workspace root."),
                            "search_block" to PropertySchema("string", "Blok kode persis yang ingin diganti."),
                            "replace_block" to PropertySchema("string", "Blok kode pengganti baru.")
                        ),
                        required = listOf("path", "search_block", "replace_block")
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "search_files",
                    description = "Mencari kemunculan kata kunci atau teks di seluruh file dalam workspace.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "query" to PropertySchema("string", "Teks pencarian."),
                            "file_pattern" to PropertySchema("string", "Ekstensi atau pola file opsional (contoh: .kt, .json).")
                        ),
                        required = listOf("query")
                    )
                )
            )
        )
    }

    private val ignoredDirectories = setOf(".git", "node_modules", "build", ".gradle", ".idea", ".cxx")

    fun resolveSafeFile(workspaceRoot: String, relativePath: String): File {
        val normalizedRoot = WorkspacePathResolver.normalizePath(workspaceRoot)
        val rootFile = File(normalizedRoot).canonicalFile
        rootFile.mkdirs()
        val targetFile = File(rootFile, relativePath).canonicalFile

        if (!targetFile.path.startsWith(rootFile.path)) {
            throw SecurityException("Akses ditolak: Path '$relativePath' berada di luar workspace '${rootFile.path}'")
        }
        targetFile.parentFile?.mkdirs()
        return targetFile
    }

    fun listDirectory(toolCallId: String, workspaceRoot: String, args: JsonObject): ToolExecutionResult {
        return try {
            val relPath = args["path"]?.jsonPrimitive?.content ?: ""
            val recursive = args["recursive"]?.jsonPrimitive?.booleanOrNull ?: false

            val dir = resolveSafeFile(workspaceRoot, relPath)
            if (!dir.exists()) {
                return ToolExecutionResult(toolCallId, "list_directory", stderr = "Direktori '$relPath' tidak ditemukan.", isSuccess = false)
            }
            if (!dir.isDirectory) {
                return ToolExecutionResult(toolCallId, "list_directory", stderr = "'$relPath' bukan sebuah direktori.", isSuccess = false)
            }

            val sb = StringBuilder()
            listRecursive(dir, dir, recursive, 0, sb)

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "list_directory",
                stdout = sb.toString().ifEmpty { "(Direktori kosong)" },
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(toolCallId, "list_directory", stderr = e.message ?: "Error saat list_directory", isSuccess = false)
        }
    }

    private fun listRecursive(baseDir: File, current: File, recursive: Boolean, depth: Int, sb: StringBuilder) {
        if (depth > 3) return
        val children = current.listFiles()?.sortedBy { it.name } ?: return

        for (child in children) {
            if (child.isDirectory && ignoredDirectories.contains(child.name)) continue
            val relative = child.relativeTo(baseDir).path
            if (child.isDirectory) {
                sb.appendLine("[DIR] $relative/")
                if (recursive) {
                    listRecursive(baseDir, child, true, depth + 1, sb)
                }
            } else {
                sb.appendLine("[FILE] $relative (${child.length()} bytes)")
            }
        }
    }

    fun readFile(toolCallId: String, workspaceRoot: String, args: JsonObject): ToolExecutionResult {
        return try {
            val relPath = args["path"]?.jsonPrimitive?.content
                ?: return ToolExecutionResult(toolCallId, "read_file", stderr = "Parameter 'path' wajib diisi.", isSuccess = false)

            val file = resolveSafeFile(workspaceRoot, relPath)
            if (!file.exists() || !file.isFile) {
                return ToolExecutionResult(toolCallId, "read_file", stderr = "File '$relPath' tidak ditemukan.", isSuccess = false)
            }

            val startLine = args["start_line"]?.jsonPrimitive?.intOrNull ?: 1
            val endLine = args["end_line"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE

            val lines = file.readLines()
            val sb = StringBuilder()
            val from = (startLine - 1).coerceAtLeast(0)
            val to = (endLine - 1).coerceAtMost(lines.size - 1)

            for (idx in from..to) {
                if (idx in lines.indices) {
                    sb.appendLine("${idx + 1} | ${lines[idx]}")
                }
            }

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "read_file",
                stdout = sb.toString(),
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(toolCallId, "read_file", stderr = e.message ?: "Error saat read_file", isSuccess = false)
        }
    }

    fun writeFile(toolCallId: String, workspaceRoot: String, args: JsonObject): ToolExecutionResult {
        return try {
            val relPath = args["path"]?.jsonPrimitive?.content
                ?: return ToolExecutionResult(toolCallId, "write_file", stderr = "Parameter 'path' wajib diisi.", isSuccess = false)
            val content = args["content"]?.jsonPrimitive?.content ?: ""

            val file = resolveSafeFile(workspaceRoot, relPath)
            file.parentFile?.mkdirs()
            file.writeText(content)

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "write_file",
                stdout = "Berhasil menulis ${content.length} karakter ke '$relPath'.",
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(toolCallId, "write_file", stderr = e.message ?: "Error saat write_file", isSuccess = false)
        }
    }

    fun applyDiff(toolCallId: String, workspaceRoot: String, args: JsonObject): ToolExecutionResult {
        return try {
            val relPath = args["path"]?.jsonPrimitive?.content
                ?: return ToolExecutionResult(toolCallId, "apply_diff", stderr = "Parameter 'path' wajib diisi.", isSuccess = false)
            val searchBlock = args["search_block"]?.jsonPrimitive?.content
                ?: return ToolExecutionResult(toolCallId, "apply_diff", stderr = "Parameter 'search_block' wajib diisi.", isSuccess = false)
            val replaceBlock = args["replace_block"]?.jsonPrimitive?.content ?: ""

            val file = resolveSafeFile(workspaceRoot, relPath)
            if (!file.exists()) {
                return ToolExecutionResult(toolCallId, "apply_diff", stderr = "File '$relPath' tidak ditemukan.", isSuccess = false)
            }

            val currentText = file.readText()
            if (!currentText.contains(searchBlock)) {
                return ToolExecutionResult(
                    toolCallId = toolCallId,
                    toolName = "apply_diff",
                    stderr = "Blok target tidak ditemukan dalam file '$relPath'. Periksa whitespace dan indentasi.",
                    isSuccess = false
                )
            }

            val newText = currentText.replaceFirst(searchBlock, replaceBlock)
            file.writeText(newText)

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "apply_diff",
                stdout = "Berhasil menerapkan perubahan pada '$relPath'.",
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(toolCallId, "apply_diff", stderr = e.message ?: "Error saat apply_diff", isSuccess = false)
        }
    }

    fun searchFiles(toolCallId: String, workspaceRoot: String, args: JsonObject): ToolExecutionResult {
        return try {
            val query = args["query"]?.jsonPrimitive?.content
                ?: return ToolExecutionResult(toolCallId, "search_files", stderr = "Parameter 'query' wajib diisi.", isSuccess = false)
            val pattern = args["file_pattern"]?.jsonPrimitive?.content

            val rootFile = File(workspaceRoot).canonicalFile
            val sb = StringBuilder()
            var matchCount = 0

            rootFile.walkTopDown()
                .filter { !it.isDirectory && !it.path.contains("/.") && !it.path.contains("/build/") && !it.path.contains("/node_modules/") }
                .filter { pattern == null || it.name.contains(pattern) }
                .forEach { file ->
                    val lines = try { file.readLines() } catch (_: Exception) { emptyList() }
                    lines.forEachIndexed { idx, line ->
                        if (line.contains(query, ignoreCase = true)) {
                            matchCount++
                            if (matchCount <= 50) {
                                sb.appendLine("${file.relativeTo(rootFile).path}:${idx + 1}: ${line.trim()}")
                            }
                        }
                    }
                }

            if (matchCount > 50) {
                sb.appendLine("... dan ${matchCount - 50} hasil lainnya.")
            }

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "search_files",
                stdout = sb.toString().ifEmpty { "Tidak ada kemunculan '$query' yang ditemukan." },
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(toolCallId, "search_files", stderr = e.message ?: "Error saat search_files", isSuccess = false)
        }
    }
}
