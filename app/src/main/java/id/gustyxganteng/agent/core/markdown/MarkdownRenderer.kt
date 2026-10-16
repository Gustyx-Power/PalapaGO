package id.gustyxganteng.agent.core.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

            // 2. Table (| header |)
            if (line.trim().startsWith("|") && line.trim().endsWith("|") && i + 1 < lines.size && lines[i + 1].contains("---")) {
                val headers = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                i += 2 // skip header and separator
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    val row = lines[i].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    rows.add(row)
                    i++
                }
                nodes.add(MarkdownNode.Table(headers, rows))
                continue
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
}

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    onOpenInCanvas: ((title: String, language: String, content: String) -> Unit)? = null
) {
    val nodes = MarkdownParser.parse(markdown)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        nodes.forEach { node ->
            when (node) {
                is MarkdownNode.Header -> {
                    val style = when (node.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        3 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(
                        text = node.text,
                        style = style.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is MarkdownNode.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(node.text),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatInlineMarkdown(node.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                is MarkdownNode.BulletItem -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                        Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatInlineMarkdown(node.text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is MarkdownNode.NumberedItem -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                        Text("${node.number}. ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatInlineMarkdown(node.text),
                            style = MaterialTheme.typography.bodyMedium,
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

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier,
    onOpenInCanvas: ((title: String, language: String, content: String) -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val displayLang = language.ifEmpty { "text" }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayLang.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(code))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Kode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp)
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
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Code content with horizontal scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
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
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .width(130.dp)
                            .padding(horizontal = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Body Rows
            rows.forEachIndexed { index, row ->
                val bg = if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                Row(
                    modifier = Modifier
                        .background(bg)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .width(130.dp)
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`)")
        val matches = regex.findAll(text)

        for (match in matches) {
            val range = match.range
            if (range.first > cursor) {
                append(text.substring(cursor, range.first))
            }

            val value = match.value
            when {
                value.startsWith("**") && value.endsWith("**") -> {
                    val inner = value.removeSurrounding("**")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(inner)
                    pop()
                }
                value.startsWith("*") && value.endsWith("*") -> {
                    val inner = value.removeSurrounding("*")
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                }
                value.startsWith("`") && value.endsWith("`") -> {
                    val inner = value.removeSurrounding("`")
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x33888888)))
                    append(" $inner ")
                    pop()
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
