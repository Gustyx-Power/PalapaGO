package id.gustyxganteng.agent.domain.tools

import id.gustyxganteng.agent.core.model.FunctionSpec
import id.gustyxganteng.agent.core.model.PropertySchema
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.model.ToolExecutionResult
import id.gustyxganteng.agent.core.model.ToolParametersSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class AcademicTool(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    companion object {
        val DEFINITIONS = listOf(
            ToolDefinition(
                function = FunctionSpec(
                    name = "search_academic_papers",
                    description = "Mencari jurnal ilmiah kredibel, paper penelitian, skripsi, dan konferensi internasional/nasional dari OpenAlex dan Semantic Scholar. Mengembalikan judul asli, penulis, tahun, nama jurnal/venue, jumlah sitasi, DOI valid, link PDF open-access, abstrak lengkap, serta format sitasi resmi APA 7th, IEEE, dan BibTeX.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "query" to PropertySchema("string", "Topik penelitian, kata kunci jurnal, atau judul paper yang ingin dicari."),
                            "year_from" to PropertySchema("integer", "Tahun minimal publikasi untuk menyaring paper terbaru (default 2021)."),
                            "limit" to PropertySchema("integer", "Jumlah maksimal jurnal yang ingin diambil (default 5).")
                        ),
                        required = listOf("query")
                    )
                )
            ),
            ToolDefinition(
                function = FunctionSpec(
                    name = "verify_doi_or_citation",
                    description = "Memvalidasi nomor DOI resmi ke Crossref dan menghasilkan metadata resmi terverifikasi beserta format sitasi APA 7th Edition, IEEE, dan BibTeX untuk Mendeley/Zotero.",
                    parameters = ToolParametersSchema(
                        properties = mapOf(
                            "doi" to PropertySchema("string", "Nomor DOI atau link DOI yang ingin diverifikasi (contoh: 10.3390/s23218875 atau https://doi.org/10.1109/...).")
                        ),
                        required = listOf("doi")
                    )
                )
            )
        )
    }

    suspend fun searchAcademicPapers(
        toolCallId: String,
        args: JsonObject
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val query = args["query"]?.jsonPrimitive?.content?.trim()
            ?: return@withContext ToolExecutionResult(toolCallId, "search_academic_papers", stderr = "Query pencarian jurnal tidak boleh kosong.", isSuccess = false)
        val yearFrom = args["year_from"]?.jsonPrimitive?.intOrNull ?: 2021
        val limit = (args["limit"]?.jsonPrimitive?.intOrNull ?: 5).coerceIn(1, 10)

        val papers = mutableListOf<AcademicPaper>()

        try {
            val openAlexPapers = searchOpenAlex(query, yearFrom, limit)
            papers.addAll(openAlexPapers)
        } catch (_: Exception) {
        }

        if (papers.isEmpty()) {
            try {
                val semanticPapers = searchSemanticScholar(query, yearFrom, limit)
                papers.addAll(semanticPapers)
            } catch (_: Exception) {
            }
        }

        if (papers.isEmpty()) {
            return@withContext ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "search_academic_papers",
                stdout = "Tidak ditemukan jurnal atau paper ilmiah untuk topik '$query' dari tahun $yearFrom ke atas. Cobalah menggunakan istilah teknis bahasa Inggris atau kurangi filter tahun.",
                isSuccess = true
            )
        }

        val sb = StringBuilder()
        sb.appendLine("## Hasil Pencarian Literatur Ilmiah Terindeks\n")
        sb.appendLine("Ditemukan ${papers.size} jurnal kredibel untuk topik: **\"$query\"** (Tahun >= $yearFrom)\n")

        papers.forEachIndexed { index, paper ->
            val num = index + 1
            sb.appendLine("---")
            sb.appendLine("### [$num] ${paper.title}")

            val authorsStr = if (paper.authors.isNotEmpty()) {
                paper.authors.joinToString(", ") { author ->
                    if (!author.institution.isNullOrBlank()) "${author.name} (${author.institution})" else author.name
                }
            } else "Penulis Tidak Dicantumkan"

            sb.appendLine("- **Penulis:** $authorsStr")
            sb.appendLine("- **Jurnal / Venue:** ${paper.journalOrVenue.ifBlank { "Jurnal Terindeks" }} (${if (paper.year > 0) paper.year else "Tahun tidak diketahui"})")
            sb.appendLine("- **Sitasi Terindeks:** ${paper.citationCount} sitasi")

            if (!paper.doi.isNullOrBlank()) {
                val cleanDoi = normalizeDoiUrl(paper.doi)
                sb.appendLine("- **DOI:** [$cleanDoi]($cleanDoi)")
            } else {
                sb.appendLine("- **DOI:** Tidak tersedia")
            }

            if (!paper.openAccessPdfUrl.isNullOrBlank()) {
                sb.appendLine("- **Akses PDF:** [Unduh PDF Langsung (Open Access)](${paper.openAccessPdfUrl})")
            } else {
                sb.appendLine("- **Akses PDF:** Akses Berlangganan / Non-OA")
            }

            sb.appendLine()
            sb.appendLine("**Abstrak:**")
            sb.appendLine(paper.abstractText?.ifBlank { "Abstrak tidak tersedia di basis data publik." } ?: "Abstrak tidak tersedia di basis data publik.")
            sb.appendLine()

            val apa = formatApaCitation(paper)
            val ieee = formatIeeeCitation(paper, num)
            val bibtex = paper.doi?.let { fetchBibtexFromDoi(it) } ?: generateLocalBibtex(paper)

            sb.appendLine("**Format Sitasi Resmi:**")
            sb.appendLine("- **APA 7th Edition:**")
            sb.appendLine("> $apa")
            sb.appendLine()
            sb.appendLine("- **IEEE:**")
            sb.appendLine("> $ieee")
            sb.appendLine()
            sb.appendLine("- **BibTeX (Mendeley / Zotero Ready):**")
            sb.appendLine("```bibtex")
            sb.appendLine(bibtex)
            sb.appendLine("```")
            sb.appendLine()
        }

        sb.appendLine("---")
        sb.appendLine("### Matriks Penelitian Terdahulu (Ringkasan)")
        sb.appendLine("| No | Penulis (Tahun) | Judul & Jurnal | Sitasi | Akses PDF | DOI |")
        sb.appendLine("|---|---|---|---|---|---|")
        papers.forEachIndexed { idx, paper ->
            val firstAuthor = paper.authors.firstOrNull()?.name?.split(Regex("\\s+"))?.lastOrNull() ?: "Anonim"
            val authorShort = if (paper.authors.size > 1) "$firstAuthor et al." else firstAuthor
            val yearStr = if (paper.year > 0) "(${paper.year})" else "(n.d.)"
            val titleShort = if (paper.title.length > 50) paper.title.take(47) + "..." else paper.title
            val journalShort = if (paper.journalOrVenue.length > 30) paper.journalOrVenue.take(27) + "..." else paper.journalOrVenue
            val pdfLink = if (!paper.openAccessPdfUrl.isNullOrBlank()) "[PDF](${paper.openAccessPdfUrl})" else "Closed"
            val doiLink = if (!paper.doi.isNullOrBlank()) "[DOI](${normalizeDoiUrl(paper.doi)})" else "-"

            sb.appendLine("| ${idx + 1} | $authorShort $yearStr | $titleShort (*$journalShort*) | ${paper.citationCount} | $pdfLink | $doiLink |")
        }

        ToolExecutionResult(
            toolCallId = toolCallId,
            toolName = "search_academic_papers",
            stdout = sb.toString().trim(),
            isSuccess = true
        )
    }

    suspend fun verifyDoiOrCitation(
        toolCallId: String,
        args: JsonObject
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val rawDoi = args["doi"]?.jsonPrimitive?.content?.trim()
            ?: return@withContext ToolExecutionResult(toolCallId, "verify_doi_or_citation", stderr = "Nomor DOI tidak boleh kosong.", isSuccess = false)

        val cleanDoi = rawDoi
            .removePrefix("https://doi.org/")
            .removePrefix("http://dx.doi.org/")
            .removePrefix("doi:")
            .trim()

        val crossrefPaper = verifyCrossrefDoi(cleanDoi)
        if (crossrefPaper != null) {
            val apa = formatApaCitation(crossrefPaper)
            val ieee = formatIeeeCitation(crossrefPaper, 1)
            val bibtex = fetchBibtexFromDoi(cleanDoi) ?: generateLocalBibtex(crossrefPaper)

            val authorsStr = if (crossrefPaper.authors.isNotEmpty()) {
                crossrefPaper.authors.joinToString(", ") { it.name }
            } else "Penulis Tidak Dicantumkan"

            val sb = StringBuilder()
            sb.appendLine("## Hasil Verifikasi DOI: TERVERIFIKASI (Crossref Validated)\n")
            sb.appendLine("- **Status:** Valid & Terdaftar di Crossref DOI Registry")
            sb.appendLine("- **Judul Artikel:** ${crossrefPaper.title}")
            sb.appendLine("- **Penulis:** $authorsStr")
            sb.appendLine("- **Penerbit / Jurnal:** ${crossrefPaper.journalOrVenue}")
            sb.appendLine("- **Tahun Publikasi:** ${if (crossrefPaper.year > 0) crossrefPaper.year else "-"}")
            if (!crossrefPaper.volume.isNullOrBlank() || !crossrefPaper.issue.isNullOrBlank() || !crossrefPaper.pages.isNullOrBlank()) {
                sb.appendLine("- **Bibliografi:** Vol. ${crossrefPaper.volume ?: "-"}, No. ${crossrefPaper.issue ?: "-"}, Halaman: ${crossrefPaper.pages ?: "-"}")
            }
            sb.appendLine("- **Sitasi Terindeks:** ${crossrefPaper.citationCount} sitasi")
            sb.appendLine("- **Tautan DOI Resmi:** [https://doi.org/$cleanDoi](https://doi.org/$cleanDoi)")
            if (!crossrefPaper.openAccessPdfUrl.isNullOrBlank()) {
                sb.appendLine("- **Tautan PDF / Resource:** [Buka Resource Dokumen](${crossrefPaper.openAccessPdfUrl})")
            }
            sb.appendLine()

            sb.appendLine("### Format Sitasi APA 7th Edition:")
            sb.appendLine("```apa")
            sb.appendLine(apa)
            sb.appendLine("```")
            sb.appendLine()

            sb.appendLine("### Format Sitasi IEEE:")
            sb.appendLine("```ieee")
            sb.appendLine(ieee)
            sb.appendLine("```")
            sb.appendLine()

            sb.appendLine("### BibTeX (Mendeley / Zotero / Overleaf Ready):")
            sb.appendLine("```bibtex")
            sb.appendLine(bibtex)
            sb.appendLine("```")

            return@withContext ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "verify_doi_or_citation",
                stdout = sb.toString().trim(),
                isSuccess = true
            )
        }

        val openAlexPaper = searchOpenAlexByDoi(cleanDoi)
        if (openAlexPaper != null) {
            val apa = formatApaCitation(openAlexPaper)
            val ieee = formatIeeeCitation(openAlexPaper, 1)
            val bibtex = fetchBibtexFromDoi(cleanDoi) ?: generateLocalBibtex(openAlexPaper)

            val sb = StringBuilder()
            sb.appendLine("## Hasil Verifikasi DOI: TERVERIFIKASI (OpenAlex Validated)\n")
            sb.appendLine("- **Status:** Valid & Terindeks di OpenAlex")
            sb.appendLine("- **Judul:** ${openAlexPaper.title}")
            sb.appendLine("- **Penulis:** ${openAlexPaper.authors.joinToString(", ") { it.name }}")
            sb.appendLine("- **Jurnal / Venue:** ${openAlexPaper.journalOrVenue} (${openAlexPaper.year})")
            sb.appendLine("- **Sitasi:** ${openAlexPaper.citationCount} sitasi")
            sb.appendLine("- **Tautan DOI:** [https://doi.org/$cleanDoi](https://doi.org/$cleanDoi)")
            sb.appendLine()
            sb.appendLine("### Sitasi APA 7th Edition:\n```apa\n$apa\n```\n")
            sb.appendLine("### Sitasi IEEE:\n```ieee\n$ieee\n```\n")
            sb.appendLine("### BibTeX:\n```bibtex\n$bibtex\n```")

            return@withContext ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "verify_doi_or_citation",
                stdout = sb.toString().trim(),
                isSuccess = true
            )
        }

        ToolExecutionResult(
            toolCallId = toolCallId,
            toolName = "verify_doi_or_citation",
            stderr = "DOI '$cleanDoi' tidak ditemukan di Crossref maupun OpenAlex. Pastikan format DOI valid (contoh: 10.3390/s23218875).",
            isSuccess = false
        )
    }

    private fun searchOpenAlex(query: String, yearFrom: Int, limit: Int): List<AcademicPaper> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val filterYear = (yearFrom - 1).coerceAtLeast(1990)
        val url = "https://api.openalex.org/works?search=$encodedQuery&filter=publication_year:>$filterYear&per-page=$limit&mailto=ggagent@gustyxganteng.id"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GGAgent/1.0 (mailto:ggagent@gustyxganteng.id)")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            return emptyList()
        }

        val bodyStr = response.body?.string() ?: return emptyList()
        val root = json.parseToJsonElement(bodyStr).jsonObject
        val resultsArr = root["results"]?.jsonArray ?: return emptyList()

        val papers = mutableListOf<AcademicPaper>()

        for (item in resultsArr) {
            val obj = item.jsonObject
            val title = obj["title"]?.jsonPrimitive?.content?.trim() ?: continue
            val year = obj["publication_year"]?.jsonPrimitive?.intOrNull ?: 0
            val citedBy = obj["cited_by_count"]?.jsonPrimitive?.intOrNull ?: 0
            val rawDoi = obj["doi"]?.jsonPrimitive?.content

            val authorships = obj["authorships"]?.jsonArray ?: emptyList()
            val authorList = mutableListOf<AcademicAuthor>()
            for (authElem in authorships) {
                val authObj = authElem.jsonObject
                val authorName = authObj["author"]?.jsonObject?.get("display_name")?.jsonPrimitive?.content
                val instName = authObj["institutions"]?.jsonArray?.firstOrNull()?.jsonObject?.get("display_name")?.jsonPrimitive?.content
                if (!authorName.isNullOrBlank()) {
                    authorList.add(AcademicAuthor(authorName.trim(), instName?.trim()))
                }
            }

            val primaryLoc = obj["primary_location"]?.jsonObject
            val sourceName = primaryLoc?.get("source")?.jsonObject?.get("display_name")?.jsonPrimitive?.content ?: ""

            val oaObj = obj["open_access"]?.jsonObject
            val oaUrl = oaObj?.get("oa_url")?.jsonPrimitive?.content

            val biblioObj = obj["biblio"]?.jsonObject
            val volume = biblioObj?.get("volume")?.jsonPrimitive?.content
            val issue = biblioObj?.get("issue")?.jsonPrimitive?.content
            val firstPage = biblioObj?.get("first_page")?.jsonPrimitive?.content
            val lastPage = biblioObj?.get("last_page")?.jsonPrimitive?.content
            val pages = if (!firstPage.isNullOrBlank() && !lastPage.isNullOrBlank()) {
                "$firstPage-$lastPage"
            } else firstPage ?: lastPage

            val invertedIndex = obj["abstract_inverted_index"]?.jsonObject
            val abstractText = reconstructAbstract(invertedIndex)

            papers.add(
                AcademicPaper(
                    title = title,
                    authors = authorList,
                    year = year,
                    journalOrVenue = sourceName,
                    volume = volume,
                    issue = issue,
                    pages = pages,
                    doi = rawDoi,
                    openAccessPdfUrl = oaUrl,
                    citationCount = citedBy,
                    abstractText = abstractText
                )
            )
        }

        return papers
    }

    private fun searchSemanticScholar(query: String, yearFrom: Int, limit: Int): List<AcademicPaper> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.semanticscholar.org/graph/v1/paper/search?query=$encodedQuery&year=${yearFrom}-&limit=$limit&fields=title,authors,year,abstract,citationCount,venue,externalIds,openAccessPdf"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GGAgent/1.0 (mailto:ggagent@gustyxganteng.id)")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            return emptyList()
        }

        val bodyStr = response.body?.string() ?: return emptyList()
        val root = json.parseToJsonElement(bodyStr).jsonObject
        val dataArr = root["data"]?.jsonArray ?: return emptyList()

        val papers = mutableListOf<AcademicPaper>()

        for (item in dataArr) {
            val obj = item.jsonObject
            val title = obj["title"]?.jsonPrimitive?.content?.trim() ?: continue
            val year = obj["year"]?.jsonPrimitive?.intOrNull ?: 0
            val citedBy = obj["citationCount"]?.jsonPrimitive?.intOrNull ?: 0
            val venue = obj["venue"]?.jsonPrimitive?.content ?: ""
            val abstractText = obj["abstract"]?.jsonPrimitive?.content

            val authorsArr = obj["authors"]?.jsonArray ?: emptyList()
            val authorList = mutableListOf<AcademicAuthor>()
            for (authElem in authorsArr) {
                val name = authElem.jsonObject["name"]?.jsonPrimitive?.content
                if (!name.isNullOrBlank()) {
                    authorList.add(AcademicAuthor(name.trim()))
                }
            }

            val externalIds = obj["externalIds"]?.jsonObject
            val doi = externalIds?.get("DOI")?.jsonPrimitive?.content

            val openAccessPdf = obj["openAccessPdf"]?.jsonObject
            val oaUrl = openAccessPdf?.get("url")?.jsonPrimitive?.content

            papers.add(
                AcademicPaper(
                    title = title,
                    authors = authorList,
                    year = year,
                    journalOrVenue = venue,
                    doi = doi,
                    openAccessPdfUrl = oaUrl,
                    citationCount = citedBy,
                    abstractText = abstractText
                )
            )
        }

        return papers
    }

    private fun verifyCrossrefDoi(cleanDoi: String): AcademicPaper? {
        return try {
            val url = "https://api.crossref.org/works/$cleanDoi"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GGAgent/1.0 (mailto:ggagent@gustyxganteng.id)")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }

            val bodyStr = response.body?.string() ?: return null
            val root = json.parseToJsonElement(bodyStr).jsonObject
            val msg = root["message"]?.jsonObject ?: return null

            val title = msg["title"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content ?: "Tanpa Judul"
            val containerTitle = msg["container-title"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
                ?: msg["publisher"]?.jsonPrimitive?.content ?: ""

            val authorsArr = msg["author"]?.jsonArray ?: emptyList()
            val authorList = mutableListOf<AcademicAuthor>()
            for (a in authorsArr) {
                val aObj = a.jsonObject
                val given = aObj["given"]?.jsonPrimitive?.content ?: ""
                val family = aObj["family"]?.jsonPrimitive?.content ?: ""
                val fullName = if (given.isNotEmpty() && family.isNotEmpty()) "$given $family" else family.ifEmpty { given }
                if (fullName.isNotBlank()) {
                    authorList.add(AcademicAuthor(fullName.trim()))
                }
            }

            val year = extractCrossrefYear(msg)
            val volume = msg["volume"]?.jsonPrimitive?.content
            val issue = msg["issue"]?.jsonPrimitive?.content
            val page = msg["page"]?.jsonPrimitive?.content
            val citations = msg["is-referenced-by-count"]?.jsonPrimitive?.intOrNull ?: 0
            val resourceUrl = msg["resource"]?.jsonObject?.get("primary")?.jsonObject?.get("URL")?.jsonPrimitive?.content

            AcademicPaper(
                title = title,
                authors = authorList,
                year = year,
                journalOrVenue = containerTitle,
                volume = volume,
                issue = issue,
                pages = page,
                doi = "https://doi.org/$cleanDoi",
                openAccessPdfUrl = resourceUrl,
                citationCount = citations
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun searchOpenAlexByDoi(cleanDoi: String): AcademicPaper? {
        return try {
            val url = "https://api.openalex.org/works/https://doi.org/$cleanDoi?mailto=ggagent@gustyxganteng.id"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GGAgent/1.0 (mailto:ggagent@gustyxganteng.id)")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }

            val bodyStr = response.body?.string() ?: return null
            val obj = json.parseToJsonElement(bodyStr).jsonObject
            val title = obj["title"]?.jsonPrimitive?.content?.trim() ?: return null
            val year = obj["publication_year"]?.jsonPrimitive?.intOrNull ?: 0
            val citedBy = obj["cited_by_count"]?.jsonPrimitive?.intOrNull ?: 0

            val authorships = obj["authorships"]?.jsonArray ?: emptyList()
            val authorList = mutableListOf<AcademicAuthor>()
            for (authElem in authorships) {
                val authObj = authElem.jsonObject
                val authorName = authObj["author"]?.jsonObject?.get("display_name")?.jsonPrimitive?.content
                if (!authorName.isNullOrBlank()) {
                    authorList.add(AcademicAuthor(authorName.trim()))
                }
            }

            val primaryLoc = obj["primary_location"]?.jsonObject
            val sourceName = primaryLoc?.get("source")?.jsonObject?.get("display_name")?.jsonPrimitive?.content ?: ""
            val oaUrl = obj["open_access"]?.jsonObject?.get("oa_url")?.jsonPrimitive?.content

            AcademicPaper(
                title = title,
                authors = authorList,
                year = year,
                journalOrVenue = sourceName,
                doi = "https://doi.org/$cleanDoi",
                openAccessPdfUrl = oaUrl,
                citationCount = citedBy
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractCrossrefYear(msg: JsonObject): Int {
        val printParts = msg["published-print"]?.jsonObject?.get("date-parts")?.jsonArray?.firstOrNull()?.jsonArray
        val onlineParts = msg["published-online"]?.jsonObject?.get("date-parts")?.jsonArray?.firstOrNull()?.jsonArray
        val createdParts = msg["created"]?.jsonObject?.get("date-parts")?.jsonArray?.firstOrNull()?.jsonArray

        return printParts?.firstOrNull()?.jsonPrimitive?.intOrNull
            ?: onlineParts?.firstOrNull()?.jsonPrimitive?.intOrNull
            ?: createdParts?.firstOrNull()?.jsonPrimitive?.intOrNull
            ?: 0
    }

    private fun reconstructAbstract(invertedIndex: JsonObject?): String? {
        if (invertedIndex == null || invertedIndex.isEmpty()) return null
        return try {
            val wordMap = mutableMapOf<Int, String>()
            for ((word, posElem) in invertedIndex) {
                val posArr = posElem.jsonArray
                for (p in posArr) {
                    p.jsonPrimitive.intOrNull?.let { index ->
                        wordMap[index] = word
                    }
                }
            }
            if (wordMap.isEmpty()) return null
            val maxIndex = wordMap.keys.maxOrNull() ?: return null
            val words = ArrayList<String>(maxIndex + 1)
            for (i in 0..maxIndex) {
                words.add(wordMap[i] ?: "")
            }
            words.joinToString(" ").replace(Regex("\\s+"), " ").trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchBibtexFromDoi(doi: String): String? {
        return try {
            val cleanDoi = doi.removePrefix("https://doi.org/").removePrefix("http://dx.doi.org/").trim()
            if (cleanDoi.isBlank()) return null
            val url = "https://doi.org/$cleanDoi"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/x-bibtex")
                .header("User-Agent", "GGAgent/1.0 (mailto:ggagent@gustyxganteng.id)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()?.trim()
                if (!body.isNullOrBlank() && body.startsWith("@")) {
                    return body
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun generateLocalBibtex(paper: AcademicPaper): String {
        val firstAuthorLastName = paper.authors.firstOrNull()?.name?.trim()?.split(Regex("\\s+"))?.lastOrNull()
            ?.filter { it.isLetter() } ?: "Paper"
        val citeKey = "${firstAuthorLastName}${if (paper.year > 0) paper.year else ""}"
        val authorBib = if (paper.authors.isNotEmpty()) {
            paper.authors.joinToString(" and ") { author ->
                val parts = author.name.trim().split(Regex("\\s+"))
                if (parts.size > 1) {
                    "${parts.last()}, ${parts.dropLast(1).joinToString(" ")}"
                } else {
                    parts[0]
                }
            }
        } else "Unknown"

        return buildString {
            appendLine("@article{$citeKey,")
            appendLine("  title = {${paper.title}},")
            appendLine("  author = {$authorBib},")
            if (paper.journalOrVenue.isNotBlank()) {
                appendLine("  journal = {${paper.journalOrVenue}},")
            }
            if (paper.year > 0) {
                appendLine("  year = {${paper.year}},")
            }
            if (!paper.volume.isNullOrBlank()) {
                appendLine("  volume = {${paper.volume}},")
            }
            if (!paper.issue.isNullOrBlank()) {
                appendLine("  number = {${paper.issue}},")
            }
            if (!paper.pages.isNullOrBlank()) {
                appendLine("  pages = {${paper.pages.replace("-", "--")}},")
            }
            if (!paper.doi.isNullOrBlank()) {
                val cleanDoi = paper.doi.removePrefix("https://doi.org/").removePrefix("http://dx.doi.org/")
                appendLine("  doi = {$cleanDoi},")
            }
            append("}")
        }
    }

    private fun formatApaCitation(paper: AcademicPaper): String {
        val authorsStr = formatApaAuthors(paper.authors)
        val yearStr = if (paper.year > 0) "(${paper.year})" else "(n.d.)"
        val cleanTitle = paper.title.trim().trimEnd('.') + "."
        val journalStr = if (paper.journalOrVenue.isNotBlank()) "*${paper.journalOrVenue}*" else ""

        val volIssueParts = buildString {
            if (!paper.volume.isNullOrBlank()) {
                append(", ${paper.volume}")
                if (!paper.issue.isNullOrBlank()) {
                    append("(${paper.issue})")
                }
            } else if (!paper.issue.isNullOrBlank()) {
                append(", (${paper.issue})")
            }
            if (!paper.pages.isNullOrBlank()) {
                append(", ${paper.pages}")
            }
        }

        val doiStr = if (!paper.doi.isNullOrBlank()) {
            val link = normalizeDoiUrl(paper.doi)
            " $link"
        } else ""

        return "$authorsStr $yearStr. $cleanTitle $journalStr$volIssueParts.$doiStr".trim().replace(Regex("\\s+"), " ")
    }

    private fun formatIeeeCitation(paper: AcademicPaper, index: Int): String {
        val authorsStr = formatIeeeAuthors(paper.authors, index)
        val cleanTitle = paper.title.trim().trimEnd('.')
        val journalStr = if (paper.journalOrVenue.isNotBlank()) "*${paper.journalOrVenue}*" else "journal article"

        val volIssueParts = buildString {
            if (!paper.volume.isNullOrBlank()) {
                append(", vol. ${paper.volume}")
            }
            if (!paper.issue.isNullOrBlank()) {
                append(", no. ${paper.issue}")
            }
            if (!paper.pages.isNullOrBlank()) {
                append(", pp. ${paper.pages}")
            }
        }

        val yearStr = if (paper.year > 0) ", ${paper.year}" else ""
        val doiStr = if (!paper.doi.isNullOrBlank()) {
            val cleanDoi = paper.doi.removePrefix("https://doi.org/").removePrefix("http://dx.doi.org/")
            ", doi: $cleanDoi."
        } else "."

        return "$authorsStr, \"$cleanTitle,\" $journalStr$volIssueParts$yearStr$doiStr"
    }

    private fun formatApaAuthors(authors: List<AcademicAuthor>): String {
        if (authors.isEmpty()) return "Anonim"
        val formatted = authors.map { author ->
            val parts = author.name.trim().split(Regex("\\s+"))
            if (parts.size == 1) {
                parts[0]
            } else {
                val lastName = parts.last()
                val initials = parts.dropLast(1).joinToString(" ") { p ->
                    val firstChar = p.firstOrNull()?.uppercaseChar()
                    if (firstChar != null) "$firstChar." else ""
                }.trim()
                if (initials.isNotEmpty()) "$lastName, $initials" else lastName
            }
        }
        return when (formatted.size) {
            1 -> formatted[0]
            2 -> "${formatted[0]}, & ${formatted[1]}"
            else -> {
                val allExceptLast = formatted.dropLast(1).joinToString(", ")
                "$allExceptLast, & ${formatted.last()}"
            }
        }
    }

    private fun formatIeeeAuthors(authors: List<AcademicAuthor>, index: Int): String {
        if (authors.isEmpty()) return "[$index] Anonim"
        val formatted = authors.map { author ->
            val parts = author.name.trim().split(Regex("\\s+"))
            if (parts.size == 1) {
                parts[0]
            } else {
                val lastName = parts.last()
                val initials = parts.dropLast(1).joinToString(" ") { p ->
                    val firstChar = p.firstOrNull()?.uppercaseChar()
                    if (firstChar != null) "$firstChar." else ""
                }.trim()
                if (initials.isNotEmpty()) "$initials $lastName" else lastName
            }
        }
        val authorsStr = when (formatted.size) {
            1 -> formatted[0]
            2 -> "${formatted[0]} and ${formatted[1]}"
            else -> {
                val allExceptLast = formatted.dropLast(1).joinToString(", ")
                "$allExceptLast, and ${formatted.last()}"
            }
        }
        return "[$index] $authorsStr"
    }

    private fun normalizeDoiUrl(doi: String): String {
        val clean = doi.removePrefix("https://doi.org/").removePrefix("http://dx.doi.org/").removePrefix("doi:").trim()
        return "https://doi.org/$clean"
    }
}

data class AcademicAuthor(
    val name: String,
    val institution: String? = null
)

data class AcademicPaper(
    val title: String,
    val authors: List<AcademicAuthor>,
    val year: Int,
    val journalOrVenue: String,
    val volume: String? = null,
    val issue: String? = null,
    val pages: String? = null,
    val doi: String? = null,
    val openAccessPdfUrl: String? = null,
    val citationCount: Int = 0,
    val abstractText: String? = null
)
