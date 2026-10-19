package id.gustyxganteng.agent.core.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

object JsonRepairUtil {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Memeriksa apakah string merupakan JSON valid.
     * Jika tidak valid (misal terpotong di tengah jalan karena batas token / max_tokens),
     * fungsi ini memperbaiki tanda kutip, kurung kurawal, dan kurung siku yang belum tertutup.
     * Menjamin output selalu berupa JSON string valid sehingga tidak ditolak oleh OpenAI-compatible API (HTTP 400).
     */
    fun repairOrSanitize(rawJson: String?): String {
        if (rawJson.isNullOrBlank()) return "{}"
        val trimmed = rawJson.trim()

        // 1. Jika sudah valid, langsung kembalikan
        try {
            json.parseToJsonElement(trimmed)
            return trimmed
        } catch (_: Exception) {}

        // 2. Coba auto-close unclosed string quotes dan brackets/braces
        val repaired = tryAutoRepair(trimmed)
        if (repaired != null) {
            try {
                json.parseToJsonElement(repaired)
                return repaired
            } catch (_: Exception) {}
        }

        // 3. Fallback ekstraksi heuristik sederhana (misal parameter path atau query)
        try {
            val pathMatch = Regex(""""path"\s*:\s*"([^"]+)"""").find(trimmed)
            if (pathMatch != null) {
                val p = pathMatch.groupValues[1]
                return """{"path":"$p"}"""
            }
            val queryMatch = Regex(""""query"\s*:\s*"([^"]+)"""").find(trimmed)
            if (queryMatch != null) {
                val q = queryMatch.groupValues[1]
                return """{"query":"$q"}"""
            }
        } catch (_: Exception) {}

        return "{}"
    }

    /**
     * Memperbaiki JSON terpotong (misal karena batas max_tokens).
     */
    private fun tryAutoRepair(s: String): String? {
        val sb = StringBuilder(s)
        var inString = false
        var isEscaped = false
        val stack = ArrayDeque<Char>()

        for (i in 0 until sb.length) {
            val c = sb[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                when (c) {
                    '{' -> stack.addLast('}')
                    '[' -> stack.addLast(']')
                    '}', ']' -> {
                        if (stack.isNotEmpty() && stack.last() == c) {
                            stack.removeLast()
                        }
                    }
                }
            }
        }

        // Jika terpotong saat di dalam string, tutup tanda kutip
        if (inString) {
            sb.append('"')
        }

        // Tutup sisa brackets dan braces
        while (stack.isNotEmpty()) {
            sb.append(stack.removeLast())
        }

        return sb.toString()
    }

    /**
     * Mengurai string JSON menjadi JsonObject secara aman dan anti-crash.
     */
    fun parseSafeObject(rawJson: String?): JsonObject {
        val sanitized = repairOrSanitize(rawJson)
        return try {
            json.parseToJsonElement(sanitized).jsonObject
        } catch (_: Exception) {
            buildJsonObject {}
        }
    }
}
