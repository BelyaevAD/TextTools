package dev.belyaevad.texttools.domain

/** A markdown document the result window draws without turning model text into HTML. */
sealed class MdBlock {
    /** A heading. */
    data class Heading(
        val level: Int,
        val text: String,
    ) : MdBlock()

    /** A paragraph. Inline markers stay as text for the view to paint. */
    data class Paragraph(
        val text: String,
    ) : MdBlock()

    /** A fenced code block. Markup inside it is literal. */
    data class Code(
        val text: String,
    ) : MdBlock()

    /** A bullet or numbered list. */
    data class ListBlock(
        val ordered: Boolean,
        val items: List<String>,
    ) : MdBlock()

    /** A table. Rendered only when a separator row was present. */
    data class Table(
        val header: List<String>,
        val rows: List<List<String>>,
    ) : MdBlock()
}

/** Parses the markdown subset the result window needs. Model text is data, not HTML. */
fun parseMarkdown(source: String): List<MdBlock> {
    val lines = source.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MdBlock>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        if (line.startsWith("```")) {
            val body = mutableListOf<String>()
            index += 1
            while (index < lines.size && !lines[index].startsWith("```")) {
                body += lines[index]
                index += 1
            }
            if (index < lines.size) index += 1
            blocks += MdBlock.Code(body.joinToString("\n"))
            continue
        }
        if (index + 1 < lines.size && line.trim().startsWith("|") && lines[index + 1].contains("---")) {
            val header = splitRow(line)
            val rows = mutableListOf<List<String>>()
            index += 2
            while (index < lines.size && lines[index].trim().startsWith("|")) {
                rows += splitRow(lines[index])
                index += 1
            }
            blocks += MdBlock.Table(header, rows)
            continue
        }
        if (line.startsWith("- ")) {
            val items = mutableListOf<String>()
            while (index < lines.size && lines[index].startsWith("- ")) {
                items += lines[index].removePrefix("- ")
                index += 1
            }
            blocks += MdBlock.ListBlock(false, items)
            continue
        }
        if (line.matches(Regex("""\d+\. .+"""))) {
            val items = mutableListOf<String>()
            while (index < lines.size && lines[index].matches(Regex("""\d+\. .+"""))) {
                items += lines[index].substringAfter(". ")
                index += 1
            }
            blocks += MdBlock.ListBlock(true, items)
            continue
        }
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            blocks += MdBlock.Heading(level, line.drop(level).trim())
            index += 1
            continue
        }
        if (line.isBlank()) {
            index += 1
            continue
        }
        blocks += MdBlock.Paragraph(line)
        index += 1
    }
    return blocks
}

private fun splitRow(line: String): List<String> =
    line
        .trim()
        .trim('|')
        .split('|')
        .map { it.trim() }
