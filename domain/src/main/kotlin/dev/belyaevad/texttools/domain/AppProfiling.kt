package dev.belyaevad.texttools.domain

/**
 * Asks the model to look the application up and write the note from that description.
 * Web notes are extra search hits. They count only when they name this executable.
 */
val APP_PROFILE_PROMPT: String =
    """
    You look up a Windows desktop application and then write a prompt note from the description you found.
    Search the web for the executable file name and the window title. Web notes, when present, are search results: use them only when they clearly name this executable. Ignore unrelated hits.

    If you cannot identify this specific application, output exactly one word: UNKNOWN
    Output UNKNOWN also when the executable name is generic, ambiguous, or could belong to several different products. Never guess, never invent a product.

    Otherwise output exactly two lines and nothing else:
    DESC: <one sentence in Russian: what this application is and what kind of text a user would select in it>
    HINT: <one or two sentences in English, addressed to a text-processing assistant, based on that description: register, what must be preserved verbatim, what to avoid>

    The HINT must follow from the DESC. If that description is not specific enough to change how the text is handled, output UNKNOWN instead.
    """.trimIndent()

/** A description and the English prompt note. Either line may be empty, not both. */
data class AppDescription(
    val description: String,
    val hint: String,
)

/** Messages for one profiling call. Blank web notes are left out. A blank title is sent as `(none)`. */
fun profileMessages(
    proc: String,
    title: String,
    webNotes: String,
): List<ChatMessage> {
    val user =
        buildString {
            append("Executable: ")
            append(proc.trim())
            append("\nWindow title: ")
            append(title.trim().ifEmpty { "(none)" })
            val notes = webNotes.trim()
            if (notes.isNotEmpty()) {
                append("\n\nWeb notes:\n")
                append(notes)
            }
        }
    return listOf(ChatMessage("system", APP_PROFILE_PROMPT), ChatMessage("user", user))
}

/** The chosen profiling model, or the default model when the choice is blank. */
fun profilingModel(
    chosen: String,
    fallback: String,
): String = chosen.trim().ifEmpty { fallback.trim() }

/**
 * OpenRouter `:online` makes that model search the web before it answers.
 * Any other host keeps the model id unchanged.
 */
fun searchModel(
    model: String,
    baseUrl: String,
): String {
    val name = model.trim()
    if (name.isEmpty() || !baseUrl.contains("openrouter.ai", ignoreCase = true)) return name
    if (name.endsWith(":online", ignoreCase = true)) return name
    return "$name:online"
}

/**
 * Reads DESC and HINT. UNKNOWN, an empty body, and a body with neither line return null.
 * A leading UNKNOWN keeps the profile neutral.
 */
fun parseAppDescription(text: String): AppDescription? {
    val body = text.trim()
    if (body.isEmpty() || body.startsWith("UNKNOWN")) return null
    var description = ""
    var hint = ""
    body.replace("\r", "").lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("DESC:") -> description = line.removePrefix("DESC:").trim()
            line.startsWith("HINT:") -> hint = line.removePrefix("HINT:").trim()
        }
    }
    if (description.isEmpty() && hint.isEmpty()) return null
    return AppDescription(description, hint)
}

/**
 * Adds a neutral profile when [proc] has no match yet.
 * The stored title stays empty so the row matches every window of that process.
 * The same list comes back when nothing was added.
 */
fun withNeutralApp(
    apps: List<AppProfile>,
    proc: String,
    title: String,
    description: String,
): List<AppProfile> {
    val name = proc.trim()
    if (name.isEmpty() || matchApp(apps, name, title) != null) return apps
    return apps + AppProfile(name, "", name, description, "")
}

/** Writes a received description onto the neutral row for [proc]. Other rows stay. */
fun applyAppDescription(
    apps: List<AppProfile>,
    proc: String,
    found: AppDescription,
): List<AppProfile> {
    val index = apps.indexOfLast { it.proc.equals(proc.trim(), ignoreCase = true) && it.title.isEmpty() }
    if (index < 0) return apps
    val current = apps[index]
    val next =
        current.copy(
            description = found.description.ifEmpty { current.description },
            hint = found.hint,
        )
    return apps.toMutableList().also { it[index] = next }
}
