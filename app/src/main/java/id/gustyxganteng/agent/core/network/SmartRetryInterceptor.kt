package id.gustyxganteng.agent.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.random.Random

data class RateLimitStatus(
    val isThrottled: Boolean,
    val retryAfterSeconds: Long,
    val attempt: Int,
    val maxAttempts: Int,
    val message: String
)

class SmartRetryInterceptor(
    private val maxRetriesProvider: () -> Int = { 5 }
) : Interceptor {

    private val _rateLimitEvents = MutableSharedFlow<RateLimitStatus>(extraBufferCapacity = 10)
    val rateLimitEvents = _rateLimitEvents.asSharedFlow()

    private val cancelledFlag = AtomicBoolean(false)

    fun cancelActiveRetries() {
        cancelledFlag.set(true)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var attempt = 0
        val maxRetries = maxRetriesProvider()
        cancelledFlag.set(false)

        while (attempt <= maxRetries) {
            if (cancelledFlag.get()) {
                throw IOException("Request cancelled by user during rate limit retry.")
            }

            try {
                response?.close()
                response = chain.proceed(request)

                if (response.isSuccessful) {
                    _rateLimitEvents.tryEmit(
                        RateLimitStatus(
                            isThrottled = false,
                            retryAfterSeconds = 0,
                            attempt = attempt,
                            maxAttempts = maxRetries,
                            message = "OK"
                        )
                    )
                    return response
                }

                val code = response.code
                val isRetryable = code == 429 || code == 502 || code == 503 || code == 504

                if (!isRetryable || attempt >= maxRetries) {
                    return response
                }

                // Hitung durasi backoff
                val retryAfterHeader = response.header("Retry-After")?.toLongOrNull()
                val backoffSeconds: Long = if (retryAfterHeader != null && retryAfterHeader > 0) {
                    retryAfterHeader
                } else {
                    // Exponential backoff: 3s, 6s, 12s, 24s... + jitter
                    val baseDelayMs = 3000L * (1L shl attempt)
                    val cappedMs = min(baseDelayMs, 30000L)
                    val jitterMs = Random.nextLong(200, 1000)
                    (cappedMs + jitterMs) / 1000
                }

                attempt++
                val statusMsg = "Terkena Rate Limit API ($code). Mencoba lagi dalam ${backoffSeconds}s... (Percobaan $attempt/$maxRetries)"
                _rateLimitEvents.tryEmit(
                    RateLimitStatus(
                        isThrottled = true,
                        retryAfterSeconds = backoffSeconds,
                        attempt = attempt,
                        maxAttempts = maxRetries,
                        message = statusMsg
                    )
                )

                // Sleep sesuai backoff dengan polling cancellation
                val sleepEnd = System.currentTimeMillis() + (backoffSeconds * 1000)
                while (System.currentTimeMillis() < sleepEnd) {
                    if (cancelledFlag.get()) {
                        throw IOException("Dibatalkan oleh pengguna saat menunggu retry.")
                    }
                    Thread.sleep(250)
                }

            } catch (e: Exception) {
                if (cancelledFlag.get()) {
                    throw IOException("Dibatalkan saat retry.", e)
                }
                if (attempt >= maxRetries) {
                    throw e
                }
                attempt++
                Thread.sleep(1000L * attempt)
            }
        }

        return response ?: throw IOException("Gagal mengeksekusi request setelah $maxRetries percobaan.")
    }
}
