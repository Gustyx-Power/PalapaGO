package id.gustyxganteng.agent.core.network

import android.util.Base64
import id.gustyxganteng.agent.BuildConfig

object KeyVault {
    private const val XOR_KEY: Byte = 0x5A

    fun getAgnesApiKey(): String = deobfuscate(BuildConfig.AGNES_API_KEY)

    fun getAgnesBaseUrl(): String = BuildConfig.AGNES_BASE_URL.ifBlank { "https://apihub.agnes-ai.com/v1" }

    fun getTavilyApiKey(): String = deobfuscate(BuildConfig.TAVILY_API_KEY)

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
