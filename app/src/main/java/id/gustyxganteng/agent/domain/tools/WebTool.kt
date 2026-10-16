package id.gustyxganteng.agent.domain.tools

import id.gustyxganteng.agent.core.model.FunctionSpec
import id.gustyxganteng.agent.core.model.PropertySchema
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.model.ToolExecutionResult
import id.gustyxganteng.agent.core.model.ToolParametersSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import java.net.URLEncoder

class WebTool {

    companion object {
        val DEFINITIONS = listOf(
            ToolDefinition(
                function = FunctionSpec(
                    name = "web_search",
                    description = "Mencari informasi terkini dari internet melalui DuckDuckGo Lite dan mengembalikan daftar judul, URL, serta ringkasan snippet.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "query" to PropertySchema("string", "Kata kunci pencarian."),
                            "max_results" to PropertySchema("integer", "Jumlah hasil maksimal yang ingin diambil (default 5).")
                        ),
                        required = listOf("query")
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "read_webpage",
                    description = "Mengambil isi halaman web dan mengekstrak teks utama menjadi format Markdown yang bersih dan hemat token.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "url" to PropertySchema("string", "URL halaman web yang ingin dibaca.")
                        ),
                        required = listOf("url")
                    )
                )
            )
        )
    }

    suspend fun search(
        toolCallId: String,
        args: JsonObject
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val query = args["query"]?.jsonPrimitive?.content
            ?: return@withContext ToolExecutionResult(toolCallId, "web_search", stderr = "Query pencarian kosong.", isSuccess = false)
        val maxResults = args["max_results"]?.jsonPrimitive?.intOrNull ?: 5

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://lite.duckduckgo.com/lite/"
            val doc = Jsoup.connect(url)
                .data("q", query)
                .userAgent("Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                .timeout(15000)
                .post()

            val results = mutableListOf<String>()
            val rows = doc.select("table tbody tr")
            var count = 0

            var currentTitle = ""
            var currentLink = ""

            for (tr in rows) {
                val linkElem = tr.select("a.result-link").first()
                if (linkElem != null) {
                    currentTitle = linkElem.text()
                    currentLink = linkElem.attr("href")
                }

                val snippetElem = tr.select("td.result-snippet").first()
                if (snippetElem != null && currentTitle.isNotEmpty()) {
                    val snippet = snippetElem.text()
                    results.add("### [${count + 1}] $currentTitle\nURL: $currentLink\n$snippet\n")
                    count++
                    currentTitle = ""
                    currentLink = ""
                    if (count >= maxResults) break
                }
            }

            val output = if (results.isNotEmpty()) {
                results.joinToString("\n---\n")
            } else {
                "Tidak ada hasil yang ditemukan untuk pencarian '$query'."
            }

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "web_search",
                stdout = output,
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "web_search",
                stderr = e.message ?: "Gagal melakukan pencarian web.",
                isSuccess = false
            )
        }
    }

    suspend fun readWebpage(
        toolCallId: String,
        args: JsonObject
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val url = args["url"]?.jsonPrimitive?.content
            ?: return@withContext ToolExecutionResult(toolCallId, "read_webpage", stderr = "URL kosong.", isSuccess = false)

        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(20000)
                .get()

            // Buang tag yang tidak relevan
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
