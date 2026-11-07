package dev.belyaevad.texttools.data

import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.AppProfile
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.HISTORY_LIMIT
import dev.belyaevad.texttools.domain.KnownModel
import dev.belyaevad.texttools.domain.OutputLanguage
import dev.belyaevad.texttools.domain.QuickRefine
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Reads an AHK state folder. The key and the proxy URL are returned, not written to SQLite. */
class LegacyImport(private val store: TextToolsStore) {
    /** Result of one import. Secrets are only in this object. */
    data class Result(val apiKey: String, val proxyUrl: String)

    /**
     * Imports [root] into the store and copies the last-result file plus user language ini files into [dataDir].
     * Does not modify [root]. A file that is missing or not valid UTF-8 leaves the table it owns as it was.
     */
    fun importFolder(root: File, dataDir: File): Result {
        importActions(root)
        importApps(root)
        importLangs(root)
        importHistory(root)
        importUsage(root)
        copyUserLangs(root, dataDir)
        copyFile(File(root, "TextTools_last.txt"), File(dataDir, "TextTools_last.txt"), root)
        val settings = readIni(File(root, "TextTools.ini"))
        if (settings != null) {
            importModels(settings)
        }
        return Result(
            apiKey = settings?.get("api.api_key").orEmpty(),
            proxyUrl = settings?.get("api.proxy").orEmpty(),
        )
    }

    private fun importActions(root: File) {
        val ini = readIni(File(root, "actions.ini")) ?: return
        val actions = parseActions(ini)
        if (actions.isNotEmpty()) {
            store.saveActions(actions)
        }
        val quick = parseQuick(ini)
        if (quick.isNotEmpty() || ini["quick.count"] == "0") {
            store.saveQuick(quick)
        }
    }

    private fun importApps(root: File) {
        val ini = readIni(File(root, "apps.ini")) ?: return
        val apps = parseApps(ini)
        if (apps.isNotEmpty()) {
            store.saveApps(apps)
        }
    }

    private fun importLangs(root: File) {
        val ini = readIni(File(root, "langs.ini")) ?: return
        val languages = parseLangs(ini)
        if (languages.isNotEmpty()) {
            store.saveLanguages(languages)
        }
    }

    private fun importHistory(root: File) {
        val text = readUtf8(File(root, "TextTools_history.txt")) ?: return
        val rows = parseHistory(text) ?: return
        store.saveHistory(rows)
    }

    private fun importUsage(root: File) {
        val text = readUtf8(File(root, "TextTools_usage.log")) ?: return
        val rows = parseUsage(text) ?: return
        store.saveUsage(rows)
    }

    private fun importModels(ini: Map<String, String>) {
        val url = ini["api.models_url"].orEmpty().ifEmpty { ini["api.base_url"].orEmpty() }
        if (url.isEmpty()) {
            return
        }
        val selected = splitList(ini["api.models"])
        val known = splitList(ini["api.models_known"])
        if (selected.isEmpty() && known.isEmpty()) {
            return
        }
        val picked = selected.toSet()
        store.saveModels(url, (selected + known).distinct().map { KnownModel(it, it in picked) })
    }

    private fun copyUserLangs(root: File, dataDir: File) {
        val srcDir = File(root, "lang/user")
        if (!srcDir.isDirectory) {
            return
        }
        val files = srcDir.listFiles() ?: return
        for (src in files) {
            if (src.isFile && src.name.endsWith(".ini", ignoreCase = true)) {
                copyFile(src, File(dataDir, "lang/user/${src.name}"), root)
            }
        }
    }
}

private val minuteStamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val secondStamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private fun parseActions(ini: Map<String, String>): List<Action> {
    val out = mutableListOf<Action>()
    for (index in 1..Factory.MAX_ACTIONS) {
        val rawName = ini["action$index.name"] ?: break
        if (rawName.isEmpty()) break
        val rawTask = ini["action$index.task"].orEmpty()
        if (rawTask.isEmpty()) continue
        val rawId = ini["action$index.id"].orEmpty()
        val rawKey = ini["action$index.key"] ?: Factory.menuKeys.getOrElse(index - 1) { "" }
        val rawModel = ini["action$index.model"].orEmpty()
        out += Action(
            factoryId = rawId.takeUnless { it.isEmpty() || it == "-" },
            name = jsonUnesc(rawName),
            icon = jsonUnesc(ini["action$index.icon"].orEmpty()),
            prefix = jsonUnesc(ini["action$index.prefix"].orEmpty()),
            task = jsonUnesc(rawTask),
            lang = ini["action$index.lang"] ?: "auto",
            styleOn = (ini["action$index.ru"] ?: "1") == "1",
            openInWindow = (ini["action$index.win"] ?: "0") == "1",
            menuKey = rawKey.takeUnless { it.isEmpty() || it == "-" },
            model = rawModel.takeUnless { it.isEmpty() || it == "-" },
            role = null,
        )
    }
    return out
}

private fun parseQuick(ini: Map<String, String>): List<QuickRefine> {
    val out = mutableListOf<QuickRefine>()
    for (index in 1..Factory.MAX_ACTIONS) {
        val rawLabel = ini["quick$index.label"] ?: break
        if (rawLabel.isEmpty()) break
        val rawInstr = ini["quick$index.instr"].orEmpty()
        if (rawInstr.isEmpty()) continue
        out += QuickRefine(jsonUnesc(rawLabel), jsonUnesc(rawInstr))
    }
    return out
}

private fun parseApps(ini: Map<String, String>): List<AppProfile> {
    val out = mutableListOf<AppProfile>()
    for (index in 1..Factory.MAX_ACTIONS) {
        val proc = jsonUnesc(ini["app$index.proc"] ?: break)
        if (proc.isEmpty()) break
        out += AppProfile(
            proc = proc,
            title = emptyDash(jsonUnesc(ini["app$index.title"].orEmpty())),
            name = emptyDash(jsonUnesc(ini["app$index.name"].orEmpty())),
            description = emptyDash(jsonUnesc(ini["app$index.desc"].orEmpty())),
            hint = emptyDash(jsonUnesc(ini["app$index.hint"].orEmpty())),
        )
    }
    return out
}

private fun parseLangs(ini: Map<String, String>): List<OutputLanguage> {
    val out = mutableListOf<OutputLanguage>()
    for (index in 1..Factory.MAX_ACTIONS) {
        val code = ini["lang$index.code"] ?: break
        if (code.isEmpty()) break
        val name = jsonUnesc(ini["lang$index.name"] ?: code)
        val rawPrompt = ini["lang$index.prompt"]
        val prompt = if (rawPrompt.isNullOrEmpty()) name else jsonUnesc(rawPrompt)
        out += OutputLanguage(code, name, prompt, (ini["lang$index.on"] ?: "0") == "1")
    }
    return out
}

/** The file is newest first. An empty file clears history; unreadable rows leave it alone. */
private fun parseHistory(text: String): List<HistoryRecord>? {
    val newestFirst = mutableListOf<HistoryRecord>()
    var nonBlank = false
    for (raw in text.split('\n')) {
        if (newestFirst.size >= HISTORY_LIMIT) break
        val line = raw.trim('\r', ' ', '\t')
        if (line.isEmpty()) continue
        nonBlank = true
        val row = historyLine(line) ?: continue
        newestFirst += row
    }
    if (newestFirst.isEmpty()) {
        return if (nonBlank) null else emptyList()
    }
    return newestFirst
}

private fun historyLine(line: String): HistoryRecord? {
    val fields = line.split('|')
    if (fields.size < 7) return null
    val tokens = fields[3].trim().toIntOrNull() ?: return null
    val cost = fields[4].trim().toDoubleOrNull() ?: return null
    val at = epochMillis(fields[0].trim(), seconds = false) ?: return null
    val known = tokens.takeIf { it > 0 }
    return HistoryRecord(
        atEpochMs = at,
        actionName = jsonUnesc(fields[1]),
        model = jsonUnesc(fields[2]),
        promptTokens = known,
        completionTokens = if (known == null) null else 0,
        cost = cost.takeIf { it >= 0.0 },
        source = jsonUnesc(fields[5]),
        result = jsonUnesc(fields[6]),
    )
}

/** An empty file clears the log; a file with no readable rows leaves it alone. */
private fun parseUsage(text: String): List<UsageRecord>? {
    val out = mutableListOf<UsageRecord>()
    var nonBlank = false
    for (raw in text.split('\n')) {
        val line = raw.trim('\r', ' ', '\t')
        if (line.isEmpty()) continue
        nonBlank = true
        val row = usageLine(line) ?: continue
        out += row
    }
    if (out.isEmpty()) {
        return if (nonBlank) null else emptyList()
    }
    return out
}

private fun usageLine(line: String): UsageRecord? {
    val fields = line.split('|')
    if (fields.size < 5) return null
    val prompt = fields[3].trim().toIntOrNull() ?: return null
    val completion = fields[4].trim().toIntOrNull() ?: return null
    val at = epochMillis(fields[0].trim(), seconds = true) ?: return null
    return UsageRecord(at, jsonUnesc(fields[1]), jsonUnesc(fields[2]), prompt, completion)
}

private fun epochMillis(text: String, seconds: Boolean): Long? {
    val formatter = if (seconds) secondStamp else minuteStamp
    val parsed = runCatching { LocalDateTime.parse(text, formatter) }.getOrNull() ?: return null
    return parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun splitList(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(',', '\n', '\r').map { it.trim() }.filter { it.isNotEmpty() }
}

private fun emptyDash(value: String): String = if (value == "-") "" else value

private fun readIni(file: File): Map<String, String>? {
    val text = readUtf8(file) ?: return null
    val out = linkedMapOf<String, String>()
    var section = ""
    for (raw in text.split('\n')) {
        val line = raw.trim().trimStart('\uFEFF')
        if (line.isEmpty() || line.startsWith(";")) continue
        if (line.startsWith("[") && line.endsWith("]")) {
            section = line.substring(1, line.length - 1).trim()
            continue
        }
        val eq = line.indexOf('=')
        if (eq < 1) continue
        val key = line.substring(0, eq).trim()
        if (key.isEmpty()) continue
        out["$section.${key}"] = line.substring(eq + 1).trim()
    }
    return out
}

private fun readUtf8(file: File): String? {
    if (!file.isFile) return null
    val bytes = file.readBytes()
    if (!isUtf8(bytes)) return null
    val text = bytes.toString(Charsets.UTF_8)
    return if (text.startsWith("\uFEFF")) text.substring(1) else text
}

private fun isUtf8(bytes: ByteArray): Boolean {
    var index = 0
    while (index < bytes.size) {
        val lead = bytes[index].toInt() and 0xFF
        val width = when (lead) {
            in 0x00..0x7F -> 1
            in 0xC2..0xDF -> 2
            in 0xE0..0xEF -> 3
            in 0xF0..0xF4 -> 4
            else -> return false
        }
        if (index + width > bytes.size) return false
        for (offset in 1 until width) {
            val cont = bytes[index + offset].toInt() and 0xFF
            if (cont !in 0x80..0xBF) return false
        }
        index += width
    }
    return true
}

/** JsonUnesc from the AHK client: `\\`, `\"`, `\n`, `\r`, `\t`, and `\uXXXX`, including surrogate pairs. */
private fun jsonUnesc(raw: String): String {
    if (!raw.contains('\\')) return raw
    val out = StringBuilder()
    var index = 0
    while (index < raw.length) {
        if (raw[index] != '\\') {
            out.append(raw[index])
            index += 1
            continue
        }
        if (index + 1 >= raw.length) {
            out.append('\\')
            break
        }
        when (val mark = raw[index + 1]) {
            'n' -> {
                out.append('\n')
                index += 2
            }
            'r' -> {
                out.append('\r')
                index += 2
            }
            't' -> {
                out.append('\t')
                index += 2
            }
            'b' -> {
                out.append('\b')
                index += 2
            }
            'f' -> {
                out.append('\u000C')
                index += 2
            }
            'u' -> index = appendUnicode(raw, index, out)
            else -> {
                out.append(mark)
                index += 2
            }
        }
    }
    return out.toString()
}

private fun appendUnicode(raw: String, index: Int, out: StringBuilder): Int {
    if (index + 6 > raw.length || !isHex4(raw.substring(index + 2, index + 6))) {
        out.append("\\u")
        return index + 2
    }
    val point = raw.substring(index + 2, index + 6).toInt(16)
    val trail = highSurrogateTrail(raw, index, point)
    if (trail != null) {
        val full = 0x10000 + ((point - 0xD800) shl 10) + (trail - 0xDC00)
        out.append(Character.toChars(full))
        return index + 12
    }
    out.append(point.toChar())
    return index + 6
}

private fun highSurrogateTrail(raw: String, index: Int, point: Int): Int? {
    if (point !in 0xD800..0xDBFF || index + 12 > raw.length) return null
    if (raw[index + 6] != '\\' || raw[index + 7] != 'u') return null
    val trailText = raw.substring(index + 8, index + 12)
    if (!isHex4(trailText)) return null
    return trailText.toInt(16).takeIf { it in 0xDC00..0xDFFF }
}

private fun isHex4(text: String): Boolean =
    text.length == 4 && text.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

private fun copyFile(src: File, dest: File, root: File) {
    if (!src.isFile || isWithin(root, dest)) return
    dest.parentFile?.mkdirs()
    src.copyTo(dest, overwrite = true)
}

private fun isWithin(parent: File, child: File): Boolean {
    val base = parent.absoluteFile.toPath().normalize()
    val path = child.absoluteFile.toPath().normalize()
    return path.startsWith(base)
}
