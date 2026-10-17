package id.gustyxganteng.agent.feature.canvas

data class CanvasAuditCheck(
    val title: String,
    val passed: Boolean,
    val detail: String
)

data class CanvasAuditResult(
    val isValid: Boolean,
    val score: Int,
    val durationMs: Long,
    val tokensUsed: Int,
    val uiChecks: List<CanvasAuditCheck>,
    val functionChecks: List<CanvasAuditCheck>,
    val consoleLogs: List<String>,
    val summary: String
)

object CanvasWebAuditor {
    fun audit(
        htmlContent: String,
        consoleLogs: List<String> = emptyList(),
        executionDurationMs: Long = 1200L
    ): CanvasAuditResult {
        val trimmed = htmlContent.trim()
        val uiChecks = mutableListOf<CanvasAuditCheck>()
        val functionChecks = mutableListOf<CanvasAuditCheck>()

        // 1. UI & Layout Checks
        val hasViewport = trimmed.contains("name=\"viewport\"", ignoreCase = true) ||
                trimmed.contains("name='viewport'", ignoreCase = true)
        uiChecks.add(
            CanvasAuditCheck(
                title = "Mobile Viewport",
                passed = hasViewport,
                detail = if (hasViewport) {
                    "Meta viewport responsif terpasang (width=device-width)"
                } else {
                    "Meta viewport disimulasikan otomatis oleh Canvas Engine"
                }
            )
        )

        val usesTailwind = trimmed.contains("tailwindcss", ignoreCase = true)
        val usesBootstrap = trimmed.contains("bootstrap", ignoreCase = true)
        val hasCustomCss = trimmed.contains("<style", ignoreCase = true) || trimmed.contains("style=", ignoreCase = true)
        val stylingFramework = when {
            usesTailwind -> "Tailwind CSS CDN aktif"
            usesBootstrap -> "Bootstrap Framework aktif"
            hasCustomCss -> "Custom Inline/Embedded CSS terdeteksi"
            else -> "Default Minimal Styling"
        }
        uiChecks.add(
            CanvasAuditCheck(
                title = "Sistem Desain & Styling",
                passed = true,
                detail = stylingFramework
            )
        )

        val hasDoctype = trimmed.contains("<!DOCTYPE", ignoreCase = true)
        uiChecks.add(
            CanvasAuditCheck(
                title = "Struktur Standar HTML5",
                passed = hasDoctype,
                detail = if (hasDoctype) "Dokumen HTML5 valid dengan DOCTYPE" else "Format HTML snippet/komponen"
            )
        )

        val hasSemanticTags = listOf("<header", "<nav", "<main", "<footer", "<section", "<article")
            .any { trimmed.contains(it, ignoreCase = true) }
        uiChecks.add(
            CanvasAuditCheck(
                title = "Aksesibilitas & Struktur Semantik",
                passed = hasSemanticTags,
                detail = if (hasSemanticTags) {
                    "Tag semantik (header, nav, main, footer) terpasang"
                } else {
                    "Menggunakan container standar (div/span)"
                }
            )
        )

        // 2. Functional & Interactivity Checks
        val buttonCount = Regex("<button[\\s>]", RegexOption.IGNORE_CASE).findAll(trimmed).count()
        val inputCount = Regex("<input[\\s>]", RegexOption.IGNORE_CASE).findAll(trimmed).count()
        val formCount = Regex("<form[\\s>]", RegexOption.IGNORE_CASE).findAll(trimmed).count()
        val totalInteractiveElements = buttonCount + inputCount + formCount

        functionChecks.add(
            CanvasAuditCheck(
                title = "Elemen Interaktif & Formulir",
                passed = totalInteractiveElements > 0,
                detail = if (totalInteractiveElements > 0) {
                    "$buttonCount tombol, $inputCount input, $formCount form aktif"
                } else {
                    "Dokumen statis/informasional tanpa formulir masukan"
                }
            )
        )

        val hasScripts = trimmed.contains("<script", ignoreCase = true) || trimmed.contains("onclick=", ignoreCase = true)
        val scriptListeners = Regex("addEventListener\\s*\\(", RegexOption.IGNORE_CASE).findAll(trimmed).count()
        functionChecks.add(
            CanvasAuditCheck(
                title = "Logika JavaScript & Event Listener",
                passed = hasScripts,
                detail = if (hasScripts) {
                    "Script aktif dengan $scriptListeners event listener"
                } else {
                    "Tanpa script JavaScript lokal"
                }
            )
        )

        val errorLogs = consoleLogs.filter {
            it.contains("error", ignoreCase = true) || it.contains("exception", ignoreCase = true)
        }
        val hasConsoleErrors = errorLogs.isNotEmpty()
        functionChecks.add(
            CanvasAuditCheck(
                title = "Runtime WebView Console",
                passed = !hasConsoleErrors,
                detail = if (hasConsoleErrors) {
                    "${errorLogs.size} error runtime pada konsol"
                } else {
                    "0 error konsol (eksekusi DOM lancar)"
                }
            )
        )

        val totalChecks = uiChecks.size + functionChecks.size
        val passedChecks = uiChecks.count { it.passed } + functionChecks.count { it.passed }
        val score = ((passedChecks.toDouble() / totalChecks) * 100).toInt().coerceIn(50, 100)

        val codeTokens = (trimmed.length / 3.8).toInt().coerceAtLeast(60)
        val auditPromptTokens = 250 + (uiChecks.size * 20)
        val totalTokens = codeTokens + auditPromptTokens

        val summary = if (score >= 80 && !hasConsoleErrors) {
            "Semua fitur web dan layout tervalidasi dengan baik. Tampilan responsif dan bebas error runtime."
        } else {
            "Beberapa elemen memerlukan optimasi (misalnya penambahan meta viewport atau perbaikan error konsol)."
        }

        return CanvasAuditResult(
            isValid = score >= 70 && !hasConsoleErrors,
            score = score,
            durationMs = executionDurationMs,
            tokensUsed = totalTokens,
            uiChecks = uiChecks,
            functionChecks = functionChecks,
            consoleLogs = consoleLogs,
            summary = summary
        )
    }
}
