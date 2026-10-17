package id.gustyxganteng.agent.core.markdown

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import id.gustyxganteng.agent.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

sealed interface MarkdownNode {
    data class Header(val level: Int, val text: String) : MarkdownNode
    data class Paragraph(val text: String) : MarkdownNode
    data class CodeBlock(val language: String, val code: String) : MarkdownNode
    data class BlockQuote(val text: String) : MarkdownNode
    data class BulletItem(val text: String) : MarkdownNode
    data class NumberedItem(val number: String, val text: String) : MarkdownNode
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownNode
}

object MarkdownParser {
    fun parse(markdown: String): List<MarkdownNode> {
        val lines = markdown.lines()
        val nodes = mutableListOf<MarkdownNode>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // 1. Code blocks ```lang
            if (line.trim().startsWith("```")) {
                val lang = line.trim().removePrefix("```").trim()
                val codeBuilder = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeBuilder.appendLine(lines[i])
                    i++
                }
                nodes.add(MarkdownNode.CodeBlock(lang, codeBuilder.toString().trimEnd()))
                i++
                continue
            }

            // 2. Table (| header | or col1 | col2)
            if (isTableRow(line) && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
                val headers = parseRowCells(line)
                i += 2 // skip header and separator
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && isTableRow(lines[i])) {
                    val rawCells = parseRowCells(lines[i])
                    val cells = if (rawCells.size < headers.size) {
                        rawCells + List(headers.size - rawCells.size) { "" }
                    } else {
                        rawCells.take(headers.size)
                    }
                    rows.add(cells)
                    i++
                }
                if (headers.isNotEmpty()) {
                    nodes.add(MarkdownNode.Table(headers, rows))
                    continue
                }
            }

            // 3. Headings (# H1, ## H2, etc.)
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length
                val text = trimmed.drop(level).trim()
                nodes.add(MarkdownNode.Header(level, text))
                i++
                continue
            }

            // 4. Blockquotes (> text)
            if (trimmed.startsWith(">")) {
                val text = trimmed.removePrefix(">").trim()
                nodes.add(MarkdownNode.BlockQuote(text))
                i++
                continue
            }

            // 5. Bullet item (* or -)
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ")) {
                val text = trimmed.drop(2).trim()
                nodes.add(MarkdownNode.BulletItem(text))
                i++
                continue
            }

            // 6. Numbered item (1. item)
            val numberedMatch = Regex("^(\\d+)\\.\\s+(.*)").find(trimmed)
            if (numberedMatch != null) {
                val num = numberedMatch.groupValues[1]
                val text = numberedMatch.groupValues[2]
                nodes.add(MarkdownNode.NumberedItem(num, text))
                i++
                continue
            }

            // 7. Regular paragraph / empty line
            if (trimmed.isNotEmpty()) {
                nodes.add(MarkdownNode.Paragraph(trimmed))
            }
            i++
        }

        return nodes
    }

    private fun isTableRow(line: String): Boolean {
        val t = line.trim()
        if (t.isEmpty() || t.startsWith("```") || t.startsWith("#")) return false
        return (t.startsWith("|") && t.endsWith("|")) || (t.count { it == '|' } >= 2)
    }

    private fun isTableSeparator(line: String): Boolean {
        val t = line.trim()
        if (!t.contains("-")) return false
        val cells = parseRowCells(t)
        return cells.isNotEmpty() && cells.all { cell ->
            val inner = cell.trim()
            inner.matches(Regex("^:?-+:?$"))
        }
    }

    private fun parseRowCells(line: String): List<String> {
        var t = line.trim()
        if (t.startsWith("|")) t = t.substring(1)
        if (t.endsWith("|")) t = t.substring(0, t.length - 1)
        return t.split("|").map { it.trim() }
    }
}

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    onOpenInCanvas: ((title: String, language: String, content: String) -> Unit)? = null
) {
    val nodes = MarkdownParser.parse(markdown)

    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            nodes.forEach { node ->
                when (node) {
                    is MarkdownNode.Header -> {
                        val (style, topPad, botPad) = when (node.level) {
                            1 -> Triple(
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    lineHeight = 26.sp
                                ),
                                10.dp,
                                4.dp
                            )
                            2 -> Triple(
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    lineHeight = 23.sp
                                ),
                                8.dp,
                                3.dp
                            )
                            3 -> Triple(
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp
                                ),
                                6.dp,
                                2.dp
                            )
                            else -> Triple(
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                4.dp,
                                2.dp
                            )
                        }
                        Text(
                            text = node.text,
                            style = style,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = topPad, bottom = botPad)
                        )
                    }
                    is MarkdownNode.Paragraph -> {
                        val downloadLinks = extractDownloadLinks(node.text)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = formatInlineMarkdown(node.text),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 14.5.sp,
                                    lineHeight = 23.sp,
                                    letterSpacing = 0.15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            downloadLinks.forEach { link ->
                                MarkdownDownloadButton(label = link.label, url = link.url)
                            }
                        }
                    }
                    is MarkdownNode.CodeBlock -> {
                        CodeBlockCard(
                            language = node.language,
                            code = node.code,
                            onOpenInCanvas = onOpenInCanvas
                        )
                    }
                    is MarkdownNode.BlockQuote -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(24.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = formatInlineMarkdown(node.text),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        lineHeight = 22.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                    is MarkdownNode.BulletItem -> {
                        val downloadLinks = extractDownloadLinks(node.text)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = formatInlineMarkdown(node.text),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.5.sp,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            downloadLinks.forEach { link ->
                                Box(modifier = Modifier.padding(start = 24.dp)) {
                                    MarkdownDownloadButton(label = link.label, url = link.url)
                                }
                            }
                        }
                    }
                    is MarkdownNode.NumberedItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${node.number}.",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatInlineMarkdown(node.text),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.5.sp,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    is MarkdownNode.Table -> {
                        TableCard(node.headers, node.rows)
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier,
    onOpenInCanvas: ((title: String, language: String, content: String) -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val displayLang = language.ifEmpty { "text" }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayLang,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(code))
                            Toast.makeText(context, context.getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Kode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (onOpenInCanvas != null) {
                        IconButton(
                            onClick = {
                                onOpenInCanvas("Snippet ($displayLang)", displayLang, code)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Buka di Canvas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TableCard(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    if (headers.isEmpty()) return

    val colCount = headers.size
    val colWidths = remember(headers, rows) {
        (0 until colCount).map { colIdx ->
            val headerLen = headers.getOrNull(colIdx)?.length ?: 0
            val maxCellLen = rows.maxOfOrNull { row -> row.getOrNull(colIdx)?.length ?: 0 } ?: 0
            val maxLen = maxOf(headerLen, maxCellLen)
            when {
                maxLen <= 4 -> 60.dp
                maxLen <= 12 -> 90.dp
                maxLen <= 22 -> 135.dp
                maxLen <= 35 -> 180.dp
                else -> 260.dp
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headers.forEachIndexed { idx, header ->
                    val w = colWidths.getOrElse(idx) { 120.dp }
                    Box(
                        modifier = Modifier
                            .width(w)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            // Body Rows
            rows.forEachIndexed { rowIndex, row ->
                val bg = if (rowIndex % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                Row(
                    modifier = Modifier
                        .background(bg)
                        .padding(vertical = 7.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEachIndexed { colIdx, cell ->
                        val w = colWidths.getOrElse(colIdx) { 120.dp }
                        Box(
                            modifier = Modifier
                                .width(w)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val singleLink = Regex("^\\[([^\\]]+)\\]\\(([^)]+)\\)$").find(cell.trim())
                            if (singleLink != null) {
                                val label = singleLink.groupValues[1]
                                val url = singleLink.groupValues[2]
                                MarkdownActionChip(label = label, url = url)
                            } else {
                                Text(
                                    text = formatInlineMarkdown(cell),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownDownloadButton(
    label: String,
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        onClick = { downloadOrOpenUrl(context, url, suggestedTitle = label) },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF004A77),
        border = BorderStroke(1.dp, Color(0xFFC2E7FF).copy(alpha = 0.5f)),
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FileDownload,
                contentDescription = null,
                tint = Color(0xFFC2E7FF),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC2E7FF)
            )
        }
    }
}

@Composable
fun MarkdownActionChip(
    label: String,
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDownload = label.contains("Unduh", ignoreCase = true) ||
                     label.contains("Download", ignoreCase = true) ||
                     label.equals("PDF", ignoreCase = true) ||
                     url.endsWith(".pdf", ignoreCase = true)
    val isDoi = label.contains("DOI", ignoreCase = true) || url.contains("doi.org", ignoreCase = true)

    val containerColor = if (isDownload) Color(0xFF004A77) else if (isDoi) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val contentColor = if (isDownload) Color(0xFFC2E7FF) else if (isDoi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = if (isDownload) Icons.Rounded.FileDownload else if (isDoi) Icons.Rounded.OpenInNew else Icons.Rounded.Article

    Surface(
        onClick = { downloadOrOpenUrl(context, url, suggestedTitle = label) },
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.35f)),
        modifier = modifier.padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

data class ExtractedDownloadLink(val label: String, val url: String)

fun extractDownloadLinks(text: String): List<ExtractedDownloadLink> {
    val regex = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
    return regex.findAll(text).mapNotNull { match ->
        val label = match.groupValues[1].trim()
        val url = match.groupValues[2].trim()
        val isDownload = label.contains("Unduh", ignoreCase = true) ||
                         label.contains("Download", ignoreCase = true) ||
                         url.endsWith(".pdf", ignoreCase = true)
        if (isDownload) ExtractedDownloadLink(label, url) else null
    }.toList()
}

fun downloadOrOpenUrl(context: Context, url: String, suggestedTitle: String? = null) {
    try {
        if (url.startsWith("file://")) {
            val file = File(url.removePrefix("file://"))
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        val uri = Uri.parse(url)
        val isPdf = url.endsWith(".pdf", ignoreCase = true) || url.contains("/pdf/", ignoreCase = true)
        if (isPdf) {
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                if (dm != null) {
                    val rawName = if (!suggestedTitle.isNullOrBlank()) {
                        suggestedTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
                    } else {
                        "paper_${System.currentTimeMillis()}"
                    }
                    val filename = if (rawName.endsWith(".pdf", ignoreCase = true)) rawName else "$rawName.pdf"
                    val req = DownloadManager.Request(uri)
                        .setTitle(suggestedTitle ?: "Jurnal Ilmiah")
                        .setDescription("Mengunduh paper jurnal...")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "papers/$filename")
                    dm.enqueue(req)
                    Toast.makeText(context, "Mulai mengunduh: $filename", Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (_: Exception) {
            }
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membuka tautan: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun formatInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("(\\*\\*\\*.*?\\*\\*\\*|\\*\\*.*?\\*\\*|\\*.*?\\*|~~.*?~~|`.*?`|\\[[^\\]]+\\]\\([^)]+\\))")
        val matches = regex.findAll(text)

        for (match in matches) {
            val range = match.range
            if (range.first > cursor) {
                append(text.substring(cursor, range.first))
            }

            val value = match.value
            when {
                value.startsWith("***") && value.endsWith("***") && value.length >= 6 -> {
                    val inner = value.removeSurrounding("***")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                }
                value.startsWith("**") && value.endsWith("**") && value.length >= 4 -> {
                    val inner = value.removeSurrounding("**")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(inner)
                    pop()
                }
                value.startsWith("*") && value.endsWith("*") && value.length >= 2 -> {
                    val inner = value.removeSurrounding("*")
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                }
                value.startsWith("~~") && value.endsWith("~~") && value.length >= 4 -> {
                    val inner = value.removeSurrounding("~~")
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(inner)
                    pop()
                }
                value.startsWith("`") && value.endsWith("`") && value.length >= 2 -> {
                    val inner = value.removeSurrounding("`")
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF1E222A),
                            color = Color(0xFFA8C7FA),
                            fontSize = 13.sp
                        )
                    )
                    append(" $inner ")
                    pop()
                }
                value.startsWith("[") && value.contains("](") && value.endsWith(")") -> {
                    val linkMatch = Regex("^\\[([^\\]]+)\\]\\(([^)]+)\\)$").find(value)
                    if (linkMatch != null) {
                        val label = linkMatch.groupValues[1]
                        val url = linkMatch.groupValues[2]
                        val linkStyle = TextLinkStyles(
                            style = SpanStyle(
                                color = Color(0xFFA8C7FA),
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        withLink(LinkAnnotation.Url(url, linkStyle)) {
                            append(label)
                        }
                    } else {
                        append(value)
                    }
                }
                else -> append(value)
            }
            cursor = range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
