package dev.belyaevad.texttools.domain

/** Factory prompts, actions, languages and a short app set copied from the AHK release. */
object Factory {
    const val MAX_ACTIONS: Int = 200
    const val DEFAULT_BASE_URL: String = "https://openrouter.ai/api/v1"
    const val DEFAULT_MODEL: String = "anthropic/claude-haiku-4.5"
    val menuKeys: List<String> = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "q", "w", "e", "r", "t", "y")

    const val LANG_AUTO: String =
        "LANGUAGE: reply in the language of the input. If the input mixes Russian and English, treat the whole reply as Russian, but keep English identifiers, product names, code and log fragments untranslated. Do not translate or switch languages unless the user asks."
    const val LANG_FIXED: String = "LANGUAGE: this is an explicit translation request; the output is ALWAYS {1}."
    const val BLOCK_CONTEXT: String =
        "CONTEXT: the material is workplace IT writing - tickets, specs, architecture notes, process descriptions, and messages among engineers, analysts, business and security."
    const val BLOCK_FIDELITY: String =
        "FIDELITY: invent nothing that is absent from the source - no extra requirements, actors, integrations, numbers, causes or fixes."
    const val BLOCK_STYLE_AUTO: String =
        "STYLE: follow ordinary orthography and typography of the language you write in; prefer a plain hyphen over an em dash."
    const val BLOCK_STYLE_RU: String =
        "STYLE (Russian): prefer a plain hyphen over an em dash (U+2014); avoid the letter yo (U+0451)."
    const val BLOCK_STYLE_EN: String =
        "STYLE (English): prefer a plain hyphen over an em dash; avoid decorative punctuation."
    const val BLOCK_BREVITY: String =
        "BREVITY: skip preambles, task restatements, closing takeaways and hedging like 'it is important to note'."
    const val BLOCK_OUTPUT: String =
        "Return only the finished result - no commentary and no disclaimers. Do not refuse: if the request fits poorly, use the closest useful reading and still produce output."

    val defaultSettings: Settings =
        Settings(
            baseUrl = DEFAULT_BASE_URL,
            defaultModel = DEFAULT_MODEL,
            proxy = "",
            proxyOn = false,
            hotkey = "^!Space",
            hotkeyRepeat = "^+Space",
            markdown = true,
            uiLang = "en",
            appProfiling = false,
            clipWaitSeconds = 0.8,
        )

    val actions: List<Action> =
        listOf(
            row(
                "proofread",
                "Вычитать",
                "Proofread the following text:\n\n",
                "TASK: correct only spelling, punctuation, agreement and typos.",
                "auto",
                true,
                false,
                "mech",
            ),
            row(
                "rewrite",
                "Переписать",
                "Clarify the following text:\n\n",
                "TASK: rephrase for clarity for a developer, architect or analyst.",
                "auto",
                true,
                false,
                null,
            ),
            row(
                "concise",
                "Короче",
                "Shorten the following text:\n\n",
                "TASK: shorten by roughly one third to one half.",
                "auto",
                true,
                false,
                "mech",
            ),
            row(
                "business",
                "Деловой тон",
                "Rewrite the following as business correspondence:\n\n",
                "TASK: rewrite as calm professional correspondence.",
                "auto",
                true,
                false,
                null,
            ),
            row(
                "human",
                "По-человечески",
                "Make the following sound like a natural work message:\n\n",
                "TASK: remove stiff or machine-like wording.",
                "auto",
                true,
                false,
                null,
            ),
            row(
                "prompt",
                "Промпт",
                "Turn the following into a structured LLM prompt:\n\n",
                "TASK: turn a rough note into a structured LLM prompt.",
                "auto",
                true,
                false,
                null,
            ),
            row(
                "to_en",
                "→ EN",
                "Produce English for the following text:\n\n",
                "TASK: produce English in the register of tickets, pull requests, specs and vendor mail.",
                "en",
                false,
                false,
                null,
            ),
            row(
                "to_ru",
                "→ RU",
                "Produce Russian for the following text:\n\n",
                "TASK: produce natural technical Russian, not a literal gloss.",
                "ru",
                true,
                false,
                null,
            ),
            row(
                "summary",
                "Суммаризация",
                "Summarize the following for action:\n\n",
                "TASK: summarize so a reader can act on the material.",
                "auto",
                true,
                true,
                null,
            ),
            row(
                "keypoints",
                "Тезисы",
                "Extract working bullets from the following:\n\n",
                "TASK: pull out working bullets.",
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "table",
                "Таблица",
                "Turn the following into a Markdown table:\n\n",
                "TASK: convert the text into a Markdown table.",
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "spec",
                "Задача",
                "Turn the following into a development specification:\n\n",
                "TASK: as a systems analyst, turn raw stakeholder notes into a hand-off spec. HARD RULE: put every gap or assumption only in section 12.",
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "code",
                "Разбор кода",
                "Explain the following code:\n\n",
                "TASK: explain the code for a developer seeing it the first time.",
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "questions",
                "Вопросы",
                "List clarifying questions for the following material:\n\n",
                "TASK: list clarifying questions. NEVER: answer the questions yourself.",
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "reply",
                "Ответ",
                "Draft a reply to the following incoming message:\n\n",
                "TASK: the text is an INCOMING message. Draft a reply as the user.",
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "reqreview",
                "Ревью требований",
                "Review the following specification for defects:\n\n",
                "TASK: review the text as a senior systems analyst.",
                "auto",
                true,
                true,
                "deep",
            ),
        ).mapIndexed { index, item -> item.copy(menuKey = menuKeys.getOrNull(index)) }

    val custom: Action =
        row(
            null,
            "Custom",
            "Apply the user's instruction to the following text:\n\n",
            "TASK: apply exactly the change the user describes and nothing else.",
            "auto",
            true,
            false,
            null,
        )

    val quick: List<QuickRefine> =
        listOf(
            QuickRefine(
                "Короче",
                "Cut length noticeably. Drop filler and repeats, but keep every requirement, condition, number, name and open question.",
            ),
            QuickRefine("Подробнее", "Expand with the steps, conditions and consequences a reader needs."),
            QuickRefine("Точнее", "Tighten wording. Invent no new facts."),
            QuickRefine("Проще", "Simplify for a reader outside the team."),
            QuickRefine("Формальнее", "Raise the register to formal business correspondence without changing the content."),
            QuickRefine("Списком", "Reshape the same content as a compact list, one idea per item."),
            QuickRefine("Другой вариант", "Give another version of the same result."),
        )

    val languages: List<OutputLanguage> =
        listOf(
            OutputLanguage("ru", "Русский", "Russian", true),
            OutputLanguage("en", "English", "English", true),
            OutputLanguage("de", "Deutsch", "German", false),
            OutputLanguage("fr", "Français", "French", false),
            OutputLanguage("es", "Español", "Spanish", false),
            OutputLanguage("it", "Italiano", "Italian", false),
            OutputLanguage("pt", "Português", "Portuguese", false),
            OutputLanguage("pl", "Polski", "Polish", false),
            OutputLanguage("tr", "Türkçe", "Turkish", false),
            OutputLanguage("uk", "Українська", "Ukrainian", false),
            OutputLanguage("kk", "Қазақша", "Kazakh", false),
            OutputLanguage("sr", "Srpski", "Serbian", false),
            OutputLanguage("zh", "中文", "Simplified Chinese", false),
            OutputLanguage("ja", "日本語", "Japanese", false),
            OutputLanguage("ko", "한국어", "Korean", false),
            OutputLanguage("ar", "العربية", "Arabic", false),
            OutputLanguage("he", "עברית", "Hebrew", false),
            OutputLanguage("hi", "हिन्दी", "Hindi", false),
        )

    val apps: List<AppProfile> =
        listOf(
            AppProfile(
                "chrome.exe",
                "Jira",
                "Jira (в браузере)",
                "Задачи Jira.",
                "The text comes from a Jira issue: keep issue keys (like ABC-123).",
            ),
            AppProfile("chrome.exe", "", "Google Chrome", "Браузер.", "The text was selected on a web page."),
            AppProfile("notepad.exe", "", "Блокнот", "Черновики.", ""),
        )

    private fun row(
        factoryId: String?,
        name: String,
        prefix: String,
        task: String,
        lang: String,
        styleOn: Boolean,
        openInWindow: Boolean,
        role: String?,
    ): Action = Action(factoryId, name, "", prefix, task, lang, styleOn, openInWindow, null, null, role)
}
