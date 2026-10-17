package id.gustyxganteng.agent.domain.tools

import id.gustyxganteng.agent.core.model.FunctionSpec
import id.gustyxganteng.agent.core.model.PropertySchema
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.model.ToolExecutionResult
import id.gustyxganteng.agent.core.model.ToolParametersSchema
import id.gustyxganteng.agent.core.network.KeyVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebTool(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private val jinaClient: OkHttpClient = httpClient.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    companion object {
        val DEFINITIONS = listOf(
            ToolDefinition(
                function = FunctionSpec(
                    name = "web_search",
                    description = "Mencari informasi terkini, referensi kode, dokumentasi API, dan pustaka dari internet menggunakan DuckDuckGo Lite dengan ekstraksi konten mendalam (atau Tavily Deep Search jika diaktifkan).",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "query" to PropertySchema("string", "Kata kunci pencarian referensi atau dokumentasi."),
                            "max_results" to PropertySchema("integer", "Jumlah hasil maksimal yang ingin diambil (default 5).")
                        ),
                        required = listOf("query")
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "wikipedia_search",
                    description = "Mencari ringkasan ensiklopedia Wikipedia terverifikasi untuk spesifikasi hardware, profil perusahaan/tokoh, definisi konsep teknis, sejarah, atau latar belakang ilmiah. Gunakan tool ini terlebih dahulu untuk topik ensiklopedis sebelum web_search.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "query" to PropertySchema("string", "Topik, entitas, istilah teknis, perangkat, atau tokoh yang ingin dicari di Wikipedia."),
                            "lang" to PropertySchema("string", "Kode bahasa Wikipedia (default 'id'). Otomatis fallback ke 'en' jika artikel tidak ditemukan.")
                        ),
                        required = listOf("query")
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "read_webpage",
                    description = "Mengambil isi lengkap halaman web atau artikel dokumentasi dari URL dan mengekstraknya menjadi teks Markdown yang bersih.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "url" to PropertySchema("string", "URL halaman web atau dokumentasi yang ingin dibaca.")
                        ),
                        required = listOf("url")
                    )
                )
            )
        )
    }

    suspend fun search(
        toolCallId: String,
        args: JsonObject,
        useTavilyDeepSearch: Boolean = false,
        isMaxMode: Boolean = false
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val query = args["query"]?.jsonPrimitive?.content?.trim()
            ?: return@withContext ToolExecutionResult(toolCallId, "web_search", stderr = "Query pencarian kosong.", isSuccess = false)
        val defaultLimit = if (isMaxMode) 8 else 5
        val maxResults = args["max_results"]?.jsonPrimitive?.intOrNull ?: defaultLimit

        val tavilyApiKey = KeyVault.getTavilyApiKey().trim()

        if (useTavilyDeepSearch && tavilyApiKey.isNotBlank()) {
            val tavilyResult = searchTavily(toolCallId, query, isFallback = false, isMaxMode = isMaxMode)
            if (tavilyResult.isSuccess) {
                return@withContext tavilyResult
            }
        }

        var ddgError: String? = null
        var ddgItems: List<SearchResultItem> = emptyList()

        try {
            ddgItems = searchDuckDuckGo(query, maxResults)
        } catch (e: Exception) {
            ddgError = e.message ?: "DuckDuckGo scraper error"
        }

        if (ddgItems.isNotEmpty()) {
            val sb = StringBuilder()
            if (isMaxMode) {
                sb.appendLine("> *[Mode: Multi-Source DuckDuckGo Research]*\n")
            }
            ddgItems.forEachIndexed { index, item ->
                sb.appendLine("### [${index + 1}] ${item.title}")
                sb.appendLine("URL: ${item.url}")
                sb.appendLine("${item.snippet}\n")
            }

            val extractLimit = if (isMaxMode) 2 else 1
            ddgItems.take(extractLimit).forEachIndexed { idx, item ->
                val cleanContent = fetchCleanPageContent(item.url)
                if (!cleanContent.isNullOrBlank()) {
                    sb.appendLine("---")
                    sb.appendLine("### Ekstraksi Konten Halaman [${idx + 1}] (${item.title}):")
                    sb.appendLine(cleanContent)
                }
            }

            return@withContext ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "web_search",
                stdout = sb.toString().trim(),
                isSuccess = true
            )
        }

        if (tavilyApiKey.isNotBlank()) {
            val fallbackResult = searchTavily(toolCallId, query, isFallback = true, isMaxMode = isMaxMode)
            if (fallbackResult.isSuccess) {
                return@withContext fallbackResult
            }
        }

        val finalMessage = if (ddgError != null) {
            "Gagal melakukan pencarian web: $ddgError"
        } else {
            "Tidak ada hasil yang ditemukan untuk pencarian '$query'."
        }

        ToolExecutionResult(
            toolCallId = toolCallId,
            toolName = "web_search",
            stdout = if (ddgError == null) finalMessage else "",
            stderr = if (ddgError != null) finalMessage else "",
            isSuccess = ddgError == null
        )
    }

    private fun searchDuckDuckGo(query: String, maxResults: Int): List<SearchResultItem> {
        val url = "https://lite.duckduckgo.com/lite/"
        val doc = Jsoup.connect(url)
            .data("q", query)
            .userAgent("Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
            .timeout(12000)
            .post()

        val results = mutableListOf<SearchResultItem>()
        val rows = doc.select("table tbody tr")

        var currentTitle = ""
        var currentLink = ""

        for (tr in rows) {
            val linkElem = tr.select("a.result-link").first()
            if (linkElem != null) {
                currentTitle = linkElem.text().trim()
                val rawLink = linkElem.attr("href")
                currentLink = cleanDuckDuckGoUrl(rawLink)
            }

            val snippetElem = tr.select("td.result-snippet").first()
            if (snippetElem != null && currentTitle.isNotEmpty()) {
                val snippet = snippetElem.text().trim()
                results.add(SearchResultItem(title = currentTitle, url = currentLink, snippet = snippet))
                currentTitle = ""
                currentLink = ""
                if (results.size >= maxResults) break
            }
        }

        return results
    }

    private fun cleanDuckDuckGoUrl(rawUrl: String): String {
        return try {
            val fixedUrl = when {
                rawUrl.startsWith("//") -> "https:$rawUrl"
                rawUrl.startsWith("/") -> "https://lite.duckduckgo.com$rawUrl"
                else -> rawUrl
            }
            val uri = URI(fixedUrl)
            val query = uri.rawQuery ?: ""
            for (param in query.split("&")) {
                val pair = param.split("=")
                if (pair.size == 2 && pair[0] == "uddg") {
                    return URLDecoder.decode(pair[1], "UTF-8")
                }
            }
            fixedUrl
        } catch (_: Exception) {
            rawUrl
        }
    }

    private fun fetchCleanPageContent(targetUrl: String, maxLength: Int = 2500): String? {
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) return null

        try {
            val jinaUrl = "https://r.jina.ai/$targetUrl"
            val request = Request.Builder()
                .url(jinaUrl)
                .header("User-Agent", "GGAgent/1.0 (Android; id.gustyxganteng.agent)")
                .header("X-No-Cache", "true")
                .header("Accept", "text/plain")
                .build()

            val response = jinaClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()?.trim()
                if (!body.isNullOrBlank() && body.length > 80) {
                    return if (body.length > maxLength) body.take(maxLength) + "\n\n... [Sisa konten dipotong agar hemat token] ..." else body
                }
            }
        } catch (_: Exception) {
        }

        try {
            val doc = Jsoup.connect(targetUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(6000)
                .get()

            doc.select("script, style, nav, footer, header, aside, .advertisement, #cookie-banner").remove()
            val text = doc.body()?.text()?.trim()
            if (!text.isNullOrBlank() && text.length > 80) {
                return if (text.length > maxLength) text.take(maxLength) + "\n\n... [Sisa konten dipotong agar hemat token] ..." else text
            }
        } catch (_: Exception) {
        }

        return null
    }

    private fun searchTavily(
        toolCallId: String,
        query: String,
        isFallback: Boolean,
        isMaxMode: Boolean = false
    ): ToolExecutionResult {
        val apiKey = KeyVault.getTavilyApiKey().trim()
        if (apiKey.isBlank()) {
            return ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "web_search",
                stderr = "Tavily API Key belum dikonfigurasi di local.properties.",
                isSuccess = false
            )
        }

        return try {
            val maxResults = if (isMaxMode) 6 else 4
            val payload = buildJsonObject {
                put("api_key", apiKey)
                put("query", query)
                put("search_depth", "basic")
                put("include_answer", true)
                put("max_results", maxResults)
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "HTTP ${response.code}"
                response.close()
                return ToolExecutionResult(
                    toolCallId = toolCallId,
                    toolName = "web_search",
                    stderr = "Tavily API error (${response.code}): $errBody",
                    isSuccess = false
                )
            }

            val bodyStr = response.body?.string() ?: "{}"
            val jsonElement = json.parseToJsonElement(bodyStr).jsonObject
            val answer = jsonElement["answer"]?.jsonPrimitive?.content
            val resultsArray = jsonElement["results"]?.jsonArray ?: emptyList()

            val sb = StringBuilder()
            if (isFallback) {
                sb.appendLine("> *[Pencarian dialihkan ke Tavily Search (1 kredit basic) karena DuckDuckGo tidak mengembalikan data]*\n")
            } else if (isMaxMode) {
                sb.appendLine("> *[Mode: Claude Code Max — Auto-Escalated Tavily Deep Search (1 kredit basic, $maxResults hasil)]*\n")
            } else {
                sb.appendLine("> *[Mode: Tavily Deep Search (1 kredit basic)]*\n")
            }

            if (!answer.isNullOrBlank()) {
                sb.appendLine("### Ringkasan Jawaban:\n$answer\n")
            }

            if (resultsArray.isNotEmpty()) {
                sb.appendLine("### Sumber & Referensi:")
                resultsArray.forEachIndexed { idx, resElem ->
                    val obj = resElem.jsonObject
                    val title = obj["title"]?.jsonPrimitive?.content ?: "Tanpa Judul"
                    val link = obj["url"]?.jsonPrimitive?.content ?: ""
                    val content = obj["content"]?.jsonPrimitive?.content ?: ""
                    sb.appendLine("#### [${idx + 1}] $title\nURL: $link\n$content\n")
                }
            } else if (answer.isNullOrBlank()) {
                sb.appendLine("Tidak ada hasil yang ditemukan untuk pencarian '$query'.")
            }

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "web_search",
                stdout = sb.toString().trim(),
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "web_search",
                stderr = "Gagal mengeksekusi Tavily Search: ${e.message}",
                isSuccess = false
            )
        }
    }

    suspend fun wikipediaSearch(
        toolCallId: String,
        args: JsonObject
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val query = args["query"]?.jsonPrimitive?.content?.trim()
            ?: return@withContext ToolExecutionResult(toolCallId, "wikipedia_search", stderr = "Query pencarian Wikipedia kosong.", isSuccess = false)
        val reqLang = args["lang"]?.jsonPrimitive?.content?.trim()?.lowercase()?.ifEmpty { "id" } ?: "id"

        val initialResult = fetchWikipediaSummary(query, reqLang)
        if (initialResult != null) {
            return@withContext ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "wikipedia_search",
                stdout = initialResult,
                isSuccess = true
            )
        }

        if (reqLang != "en") {
            val enResult = fetchWikipediaSummary(query, "en")
            if (enResult != null) {
                val fallbackNote = "> *[Artikel tidak ditemukan di Wikipedia (${reqLang.uppercase()}), dialihkan otomatis ke Wikipedia Bahasa Inggris (EN)]*\n\n"
                return@withContext ToolExecutionResult(
                    toolCallId = toolCallId,
                    toolName = "wikipedia_search",
                    stdout = fallbackNote + enResult,
                    isSuccess = true
                )
            }
        }

        ToolExecutionResult(
            toolCallId = toolCallId,
            toolName = "wikipedia_search",
            stdout = "Tidak ditemukan artikel Wikipedia untuk '$query' baik di Wikipedia (${reqLang.uppercase()}) maupun Wikipedia (EN). Silakan gunakan tool 'web_search' untuk pencarian web yang lebih luas.",
            isSuccess = true
        )
    }

    private fun fetchWikipediaSummary(query: String, lang: String): String? {
        val directSummary = fetchSummaryByTitle(query, lang)
        if (directSummary != null) return directSummary

        val searchTitle = searchMediaWikiTitle(query, lang)
        if (searchTitle != null && !searchTitle.equals(query, ignoreCase = true)) {
            val searchedSummary = fetchSummaryByTitle(searchTitle, lang)
            if (searchedSummary != null) return searchedSummary
        }

        return null
    }

    private fun fetchSummaryByTitle(title: String, lang: String): String? {
        return try {
            val encodedTitle = URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
            val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GGAgent/1.0 (Android; id.gustyxganteng.agent)")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }

            val bodyStr = response.body?.string() ?: return null
            val obj = json.parseToJsonElement(bodyStr).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content
            if (type == "disambiguation") return null

            val pageTitle = obj["title"]?.jsonPrimitive?.content ?: title
            val description = obj["description"]?.jsonPrimitive?.content
            val extract = obj["extract"]?.jsonPrimitive?.content
            val pageUrl = obj["content_urls"]?.jsonObject?.get("desktop")?.jsonObject?.get("page")?.jsonPrimitive?.content
                ?: "https://$lang.wikipedia.org/wiki/$encodedTitle"

            if (extract.isNullOrBlank()) return null

            val sb = StringBuilder()
            sb.appendLine("# Wikipedia ($lang): $pageTitle")
            sb.appendLine("URL: $pageUrl")
            if (!description.isNullOrBlank()) {
                sb.appendLine("Deskripsi: $description")
            }
            sb.appendLine()
            sb.appendLine(extract)

            sb.toString().trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun searchMediaWikiTitle(query: String, lang: String): String? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://$lang.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&utf8=&format=json&srlimit=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GGAgent/1.0 (Android; id.gustyxganteng.agent)")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }

            val bodyStr = response.body?.string() ?: return null
            val root = json.parseToJsonElement(bodyStr).jsonObject
            val searchArr = root["query"]?.jsonObject?.get("search")?.jsonArray ?: return null
            if (searchArr.isEmpty()) return null

            searchArr.first().jsonObject["title"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    }

    suspend fun readWebpage(
        toolCallId: String,
        args: JsonObject
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val url = args["url"]?.jsonPrimitive?.content?.trim()
            ?: return@withContext ToolExecutionResult(toolCallId, "read_webpage", stderr = "URL kosong.", isSuccess = false)

        val cleanFromJina = fetchCleanPageContent(url, maxLength = 10000)
        if (!cleanFromJina.isNullOrBlank() && cleanFromJina.length > 150) {
            return@withContext ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "read_webpage",
                stdout = cleanFromJina,
                isSuccess = true
            )
        }

        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(20000)
                .get()

            doc.select("script, style, nav, footer, header, aside, .advertisement, #cookie-banner").remove()

            val title = doc.title()
            val body = doc.body()

            val sb = StringBuilder()
            if (title.isNotEmpty()) {
                sb.appendLine("# $title\n")
            }

            body.select("h1, h2, h3, p, li").forEach { elem ->
                when (elem.tagName()) {
                    "h1" -> sb.appendLine("# ${elem.text()}\n")
                    "h2" -> sb.appendLine("## ${elem.text()}\n")
                    "h3" -> sb.appendLine("### ${elem.text()}\n")
                    "li" -> sb.appendLine("- ${elem.text()}")
                    "p" -> {
                        val text = elem.text().trim()
                        if (text.isNotEmpty()) sb.appendLine("$text\n")
                    }
                }
            }

            val textOutput = sb.toString().trim()
            val cappedOutput = if (textOutput.length > 8000) {
                textOutput.take(8000) + "\n\n... [Sisa teks dipotong agar hemat token] ..."
            } else {
                textOutput
            }

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "read_webpage",
                stdout = cappedOutput.ifEmpty { "Konten halaman web kosong." },
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "read_webpage",
                stderr = e.message ?: "Gagal membaca halaman web.",
                isSuccess = false
            )
        }
    }
}

data class SearchResultItem(
    val title: String,
    val url: String,
    val snippet: String
)
