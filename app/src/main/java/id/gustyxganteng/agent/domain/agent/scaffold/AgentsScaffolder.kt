package id.gustyxganteng.agent.domain.agent.scaffold

import java.io.File

class AgentsScaffolder(
    private val languageDetector: LanguageDetector = LanguageDetector()
) {

    fun hasAgentsConfig(workspacePath: String?): Boolean {
        if (workspacePath.isNullOrBlank()) return false
        val file = File(workspacePath, "AGENTS.md")
        return file.exists() && file.isFile && file.length() > 0
    }

    fun scaffoldAgentsMdIfNeeded(
        workspacePath: String?,
        userPrompt: String
    ): File? {
        if (workspacePath.isNullOrBlank()) return null
        val workspaceDir = File(workspacePath)
        if (!workspaceDir.exists() || !workspaceDir.isDirectory) return null

        val targetFile = File(workspaceDir, "AGENTS.md")
        // Immutability Rule: If it already exists, never overwrite or translate
        if (targetFile.exists() && targetFile.length() > 0) {
            return null
        }

        val language = languageDetector.detectLanguage(userPrompt)
        val projectName = workspaceDir.name.ifBlank { "Workspace Project" }
        val detectedStack = detectProjectStack(workspaceDir)

        val content = if (language == SupportedLanguage.INDONESIAN) {
            buildIndonesianAgentsTemplate(projectName, detectedStack)
        } else {
            buildEnglishAgentsTemplate(projectName, detectedStack)
        }

        targetFile.writeText(content)

        // Ensure .agents/rules and .agents/skills directories exist
        File(workspaceDir, ".agents/rules").mkdirs()
        File(workspaceDir, ".agents/skills").mkdirs()

        return targetFile
    }

    private fun detectProjectStack(dir: File): String {
        return when {
            File(dir, "build.gradle.kts").exists() || File(dir, "build.gradle").exists() ->
                "Android / Kotlin (Gradle)"
            File(dir, "package.json").exists() ->
                "Node.js / TypeScript / Web"
            File(dir, "requirements.txt").exists() || File(dir, "pyproject.toml").exists() ->
                "Python"
            File(dir, "Cargo.toml").exists() ->
                "Rust"
            File(dir, "go.mod").exists() ->
                "Go"
            else ->
                "General Computing / Multi-purpose Workspace"
        }
    }

    private fun buildEnglishAgentsTemplate(projectName: String, detectedStack: String): String {
        return """
        # AGENTS.md — $projectName
        # Architectural Standards & AI Agent Guidelines

        This document defines mandatory guidelines, architecture invariants, and security boundaries for AI Coding Agents operating within this repository.

        ---

        ## 1. Project Overview & Architecture
        - **Project Name**: $projectName
        - **Detected Technology Stack**: $detectedStack
        - **Architectural Pattern**: Modular Clean Architecture with Unidirectional Data Flow (UDF).
        - **Dependency Management**: Encapsulate external dependencies behind domain interfaces.

        ---

        ## 2. Engineering Standards & Code Style
        - **Type Safety & Idiomatic Code**: Strictly enforce language-idiomatic constructs and compile-time type safety.
        - **Concurrency & Asynchrony**: Use structured concurrency with safe lifecycle scopes. Handle exceptions defensively at boundary layers.
        - **Documentation & Comments**:
          * Zero narrator comments (e.g., '// Initialize variable', '// Button click'). Write comments strictly for non-obvious WHY reasons.
          * Zero lazy placeholders (e.g., '// TODO', '// ... rest of code'). Provide complete, functional code.

        ---

        ## 3. Workspace Security & Sandboxing
        - **Boundary Isolation**: All file operations (`write_file`, `apply_diff`, `read_file`) and shell executions must remain within this workspace root.
        - **Path Traversal Protection**: Never access parent paths outside the active directory.
        - **Human-in-the-Loop Governance**: Inspect and verify changes via diff approval dialogs before persisting modifications.
        - **Sensitive Data Shield**: Never hardcode, commit, or log credentials, secret keys, or `.env` files.

        ---

        ## 4. Communication & Anti-Slop Directive
        - Be direct, professional, and concise. Omit conversational filler and sycophantic greetings.
        - Ban generic marketing clichés (*delve, leverage, seamless, cutting-edge, solusi komprehensif*).
        """.trimIndent()
    }

    private fun buildIndonesianAgentsTemplate(projectName: String, detectedStack: String): String {
        return """
        # AGENTS.md — $projectName
        # Standar Arsitektur & Panduan AI Coding Agent

        Dokumen ini menjadi rujukan utama bagi AI Coding Agent dalam membangun, memelihara, dan mengembangkan fitur-fitur pada repositori ini.

        ---

        ## 1. Ikhtisar & Arsitektur Proyek
        - **Nama Proyek**: $projectName
        - **Teknologi Terdeteksi**: $detectedStack
        - **Pola Desain**: Clean Architecture modular dengan Unidirectional Data Flow (UDF).
        - **Manajemen Ketergantungan**: Pisahkan logika bisnis domain dari pustaka kerangka kerja eksternal.

        ---

        ## 2. Standar Rekayasa Perangkat Lunak & Gaya Kode
        - **Tipe Data & Idiomatic Code**: Terapkan penulisan kode yang lugas, aman, dan mematuhi konvensi bahasa resmi.
        - **Penanganan Asinkron**: Terapkan konkurensi terstruktur (*structured concurrency*) dengan penanganan kesalahan pada lapisan batas.
        - **Komentar & Dokumentasi**:
          * Dilarang menulis komentar narator (misal: '// Inisialisasi variabel'). Tulis komentar hanya untuk alasan *MENGAPA* (*WHY*).
          * Dilarang keras menggunakan *placeholder* malas (misal: '// TODO', '// ... sisa kode'). Berikan implementasi lengkap dan fungsional.

        ---

        ## 3. Keamanan Workspace & Batasan Direktori
        - **Isolasi Batas**: Seluruh manipulasi berkas dan eksekusi skrip wajib dibatasi di dalam folder proyek ini.
        - **Pencegahan Path Traversal**: Dilarang mengakses berkas di luar batas root proyek tanpa izin eksplisit.
        - **Human-in-the-Loop**: Tinjau perubahan secara visual melalui dialog *diff approval* sebelum disimpan permanen.
        - **Perlindungan Berkas Sensitif**: Jangan pernah mengekspos atau mencatat kunci API, token, atau berkas rahasia.

        ---

        ## 4. Komunikasi & Standar Anti-AI-Slop
        - Mulai langsung ke inti jawaban teknis tanpa basa-basi atau pujian berlebihan (*"Tentu!"*, *"Siap!"*).
        - Larang kata klise AI: *delve, leverage, seamless, inovasi terdepan, solusi komprehensif, di era digital*.
        """.trimIndent()
    }
}
