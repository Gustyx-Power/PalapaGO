package id.gustyxganteng.agent.core.network

import android.util.Base64
import id.gustyxganteng.agent.BuildConfig

object KeyVault {
    private const val XOR_KEY: Byte = 0x5A

    fun getAgnesApiKey(): String = deobfuscate(BuildConfig.AGNES_API_KEY)
    fun getAgnesApiKeySecondary(): String = deobfuscate(BuildConfig.AGNES_API_KEY_SECONDARY)

    fun getAgnesKeyPool(): List<String> {
        val list = mutableListOf<String>()
        val primary = getAgnesApiKey()
        if (primary.isNotBlank()) list.add(primary)
        val secondary = getAgnesApiKeySecondary()
        if (secondary.isNotBlank() && secondary != primary) list.add(secondary)
        return list
    }

    fun getAgnesBaseUrl(): String = BuildConfig.AGNES_BASE_URL.ifBlank { "https://apihub.agnes-ai.com/v1" }

    fun getGroqApiKey(): String = deobfuscate(BuildConfig.GROQ_API_KEY)
    fun getGroqApiKeySecondary(): String = deobfuscate(BuildConfig.GROQ_API_KEY_SECONDARY)

    fun getGroqKeyPool(): List<String> {
        val list = mutableListOf<String>()
        val primary = getGroqApiKey()
        if (primary.isNotBlank()) list.add(primary)
        val secondary = getGroqApiKeySecondary()
        if (secondary.isNotBlank() && secondary != primary) list.add(secondary)
        return list
    }

    fun getGeminiApiKey(): String = deobfuscate(BuildConfig.GEMINI_API_KEY)

    fun getTavilyApiKey(): String = deobfuscate(BuildConfig.TAVILY_API_KEY)

    fun getXemaruxApiKey(): String = deobfuscate(BuildConfig.XEMARUX_API_KEY)
    fun getXemaruxBaseUrl(): String = BuildConfig.XEMARUX_BASE_URL.ifBlank { "https://ai.xemarux.com/v1" }

    fun obfuscate(input: String): String {
        if (input.isEmpty()) return ""
        val bytes = input.toByteArray(Charsets.UTF_8)
        val masked = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor XOR_KEY.toInt()).toByte() }
        return Base64.encodeToString(masked, Base64.NO_WRAP)
    }

    fun deobfuscate(encoded: String): String {
        if (encoded.isBlank()) return ""
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val unmasked = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor XOR_KEY.toInt()).toByte() }
            String(unmasked, Charsets.UTF_8)
        } catch (_: Exception) {
            encoded
        }
    }
}
