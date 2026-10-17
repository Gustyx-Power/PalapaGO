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
                    description = "Mencari kemunculan kata kunci atau pola regex di seluruh file workspace dengan konteks baris sekitar.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "query" to PropertySchema("string", "Teks atau pola regex pencarian."),
                            "file_pattern" to PropertySchema("string", "Ekstensi atau pola file opsional (contoh: .c, .h, .kt, Makefile)."),
                            "is_regex" to PropertySchema("boolean", "Set true jika query berupa regular expression (default false)."),
                            "context_lines" to PropertySchema("integer", "Jumlah baris konteks sebelum dan sesudah match (default 2)."),
                            "case_sensitive" to PropertySchema("boolean", "Set true untuk pencarian case-sensitive (default false).")
                        ),
                        required = listOf("query")
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "search_code_symbols",
                    description = "Mencari deklarasi simbol kode (fungsi, struct, class, macro #define, enum, typedef, hook, atau opsi Kconfig CONFIG_...) di seluruh workspace dengan cuplikan definisi lengkap dan nomor baris.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "symbol" to PropertySchema("string", "Nama simbol yang dicari (contoh: 'anubis_hook', 'do_execve', 'task_struct', 'CONFIG_ANUBIS')."),
                            "file_pattern" to PropertySchema("string", "Filter pola file opsional (contoh: '.c', '.h', '.kt', 'Makefile').")
                        ),
                        required = listOf("symbol")
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "apply_unified_patch",
                    description = "Menerapkan unified diff / patch (format standard patch dengan hunk @@ -l,s +l,s @@) ke file di workspace. Mendukung penyesuaian offset baris otomatis (toleransi fuzzy matching) saat mem-patch kernel Linux / Android.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "path" to PropertySchema("string", "Path file target yang akan di-patch relatif terhadap workspace root."),
                            "patch_content" to PropertySchema("string", "Konten unified patch lengkap atau hunk diff (termasuk baris context, '-' dan '+').")
                        ),
                        required = listOf("path", "patch_content")
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
            val isRegex = args["is_regex"]?.jsonPrimitive?.booleanOrNull ?: false
            val contextLines = (args["context_lines"]?.jsonPrimitive?.intOrNull ?: 2).coerceIn(0, 5)
            val caseSensitive = args["case_sensitive"]?.jsonPrimitive?.booleanOrNull ?: false

            val regex = try {
                val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                if (isRegex) Regex(query, options) else Regex(Regex.escape(query), options)
            } catch (e: Exception) {
                return ToolExecutionResult(toolCallId, "search_files", stderr = "Regex tidak valid: ${e.message}", isSuccess = false)
            }

            val rootFile = File(WorkspacePathResolver.normalizePath(workspaceRoot)).canonicalFile
            val sb = StringBuilder()
            var matchCount = 0

            rootFile.walkTopDown()
                .filter { !it.isDirectory && !it.path.contains("/.") && !it.path.contains("/build/") && !it.path.contains("/node_modules/") }
                .filter { pattern == null || it.name.contains(pattern, ignoreCase = true) }
                .forEach { file ->
                    if (matchCount >= 40) return@forEach
                    val lines = try { file.readLines() } catch (_: Exception) { emptyList() }
                    val relPath = file.relativeTo(rootFile).path

                    lines.forEachIndexed { idx, line ->
                        if (regex.containsMatchIn(line)) {
                            matchCount++
                            if (matchCount <= 40) {
                                if (contextLines == 0) {
                                    sb.appendLine("$relPath:${idx + 1}: ${line.trim()}")
                                } else {
                                    sb.appendLine("--- $relPath (baris ${idx + 1}) ---")
                                    val start = (idx - contextLines).coerceAtLeast(0)
                                    val end = (idx + contextLines).coerceAtMost(lines.size - 1)
                                    for (cIdx in start..end) {
                                        val marker = if (cIdx == idx) ">" else " "
                                        sb.appendLine("$marker ${cIdx + 1} | ${lines[cIdx]}")
                                    }
                                    sb.appendLine()
                                }
                            }
                        }
                    }
                }

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "search_files",
                stdout = sb.toString().trim().ifEmpty { "Tidak ada kemunculan '$query' yang ditemukan." },
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(toolCallId, "search_files", stderr = e.message ?: "Error saat search_files", isSuccess = false)
        }
    }

    fun searchCodeSymbols(toolCallId: String, workspaceRoot: String, args: JsonObject): ToolExecutionResult {
        return try {
            val symbol = args["symbol"]?.jsonPrimitive?.content?.trim()
                ?: return ToolExecutionResult(toolCallId, "search_code_symbols", stderr = "Parameter 'symbol' wajib diisi.", isSuccess = false)
            val pattern = args["file_pattern"]?.jsonPrimitive?.content

            val rootFile = File(WorkspacePathResolver.normalizePath(workspaceRoot)).canonicalFile
            val escaped = Regex.escape(symbol)
            val symbolRegexes = listOf(
                Regex("""^\s*#\s*define\s+$escaped\b"""),
                Regex("""\b(struct|union|enum|class)\s+$escaped\b"""),
                Regex("""\btypedef\s+.*\b$escaped\b\s*;"""),
                Regex("""\b(?:[A-Za-z0-9_*]+\s+)+$escaped\s*\("""),
                Regex("""\b(config|CONFIG_)\s*$escaped\b"""),
                Regex("""\b$escaped\b""")
            )

            val sb = StringBuilder()
            var matchCount = 0

            rootFile.walkTopDown()
                .filter { !it.isDirectory && !it.path.contains("/.") && !it.path.contains("/build/") && !it.path.contains("/node_modules/") }
                .filter { pattern == null || it.name.contains(pattern, ignoreCase = true) }
                .forEach { file ->
                    if (matchCount >= 20) return@forEach
                    val lines = try { file.readLines() } catch (_: Exception) { emptyList() }
                    val relPath = file.relativeTo(rootFile).path

                    lines.forEachIndexed { idx, line ->
                        if (symbolRegexes.any { it.containsMatchIn(line) }) {
                            matchCount++
                            if (matchCount <= 20) {
                                val start = (idx - 2).coerceAtLeast(0)
                                val end = (idx + 8).coerceAtMost(lines.size - 1)
                                sb.appendLine("=== Simbol '$symbol' di $relPath (baris ${idx + 1}) ===")
                                for (cIdx in start..end) {
                                    val marker = if (cIdx == idx) ">" else " "
                                    sb.appendLine("$marker ${cIdx + 1} | ${lines[cIdx]}")
                                }
                                sb.appendLine()
                            }
                        }
                    }
                }

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "search_code_symbols",
                stdout = sb.toString().trim().ifEmpty { "Deklarasi atau penggunaan simbol '$symbol' tidak ditemukan di workspace." },
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(toolCallId, "search_code_symbols", stderr = e.message ?: "Error saat search_code_symbols", isSuccess = false)
        }
    }

    fun applyUnifiedPatch(toolCallId: String, workspaceRoot: String, args: JsonObject): ToolExecutionResult {
        return try {
            val relPath = args["path"]?.jsonPrimitive?.content
                ?: return ToolExecutionResult(toolCallId, "apply_unified_patch", stderr = "Parameter 'path' wajib diisi.", isSuccess = false)
            val patchContent = args["patch_content"]?.jsonPrimitive?.content
                ?: return ToolExecutionResult(toolCallId, "apply_unified_patch", stderr = "Parameter 'patch_content' wajib diisi.", isSuccess = false)

            val targetFile = resolveSafeFile(workspaceRoot, relPath)
            if (!targetFile.exists() || !targetFile.isFile) {
                return ToolExecutionResult(toolCallId, "apply_unified_patch", stderr = "File '$relPath' tidak ditemukan.", isSuccess = false)
            }

            val fileLines = targetFile.readLines().toMutableList()
            val patchLines = patchContent.lines()

            var i = 0
            var hunksApplied = 0
            val hunkOffsets = mutableListOf<String>()

            while (i < patchLines.size) {
                val line = patchLines[i]
                if (line.startsWith("@@")) {
                    val headerMatch = Regex("""@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@(.*)""").find(line)
                    val originalStart = headerMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    i++

                    val expectedLines = mutableListOf<String>()
                    val replacementLines = mutableListOf<String>()

                    while (i < patchLines.size && !patchLines[i].startsWith("@@") && !patchLines[i].startsWith("--- ") && !patchLines[i].startsWith("diff --git")) {
                        val pl = patchLines[i]
                        when {
                            pl.startsWith(" ") -> {
                                expectedLines.add(pl.substring(1))
                                replacementLines.add(pl.substring(1))
                            }
                            pl.startsWith("-") -> {
                                expectedLines.add(pl.substring(1))
                            }
                            pl.startsWith("+") -> {
                                replacementLines.add(pl.substring(1))
                            }
                        }
                        i++
                    }

                    if (expectedLines.isEmpty()) continue

                    val targetMatch = findHunkPosition(fileLines, expectedLines, originalStart - 1)
                    if (targetMatch == -1) {
                        return ToolExecutionResult(
                            toolCallId = toolCallId,
                            toolName = "apply_unified_patch",
                            stderr = "Gagal menerapkan hunk pada '$relPath'. Blok baris sebelum patch tidak cocok:\n" +
                                expectedLines.take(5).joinToString("\n") { "  $it" },
                            isSuccess = false
                        )
                    }

                    for (del in 0 until expectedLines.size) {
                        fileLines.removeAt(targetMatch)
                    }
                    fileLines.addAll(targetMatch, replacementLines)
                    hunksApplied++
                    val offset = (targetMatch + 1) - originalStart
                    hunkOffsets.add("Hunk $hunksApplied @ baris ${targetMatch + 1} (offset $offset)")
                    continue
                }
                i++
            }

            if (hunksApplied == 0) {
                return ToolExecutionResult(
                    toolCallId = toolCallId,
                    toolName = "apply_unified_patch",
                    stderr = "Tidak ada hunk valid (dimulai dengan @@ ... @@) yang ditemukan dalam patch_content.",
                    isSuccess = false
                )
            }

            targetFile.writeText(fileLines.joinToString("\n"))

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "apply_unified_patch",
                stdout = "Berhasil menerapkan $hunksApplied hunk ke '$relPath':\n${hunkOffsets.joinToString("\n")}",
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(toolCallId, "apply_unified_patch", stderr = e.message ?: "Error saat apply_unified_patch", isSuccess = false)
        }
    }

    private fun findHunkPosition(fileLines: List<String>, expectedLines: List<String>, preferredIdx: Int): Int {
        if (expectedLines.isEmpty() || expectedLines.size > fileLines.size) return -1

        // 1. Coba exact match di preferredIdx
        if (preferredIdx in 0..(fileLines.size - expectedLines.size)) {
            var match = true
            for (j in expectedLines.indices) {
                if (fileLines[preferredIdx + j].trimEnd() != expectedLines[j].trimEnd()) {
                    match = false
                    break
                }
            }
            if (match) return preferredIdx
        }

        // 2. Sliding window radius +/- 100 baris
        val radius = 100
        val minIdx = (preferredIdx - radius).coerceAtLeast(0)
        val maxIdx = (preferredIdx + radius).coerceAtMost(fileLines.size - expectedLines.size)

        for (candidate in minIdx..maxIdx) {
            var match = true
            for (j in expectedLines.indices) {
                if (fileLines[candidate + j].trimEnd() != expectedLines[j].trimEnd()) {
                    match = false
                    break
                }
            }
            if (match) return candidate
        }

        // 3. Scan seluruh file dengan normalisasi trim
        for (candidate in 0..(fileLines.size - expectedLines.size)) {
            var match = true
            for (j in expectedLines.indices) {
                if (fileLines[candidate + j].trim() != expectedLines[j].trim()) {
                    match = false
                    break
                }
            }
            if (match) return candidate
        }

        return -1
    }
}
