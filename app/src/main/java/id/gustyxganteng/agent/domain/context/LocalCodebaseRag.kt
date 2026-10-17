package id.gustyxganteng.agent.domain.context

import id.gustyxganteng.agent.domain.tools.WorkspacePathResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

object LocalCodebaseRag {

    private val IGNORED_DIRS = setOf(
        ".git", ".gradle", ".idea", "build", "node_modules", "out", "target", ".cxx",
        "DCIM", "Download", "Android", "Pictures", "Music", "Movies"
    )

    private val CODE_EXTENSIONS = setOf(
        "c", "h", "cpp", "hpp", "cc", "S", "s", "kt", "kts", "java", "rs", "go",
        "py", "js", "ts", "jsx", "tsx", "sh", "bash", "patch", "diff"
    )

    private val STOP_WORDS = setOf(
        "yang", "dan", "dari", "bisa", "tolong", "coba", "dimasukin", "kontol",
        "adalah", "ini", "itu", "sama", "ama", "gw", "gua", "lu", "pada", "untuk",
        "buatkan", "tambahkan", "bikin", "gimana", "bagaimana", "mengapa", "kenapa",
        "the", "and", "with", "from", "have", "that", "this", "code", "file", "make",
        "help", "please", "like", "just", "into", "some", "what", "where", "when"
    )

    suspend fun retrieve(workspacePath: String, userPrompt: String): String? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(2500L) {
            try {
                val normalizedRoot = WorkspacePathResolver.normalizePath(workspacePath)
                val rootDir = File(normalizedRoot).canonicalFile
                if (!rootDir.exists() || !rootDir.isDirectory) return@withTimeoutOrNull null

                val keywords = extractKeywords(userPrompt)
                val hasExplicitCodeInPrompt = userPrompt.contains("```") || userPrompt.lines().count { it.contains("{") || it.contains(";") || it.startsWith("#") } >= 3

                val foundSnippets = if (keywords.isNotEmpty()) {
                    searchWorkspace(rootDir, keywords)
                } else emptyList()

                if (foundSnippets.isEmpty() && !hasExplicitCodeInPrompt) {
                    return@withTimeoutOrNull null
                }

                buildString {
                    appendLine("[GROUNDED CODE CONTEXT & RAG PROTOCOL]")
                    if (hasExplicitCodeInPrompt) {
                        appendLine("• DETEKSI KODE PENGGUNA: Pengguna telah melampirkan potongan kode/patch langsung dalam prompt. Prioritaskan pembedahan kode pengguna ini dan JANGAN beralih ke pencarian dokumentasi web umum.")
                    }
                    if (foundSnippets.isNotEmpty()) {
                        appendLine("• TEMUAN KODE DARI WORKSPACE LOKAL (${foundSnippets.size} cuplikan berbobot tinggi):")
                        for (snippet in foundSnippets) {
                            appendLine()
                            appendLine("--- Berkas: ${snippet.relativePath} (Baris ${snippet.startLine}-${snippet.endLine}) ---")
                            appendLine(snippet.code)
                        }
                        appendLine()
                        appendLine("Instruksi: Gunakan berkas lokal di atas sebagai acuan kebenaran utama (ground truth). Lakukan perubahan surgical diff pada berkas yang relevan.")
                    }
                }.trim()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun extractKeywords(prompt: String): List<String> {
        val tokens = prompt.split(Regex("[\\s,;!?\"'()\\[\\]{}<>=]+"))
            .map { it.trim().lowercase() }
            .filter { it.length >= 3 && !STOP_WORDS.contains(it) }

        val highPriorityIdentifiers = mutableListOf<String>()
        val normalKeywords = mutableListOf<String>()

        for (token in tokens) {
            if (token.contains("_") || token.contains(".") || token.any { it.isDigit() } ||
                token.startsWith("config_") || token in listOf("anubis", "berg", "execve", "syscall", "hook", "vfs", "kernel", "patch")) {
                highPriorityIdentifiers.add(token)
            } else {
                normalKeywords.add(token)
            }
        }

        return (highPriorityIdentifiers + normalKeywords).distinct().take(6)
    }

    private data class ScoredSnippet(
        val relativePath: String,
        val startLine: Int,
        val endLine: Int,
        val code: String,
        val score: Int
    )

    private fun searchWorkspace(rootDir: File, keywords: List<String>): List<ScoredSnippet> {
        val isRootStorage = rootDir.path == "/storage/emulated/0" || rootDir.path == "/sdcard"
        val maxDepth = if (isRootStorage) 2 else 5
        val maxFilesToCheck = if (isRootStorage) 80 else 300

        val scoredSnippets = mutableListOf<ScoredSnippet>()
        var filesChecked = 0

        val fileTree = rootDir.walkTopDown()
            .maxDepth(maxDepth)
            .onEnter { dir ->
                !IGNORED_DIRS.contains(dir.name) && !dir.name.startsWith(".")
            }

        for (file in fileTree) {
            if (filesChecked >= maxFilesToCheck) break
            if (file.isDirectory) continue

            val ext = file.extension.lowercase()
            val isExactConfig = file.name.equals("Makefile", ignoreCase = true) || file.name.equals("Kconfig", ignoreCase = true)
            if (!CODE_EXTENSIONS.contains(ext) && !isExactConfig) continue
            if (file.length() > 600_000) continue

            filesChecked++
            val relPath = file.relativeTo(rootDir).path
            val fileNameLower = file.name.lowercase()

            val lines = try {
                file.readLines()
            } catch (_: Exception) {
                continue
            }

            var bestFileScore = 0
            var bestMatchLine = -1

            for (kw in keywords) {
                if (fileNameLower.contains(kw)) {
                    bestFileScore += 80
                    if (bestMatchLine == -1 && lines.isNotEmpty()) bestMatchLine = 0
                }
            }

            lines.forEachIndexed { idx, line ->
                val lineLower = line.lowercase()
                for (kw in keywords) {
                    if (lineLower.contains(kw)) {
                        var lineScore = 15
                        if (line.contains("(") || line.contains("struct ") || line.contains("#define") || line.contains("void ") || line.contains("int ")) {
                            lineScore += 25
                        }
                        if (lineScore > bestFileScore) {
                            bestFileScore = lineScore
                            bestMatchLine = idx
                        }
                    }
                }
            }

            if (bestFileScore > 0 && bestMatchLine != -1) {
                val start = (bestMatchLine - 10).coerceAtLeast(0)
                val end = (bestMatchLine + 15).coerceAtMost(lines.size - 1)
                val snippetText = lines.subList(start, end + 1).mapIndexed { offset, s ->
                    val lineNum = start + offset + 1
                    "$lineNum | $s"
                }.joinToString("\n")

                scoredSnippets.add(
                    ScoredSnippet(
                        relativePath = relPath,
                        startLine = start + 1,
                        endLine = end + 1,
                        code = snippetText,
                        score = bestFileScore
                    )
                )
            }
        }

        return scoredSnippets.sortedByDescending { it.score }.take(3)
    }
}
