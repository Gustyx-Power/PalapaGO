package id.gustyxganteng.agent.core.markdown

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import id.gustyxganteng.agent.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.io.File

sealed interface MarkdownNode {
    data class Header(val level: Int, val text: String) : MarkdownNode
    data class Paragraph(val text: String) : MarkdownNode
    data class CodeBlock(val language: String, val code: String) : MarkdownNode
    data class BlockQuote(val text: String) : MarkdownNode
    data class BulletItem(val text: String, val depth: Int = 0) : MarkdownNode
    data class NumberedItem(val number: String, val text: String, val depth: Int = 0) : MarkdownNode
    data object Divider : MarkdownNode
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownNode
    data class MathBlock(val formula: String) : MarkdownNode
    data class ImageBlock(val alt: String, val url: String) : MarkdownNode
}

object MarkdownParser {
    fun parse(markdown: String): List<MarkdownNode> {
        val lines = markdown.lines()
        val nodes = mutableListOf<MarkdownNode>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // 1. LaTeX block math ($$ ... $$)
            if (line.trim().startsWith("$$")) {
                val trimmed = line.trim()
                if (trimmed.length > 2 && trimmed.endsWith("$$")) {
                    val formula = trimmed.removeSurrounding("$$").trim()
                    nodes.add(MarkdownNode.MathBlock(formula))
                    i++
                    continue
                } else {
                    val formulaBuilder = StringBuilder()
                    val rest = trimmed.removePrefix("$$").trim()
                    if (rest.isNotEmpty()) formulaBuilder.appendLine(rest)
                    i++
                    while (i < lines.size && !lines[i].trim().endsWith("$$")) {
                        formulaBuilder.appendLine(lines[i])
                        i++
                    }
                    if (i < lines.size) {
                        val last = lines[i].trim().removeSuffix("$$").trim()
                        if (last.isNotEmpty()) formulaBuilder.appendLine(last)
                        i++
                    }
                    nodes.add(MarkdownNode.MathBlock(formulaBuilder.toString().trim()))
                    continue
                }
            }

            // 2. LaTeX block math (\[ ... \])
            if (line.trim().startsWith("\\[")) {
                val trimmed = line.trim()
                if (trimmed.endsWith("\\]")) {
                    val formula = trimmed.removePrefix("\\[").removeSuffix("\\]").trim()
                    nodes.add(MarkdownNode.MathBlock(formula))
                    i++
                    continue
                } else {
                    val formulaBuilder = StringBuilder()
                    val rest = trimmed.removePrefix("\\[").trim()
                    if (rest.isNotEmpty()) formulaBuilder.appendLine(rest)
                    i++
                    while (i < lines.size && !lines[i].trim().contains("\\]")) {
                        formulaBuilder.appendLine(lines[i])
                        i++
                    }
                    if (i < lines.size) {
                        val last = lines[i].trim().removeSuffix("\\]").trim()
                        if (last.isNotEmpty()) formulaBuilder.appendLine(last)
                        i++
                    }
                    nodes.add(MarkdownNode.MathBlock(formulaBuilder.toString().trim()))
                    continue
                }
            }

            // 3. Code blocks ```lang (including math/latex/tex)
            val fence = Regex("^ {0,3}(`{3,}|~{3,})(.*)$").matchEntire(line)
            if (fence != null) {
                val marker = fence.groupValues[1]
                val lang = fence.groupValues[2].trim().substringBefore(' ')
                val closing = Regex("^ {0,3}${marker.first()}{${marker.length},}\\s*$")
                val codeBuilder = StringBuilder()
                i++
                while (i < lines.size && !closing.matches(lines[i])) {
                    codeBuilder.appendLine(lines[i])
                    i++
                }
                if (lang.lowercase() in listOf("math", "latex", "tex")) {
                    nodes.add(MarkdownNode.MathBlock(codeBuilder.toString().trimEnd()))
                } else {
                    nodes.add(MarkdownNode.CodeBlock(lang, codeBuilder.toString().removeSuffix("\n")))
                }
                i++
                continue
            }

            // 4. Standalone un-delimited pure LaTeX math formulas (no prose words)
            val rawTrimmed = line.trim()
            if (isPureMathEquation(rawTrimmed)) {
                nodes.add(MarkdownNode.MathBlock(rawTrimmed))
                i++
                continue
            }

            // 5. Table (| header | or col1 | col2)
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
            if (Regex("^(?:\\*\\s*){3,}$|^(?:-\\s*){3,}$|^(?:_\\s*){3,}$").matches(trimmed)) {
                nodes.add(MarkdownNode.Divider)
                i++
                continue
            }
            if (Regex("^#{1,6}\\s+.*").matches(trimmed)) {
                val level = trimmed.takeWhile { it == '#' }.length
                val text = trimmed.drop(level).trim().replace(Regex("\\s+#+\\s*$"), "")
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
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("+ ")) {
                val text = trimmed.drop(2).trim()
                nodes.add(MarkdownNode.BulletItem(text, line.takeWhile { it == ' ' }.length / 2))
                i++
                continue
            }

            // 6. Numbered item (1. item)
            val numberedMatch = Regex("^(\\d+)[.)]\\s+(.*)").find(trimmed)
            if (numberedMatch != null) {
                val num = numberedMatch.groupValues[1]
                val text = numberedMatch.groupValues[2]
                nodes.add(MarkdownNode.NumberedItem(num, text, line.takeWhile { it == ' ' }.length / 2))
                i++
                continue
            }

            // 7. Standalone or embedded image (![alt](url))
            val imgMatch = Regex("""!\[(.*?)\]\(((?:https?://|data:image/)[^\s)]+)\)""").find(trimmed)
            if (imgMatch != null) {
                val prefix = trimmed.substring(0, imgMatch.range.first).trim()
                val alt = imgMatch.groupValues[1]
                val url = imgMatch.groupValues[2]
                val postfix = trimmed.substring(imgMatch.range.last + 1).trim()
                if (prefix.isNotEmpty()) {
                    nodes.add(MarkdownNode.Paragraph(prefix))
                }
                nodes.add(MarkdownNode.ImageBlock(alt, url))
                if (postfix.isNotEmpty()) {
                    nodes.add(MarkdownNode.Paragraph(postfix))
                }
                i++
                continue
            }

            // 8. Regular paragraph / empty line
            if (trimmed.isNotEmpty()) {
                val previous = nodes.lastOrNull()
                val continuesParagraph = i > 0 && lines[i - 1].isNotBlank() && previous is MarkdownNode.Paragraph
                if (continuesParagraph) {
                    val separator = if (lines[i - 1].endsWith("  ") || lines[i - 1].endsWith("\\")) "\n" else " "
                    nodes[nodes.lastIndex] = MarkdownNode.Paragraph((previous as MarkdownNode.Paragraph).text.removeSuffix("\\") + separator + trimmed)
                } else {
                    nodes.add(MarkdownNode.Paragraph(trimmed))
                }
            }
            i++
        }

        return nodes
    }

    private fun isPureMathEquation(line: String): Boolean {
        val t = line.trim()
        if (t.isEmpty()) return false
        if (t.contains("$") && !t.startsWith("$$")) return false
        if (t.startsWith("#") || t.startsWith("- ") || t.startsWith("* ") || t.startsWith(">")) return false

        val rawWords = t.split(Regex("[^a-zA-Z\\\\]+")).filter { it.isNotEmpty() }
        val allowedMathTerms = setOf("sin", "cos", "tan", "exp", "log", "ln", "det", "min", "max", "sup", "inf", "lim", "dim", "ker", "deg", "gcd", "mod")
        val proseWords = rawWords.filter { w ->
            !w.startsWith("\\") && w.length >= 3 && w.lowercase() !in allowedMathTerms
        }
        if (proseWords.size >= 2) return false

        return t.startsWith("\\frac") || t.startsWith("\\sum") || t.startsWith("\\int") ||
               t.startsWith("\\prod") || t.startsWith("\\begin{") || t.startsWith("\\lim") ||
               (t.startsWith("\\") && t.contains("=") && proseWords.isEmpty()) ||
               (t.contains("\\frac{") && t.contains("}") && proseWords.isEmpty())
    }

    private fun isTableRow(line: String): Boolean {
        val t = line.trim()
        if (t.isEmpty() || t.startsWith("```") || t.startsWith("#")) return false
        return t.contains('|')
    }

    private fun isTableSeparator(line: String): Boolean {
        val t = line.trim()
        if (!t.contains("-")) return false
        val cells = parseRowCells(t)
        return cells.isNotEmpty() && cells.all { cell ->
            val inner = cell.trim()
            inner.matches(Regex("^:?-{3,}:?$"))
        }
    }

    private fun parseRowCells(line: String): List<String> {
        var t = line.trim()
        if (t.startsWith("|")) t = t.substring(1)
        if (t.endsWith("|")) t = t.substring(0, t.length - 1)
        return t.split(Regex("(?<!\\\\)\\|")).map { it.trim().replace("\\|", "|") }
    }
}

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    onOpenInCanvas: ((title: String, language: String, content: String) -> Unit)? = null
) {
    val nodes = remember(markdown) { MarkdownParser.parse(markdown) }

    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            nodes.forEach { node ->
                when (node) {
                    MarkdownNode.Divider -> HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
                            text = formatInlineMarkdown(node.text),
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
                                    fontSize = 16.sp,
                                    lineHeight = 25.sp,
                                    letterSpacing = 0.15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            downloadLinks.forEach { link ->
                                MarkdownDownloadButton(label = link.label, url = link.url)
                            }
                        }
                    }
                    is MarkdownNode.ImageBlock -> {
                        MarkdownImageCard(
                            alt = node.alt,
                            url = node.url,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
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
                                    .padding(start = (4 + node.depth.coerceAtMost(8) * 16).dp, top = 2.dp, bottom = 2.dp),
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
                                        fontSize = 16.sp,
                                        lineHeight = 25.sp
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
                                .padding(start = (4 + node.depth.coerceAtMost(8) * 16).dp, top = 2.dp, bottom = 2.dp),
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
                                    fontSize = 16.sp,
                                    lineHeight = 25.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    is MarkdownNode.Table -> {
                        TableCard(node.headers, node.rows)
                    }
                    is MarkdownNode.MathBlock -> {
                        MathBlockCard(formula = node.formula)
                    }
                }
            }
        }
    }
}

@Composable
fun MathBlockCard(formula: String) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF171A1F), // MaterialTheme.colorScheme.surfaceContainerLow
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Functions,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Formula (LaTeX)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(formula))
                        isCopied = true
                        Toast.makeText(context, "Rumus LaTeX disalin", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy formula",
                            tint = if (isCopied) Color(0xFF7BE598) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isCopied) "Tersalin" else "Salin LaTeX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isCopied) Color(0xFF7BE598) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = true
                        setBackgroundColor(0)
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val escaped = formula
                                    .replace("\\", "\\\\")
                                    .replace("'", "\\'")
                                    .replace("\n", "\\n")
                                    .replace("\r", "")
                                evaluateJavascript("renderLatex('$escaped');", null)
                            }
                        }
                        loadUrl("file:///android_asset/katex/katex_template.html")
                    }
                },
                update = { webView ->
                    val escaped = formula
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "")
                    webView.evaluateJavascript("renderLatex('$escaped');", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp, max = 240.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayLang,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(code))
                            Toast.makeText(context, context.getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Kode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (onOpenInCanvas != null) {
                        IconButton(
                            onClick = {
                                onOpenInCanvas("Snippet ($displayLang)", displayLang, code)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Buka di Canvas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
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
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 10.dp, horizontal = 6.dp),
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
                            text = formatInlineMarkdown(header),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Body Rows
            rows.forEachIndexed { rowIndex, row ->
                val bg = if (rowIndex % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                Row(
                    modifier = Modifier
                        .background(bg)
                        .padding(vertical = 8.dp, horizontal = 6.dp),
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
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FileDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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

    val containerColor = if (isDownload) MaterialTheme.colorScheme.primaryContainer else if (isDoi) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (isDownload) MaterialTheme.colorScheme.onPrimaryContainer else if (isDoi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = if (isDownload) Icons.Rounded.FileDownload else if (isDoi) Icons.AutoMirrored.Rounded.OpenInNew else Icons.AutoMirrored.Rounded.Article

    Surface(
        onClick = { downloadOrOpenUrl(context, url, suggestedTitle = label) },
        shape = RoundedCornerShape(10.dp),
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

fun formatInlineLatexToPrettyMath(latex: String): String {
    var s = latex.trim()

    val greek = mapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\epsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η", "\\theta" to "θ",
        "\\iota" to "ι", "\\kappa" to "κ", "\\lambda" to "λ", "\\mu" to "μ",
        "\\nu" to "ν", "\\xi" to "ξ", "\\pi" to "π", "\\rho" to "ρ",
        "\\sigma" to "σ", "\\tau" to "τ", "\\upsilon" to "υ", "\\phi" to "φ",
        "\\chi" to "χ", "\\psi" to "ψ", "\\omega" to "ω",
        "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
        "\\Xi" to "Ξ", "\\Pi" to "Π", "\\Sigma" to "Σ", "\\Phi" to "Φ",
        "\\Psi" to "Ψ", "\\Omega" to "Ω"
    )
    for ((cmd, char) in greek) {
        s = s.replace(cmd, char)
    }

    val mathFuncs = mapOf(
        "\\sum" to "∑", "\\prod" to "∏", "\\int" to "∫",
        "\\ln" to "ln", "\\exp" to "exp", "\\log" to "log",
        "\\sin" to "sin", "\\cos" to "cos", "\\tan" to "tan",
        "\\lim" to "lim", "\\min" to "min", "\\max" to "max",
        "\\det" to "det", "\\sup" to "sup", "\\inf" to "inf"
    )
    for ((cmd, char) in mathFuncs) {
        s = s.replace(cmd, char)
    }

    val symbols = mapOf(
        "\\times" to "×", "\\div" to "÷", "\\pm" to "±", "\\mp" to "∓",
        "\\cdot" to "·", "\\neq" to "≠", "\\approx" to "≈", "\\equiv" to "≡",
        "\\leq" to "≤", "\\le" to "≤", "\\geq" to "≥", "\\ge" to "≥",
        "\\infty" to "∞", "\\in" to "∈", "\\notin" to "∉", "\\subset" to "⊂",
        "\\subseteq" to "⊆", "\\cup" to "∪", "\\cap" to "∩", "\\emptyset" to "∅",
        "\\forall" to "∀", "\\exists" to "∃", "\\nabla" to "∇", "\\partial" to "∂",
        "\\to" to "→", "\\rightarrow" to "→", "\\leftarrow" to "←",
        "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐", "\\Leftrightarrow" to "⇔"
    )
    for ((cmd, char) in symbols) {
        s = s.replace(cmd, char)
    }

    s = s.replace(Regex("\\\\overline\\{([^}]+)\\}")) { "${it.groupValues[1]}̄" }
    s = s.replace(Regex("\\\\bar\\{([^}]+)\\}")) { "${it.groupValues[1]}̄" }
    s = s.replace(Regex("\\\\vec\\{([^}]+)\\}")) { "${it.groupValues[1]}⃗" }
    s = s.replace(Regex("\\\\hat\\{([^}]+)\\}")) { "${it.groupValues[1]}̂" }
    s = s.replace(Regex("\\\\mathbb\\{([^}]+)\\}")) { it.groupValues[1] }
    s = s.replace(Regex("\\\\mathbf\\{([^}]+)\\}")) { it.groupValues[1] }
    s = s.replace(Regex("\\\\text\\{([^}]+)\\}")) { it.groupValues[1] }
    s = s.replace(Regex("\\\\mathcal\\{([^}]+)\\}")) { it.groupValues[1] }

    val superscripts = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ'
    )
    s = s.replace(Regex("\\^\\{([0-9+\\-()ni]+)\\}")) { match ->
        match.groupValues[1].map { superscripts[it] ?: it }.joinToString("")
    }
    s = s.replace(Regex("\\^([0-9+\\-()ni])")) { match ->
        val c = match.groupValues[1][0]
        superscripts[c]?.toString() ?: "^$c"
    }

    val subscripts = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ', 'h' to 'ₕ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'p' to 'ₚ',
        's' to 'ₛ', 't' to 'ₜ', 'i' to 'ᵢ', 'j' to 'ⱼ'
    )
    s = s.replace(Regex("_\\{([0-9+\\-()aeoxhklmnpstij]+)\\}")) { match ->
        match.groupValues[1].map { subscripts[it] ?: it }.joinToString("")
    }
    s = s.replace(Regex("_([0-9+\\-()aeoxhklmnpstij])")) { match ->
        val c = match.groupValues[1][0]
        subscripts[c]?.toString() ?: "_$c"
    }

    s = s.replace(Regex("\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}")) { match ->
        "(${match.groupValues[1]}/${match.groupValues[2]})"
    }
    s = s.replace(Regex("\\\\sqrt\\{([^}]+)\\}")) { match ->
        "√(${match.groupValues[1]})"
    }
    s = s.replace("{", "").replace("}", "")

    return s
}

fun prettyFormatPlainText(plain: String): String {
    var s = plain
    s = s.replace("\\to", "→")
        .replace("\\rightarrow", "→")
        .replace("\\leftarrow", "←")
        .replace("\\Rightarrow", "⇒")
        .replace("\\Leftarrow", "⇐")
        .replace("\\infty", "∞")
        .replace("\\approx", "≈")
        .replace("\\neq", "≠")
        .replace("\\leq", "≤")
        .replace("\\le", "≤")
        .replace("\\geq", "≥")
        .replace("\\ge", "≥")
        .replace("\\pm", "±")
        .replace("\\times", "×")
        .replace("\\cdot", "·")
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\gamma", "γ")
        .replace("\\theta", "θ")
        .replace("\\pi", "π")
        .replace("\\sigma", "σ")
        .replace("\\sum", "∑")
        .replace("\\int", "∫")
        .replace("\\prod", "∏")

    s = s.replace("^2", "²")
        .replace("^3", "³")
        .replace("^0", "⁰")
        .replace("^1", "¹")
        .replace("^n", "ⁿ")
        .replace("^-1", "⁻¹")

    return s
}

fun formatInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("(\\*\\*\\*.*?\\*\\*\\*|\\*\\*.*?\\*\\*|\\*.*?\\*|~~.*?~~|`.*?`|\\[[^\\]]+\\]\\([^)]+\\)|(?<!\\\\)\\$(?!\\s)[^\\$\\n]+?(?<!\\s)(?<!\\\\)\\$|\\\\\\([\\s\\S]+?\\\\\\))")
        val matches = regex.findAll(text)

        for (match in matches) {
            val range = match.range
            if (range.first > cursor) {
                append(prettyFormatPlainText(text.substring(cursor, range.first)))
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
                value.startsWith("$") && value.endsWith("$") && value.length >= 2 -> {
                    val inner = value.removeSurrounding("$")
                    val pretty = formatInlineLatexToPrettyMath(inner)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFFA8C7FA),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(pretty)
                    pop()
                }
                value.startsWith("\\(") && value.endsWith("\\)") && value.length >= 4 -> {
                    val inner = value.removePrefix("\\(").removeSuffix("\\)")
                    val pretty = formatInlineLatexToPrettyMath(inner)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFFA8C7FA),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(pretty)
                    pop()
                }
                else -> append(prettyFormatPlainText(value))
            }
            cursor = range.last + 1
        }

        if (cursor < text.length) {
            append(prettyFormatPlainText(text.substring(cursor)))
        }
    }
}

@Composable
fun MarkdownImageCard(
    alt: String,
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFullscreen by remember { mutableStateOf(false) }

    val imageModel: Any = remember(url) {
        if (url.startsWith("data:image/") && url.contains("base64,")) {
            try {
                val cleanB64 = url.substringAfter("base64,")
                android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
            } catch (_: Exception) {
                url
            }
        } else {
            url
        }
    }
    var retryTrigger by remember { mutableIntStateOf(0) }

    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullscreen = false },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                        .build(),
                    contentDescription = alt,
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { showFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup",
                        tint = Color.White
                    )
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 380.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .clickable { showFullscreen = true },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .setParameter("retry", retryTrigger, memoryCacheKey = null)
                        .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                        .build(),
                    contentDescription = alt,
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    error = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Gagal memuat visual",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable { retryTrigger++ }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Muat Ulang",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Coba Lagi",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }
            if (alt.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = alt,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                try {
                                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                                    if (dm != null) {
                                        val filename = "palapago_${System.currentTimeMillis()}.png"
                                        val req = DownloadManager.Request(Uri.parse(url))
                                            .setTitle("PalapaGO Image Gen")
                                            .setDescription(alt.ifBlank { "Mengunduh gambar..." })
                                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "PalapaGO/$filename")
                                        dm.enqueue(req)
                                        Toast.makeText(context, "Mulai mengunduh ke Pictures/PalapaGO", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal mengunduh: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = "Unduh Gambar",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = "Buka di Browser",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
