package id.gustyxganteng.agent.domain.agent.scaffold

enum class SupportedLanguage {
    INDONESIAN,
    ENGLISH
}

class LanguageDetector {

    private val indonesianIndicators = setOf(
        "yang", "untuk", "dengan", "dan", "dari", "dalam", "buatkan", "tolong",
        "saya", "ini", "itu", "pada", "bisa", "kamu", "bagaimana", "coba",
        "buat", "tambahkan", "ubah", "jalankan", "hapus", "adalah", "projek", "aplikasi"
    )

    private val englishIndicators = setOf(
        "the", "and", "for", "with", "from", "create", "build", "please",
        "make", "this", "that", "how", "what", "can", "you", "update",
        "add", "run", "delete", "is", "are", "project", "app", "code"
    )

    fun detectLanguage(prompt: String): SupportedLanguage {
        val tokens = prompt.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return SupportedLanguage.ENGLISH

        var idCount = 0
        var enCount = 0

        for (token in tokens) {
            if (indonesianIndicators.contains(token)) idCount++
            if (englishIndicators.contains(token)) enCount++
        }

        return when {
            idCount > enCount -> SupportedLanguage.INDONESIAN
            enCount > idCount -> SupportedLanguage.ENGLISH
            idCount > 0 -> SupportedLanguage.INDONESIAN
            else -> SupportedLanguage.ENGLISH // Default fallback to English
        }
    }
}
