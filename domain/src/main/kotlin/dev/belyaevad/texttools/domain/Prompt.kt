package dev.belyaevad.texttools.domain

/** Builds the system prompt. Brevity always follows the task block. */
fun buildInstruction(
    action: Action,
    languages: List<OutputLanguage>,
    sourceHint: String,
): String {
    val parts =
        buildList {
            add(languageBlock(action.lang, languages))
            add(Factory.BLOCK_CONTEXT)
            if (sourceHint.isNotEmpty()) {
                add("SOURCE APPLICATION: $sourceHint")
            }
            add(Factory.BLOCK_FIDELITY)
            if (action.styleOn) {
                add(styleBlock(action.lang))
            }
            add(action.task.trim())
            add(Factory.BLOCK_BREVITY)
            add(Factory.BLOCK_OUTPUT)
        }
    return parts.joinToString("\n\n")
}

/** User message: the action prefix, the selection, then an optional extra instruction. */
fun buildUserContent(
    action: Action,
    selection: String,
    customText: String,
): String {
    val base = action.prefix + selection
    if (customText.isEmpty()) {
        return base
    }
    val label =
        if (action.name == "Custom") {
            "The change to make: "
        } else {
            "Additional instruction from the user (follow it within the task above): "
        }
    return "$base\n\n$label$customText"
}

private fun languageBlock(
    lang: String,
    languages: List<OutputLanguage>,
): String {
    if (lang.isEmpty() || lang == "auto") {
        return Factory.LANG_AUTO
    }
    val english = languages.firstOrNull { it.code == lang }?.prompt ?: lang
    return Factory.LANG_FIXED.replace("{1}", english)
}

private fun styleBlock(lang: String): String =
    when (lang) {
        "ru" -> Factory.BLOCK_STYLE_RU
        "en" -> Factory.BLOCK_STYLE_EN
        else -> Factory.BLOCK_STYLE_AUTO
    }
