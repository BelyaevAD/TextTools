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
    var index = 0
    return buildList {
        while (index < lines.size) {
            val taken = takeBlock(lines, index)
            if (taken == null) {
                index += 1
            } else {
                add(taken.first)
                index = taken.second
            }
        }
    }
}

private fun takeBlock(
    lines: List<String>,
    index: Int,
): Pair<MdBlock, Int>? {
    readFence(lines, index)?.let { return it }
    readTable(lines, index)?.let { return it }
    readBullets(lines, index)?.let { return it }
    readNumbers(lines, index)?.let { return it }
    val line = lines[index]
    readHeading(line, index)?.let { return it }
    if (line.isBlank()) return null
    return MdBlock.Paragraph(line) to (index + 1)
}

private fun readFence(
    lines: List<String>,
    index: Int,
): Pair<MdBlock, Int>? {
    if (!lines[index].startsWith("```")) return null
    var cursor = index + 1
    val body =
        buildList {
            while (cursor < lines.size && !lines[cursor].startsWith("```")) {
                add(lines[cursor])
                cursor += 1
            }
        }
    if (cursor < lines.size) cursor += 1
    return MdBlock.Code(body.joinToString("\n")) to cursor
}

private fun readTable(
    lines: List<String>,
    index: Int,
): Pair<MdBlock, Int>? {
    val line = lines[index]
    if (index + 1 >= lines.size || !line.trim().startsWith("|") || !lines[index + 1].contains("---")) return null
    val header = splitRow(line)
    var cursor = index + 2
    val rows =
        buildList {
            while (cursor < lines.size && lines[cursor].trim().startsWith("|")) {
                add(splitRow(lines[cursor]))
                cursor += 1
            }
        }
    return MdBlock.Table(header, rows) to cursor
}

private fun readBullets(
    lines: List<String>,
    index: Int,
): Pair<MdBlock, Int>? {
    if (!lines[index].startsWith("- ")) return null
    var cursor = index
    val items =
        buildList {
            while (cursor < lines.size && lines[cursor].startsWith("- ")) {
                add(lines[cursor].removePrefix("- "))
                cursor += 1
            }
        }
    return MdBlock.ListBlock(ordered = false, items) to cursor
}

private fun readNumbers(
    lines: List<String>,
    index: Int,
): Pair<MdBlock, Int>? {
    val pattern = Regex("""\d+\. .+""")
    if (!pattern.matches(lines[index])) return null
    var cursor = index
    val items =
        buildList {
            while (cursor < lines.size && pattern.matches(lines[cursor])) {
                add(lines[cursor].substringAfter(". "))
                cursor += 1
            }
        }
    return MdBlock.ListBlock(ordered = true, items) to cursor
}

private fun readHeading(
    line: String,
    index: Int,
): Pair<MdBlock, Int>? {
    if (!line.startsWith("#")) return null
    val level = line.takeWhile { it == '#' }.length
    return MdBlock.Heading(level, line.drop(level).trim()) to (index + 1)
}

private fun splitRow(line: String): List<String> =
    line
        .trim()
        .trim('|')
        .split('|')
        .map { it.trim() }
