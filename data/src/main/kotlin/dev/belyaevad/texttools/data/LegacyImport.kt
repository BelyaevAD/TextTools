package dev.belyaevad.texttools.data

import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.Factory
import java.io.File

/** Reads an AHK state folder. The key and the proxy URL are returned, not written to SQLite. */
class LegacyImport(private val store: TextToolsStore) {
    /** Result of one import. Secrets are only in this object. */
    data class Result(val apiKey: String, val proxyUrl: String)

    /** Imports actions. Does not modify the source folder. */
    fun importFolder(root: File): Result {
        val actions = parseActions(readIni(File(root, "actions.ini")))
        if (actions.isNotEmpty()) {
            store.saveActions(actions)
        }
        val settings = readIni(File(root, "TextTools.ini"))
        return Result(
            apiKey = settings["api.api_key"].orEmpty(),
            proxyUrl = settings["api.proxy"].orEmpty(),
        )
    }

    private fun parseActions(ini: Map<String, String>): List<Action> {
        val out = mutableListOf<Action>()
        for (index in 1..Factory.MAX_ACTIONS) {
            val name = ini["action$index.name"] ?: break
            if (name.isEmpty()) break
            val task = ini["action$index.task"].orEmpty()
            if (task.isEmpty()) continue
            val rawId = ini["action$index.id"].orEmpty()
            val rawKey = ini["action$index.key"] ?: Factory.menuKeys.getOrElse(index - 1) { "" }
            val rawModel = ini["action$index.model"].orEmpty()
            out += Action(
                factoryId = rawId.takeUnless { it.isEmpty() || it == "-" },
                name = name,
                icon = ini["action$index.icon"].orEmpty(),
                prefix = ini["action$index.prefix"].orEmpty(),
                task = task,
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

    private fun readIni(file: File): Map<String, String> {
        if (!file.isFile) return emptyMap()
        val out = linkedMapOf<String, String>()
        var section = ""
        file.readLines().forEach { raw ->
            val line = raw.trim().trimStart('\uFEFF')
            if (line.isEmpty() || line.startsWith(";")) return@forEach
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length - 1)
                return@forEach
            }
            val eq = line.indexOf('=')
            if (eq < 1) return@forEach
            out["$section.${line.substring(0, eq).trim()}"] = line.substring(eq + 1).trim()
        }
        return out
    }
}
