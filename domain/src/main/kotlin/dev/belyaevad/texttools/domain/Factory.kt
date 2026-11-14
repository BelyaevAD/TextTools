package dev.belyaevad.texttools.domain

/** Factory prompts, actions, languages and app profiles. Action text and menu keys are the tuned local AHK set. */
object Factory {
    const val MAX_ACTIONS: Int = 200
    const val DEFAULT_BASE_URL: String = "https://openrouter.ai/api/v1"
    const val DEFAULT_MODEL: String = "anthropic/claude-haiku-4.5"
    val menuKeys: List<String> = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "q", "w", "e", "r", "t", "y")

    const val LANG_AUTO: String =
        "LANGUAGE: reply in the language of the input. If the input mixes Russian and English, treat the whole reply as Russian, but keep English identifiers, product names, code and log fragments untranslated. Do not translate or switch languages unless the user asks."
    const val LANG_FIXED: String = "LANGUAGE: this is an explicit translation request; the output is ALWAYS {1}."
    const val BLOCK_CONTEXT: String =
        "CONTEXT: the material is workplace IT writing - tickets, specs, architecture notes, process descriptions, and messages among engineers, analysts, business and security. Read it like a systems analyst: who acts, what triggers work, ordered steps, data and fields, integrations, rights, states, edge cases and non-functional limits. Preserve meaningful labels (systems, entities, fields, roles, statuses) and every number, deadline, condition and exception. Keep business wording business-like and technical wording technical; do not assume a particular stack."
    const val BLOCK_FIDELITY: String =
        "FIDELITY: invent nothing that is absent from the source - no extra requirements, actors, integrations, numbers, causes or fixes. Preserve must / should / may and the as-is versus to-be distinction. Leave hypotheses, proposals and questions labeled as such. Keep genuine ambiguity; do not silently merge inconsistent terms. Name the actor when the source does."
    const val BLOCK_STYLE_AUTO: String =
        "STYLE: follow ordinary orthography and typography of the language you write in; prefer a plain hyphen over an em dash; do not end paragraphs with wrap-up slogans; avoid bureaucratic filler."
    const val BLOCK_STYLE_RU: String =
        "STYLE (Russian): prefer a plain hyphen over an em dash (U+2014); avoid the letter yo (U+0451); do not end paragraphs with wrap-up slogans; avoid bureaucratic filler."
    const val BLOCK_STYLE_EN: String =
        "STYLE (English): prefer a plain hyphen over an em dash; avoid decorative punctuation; do not end paragraphs with wrap-up slogans; avoid corporate fluff."
    const val BLOCK_BREVITY: String =
        "BREVITY: skip preambles, task restatements, closing takeaways and hedging like 'it is important to note'. Drop meta-remarks about the text. Every sentence must carry a source fact; delete framing or repetition. Use lists only for real enumerations. Prefer shorter wording when no fact is lost."
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
            sentryErrors = false,
            autostart = true,
            profileModel = "",
        )

    val actions: List<Action> =
        listOf(
            Row(
                "prompt",
                "Prompt",
                "🤖",
                "Turn this into a well-structured prompt:\n\n",
                """
                TASK: turn a rough note into a structured prompt for a language model: role and context, task, input data (only if present), requirements and constraints, output format.
                KEEP: every concrete detail - numbers, names, systems, limits. Make implied expectations explicit (audience, depth, answer language) without inventing new requirements or subject matter.
                FORM: short headings or blank lines between blocks. The prompt stays in the language of the note. No surrounding backticks.
                """.trimIndent(),
                "auto",
                true,
                false,
                null,
                "1",
            ),
            Row(
                "proofread",
                "Proofread",
                "✏",
                "Make this change to the following text:\n\n",
                """
                TASK: proofread only - spelling, punctuation, agreement, typos.
                KEEP: the author's structure, tone, wording and formatting. Substance stays untouched: a questionable requirement or a misused term stays as written. Terms, identifiers, paths, commands, field names, SQL, code and log lines byte-for-byte, case included. Markdown, line breaks and numbering intact.
                """.trimIndent(),
                "auto",
                true,
                false,
                "mech",
                "6",
            ),
            Row(
                "rewrite",
                "Improve",
                "🧬",
                "Rewrite this text:\n\n",
                """
                TASK: rewrite for clarity and structure; the reader is a developer, architect or analyst.

                FORMAT:
                - Use clear paragraphs and lists where appropriate.
                - Keep one thought per sentence.
                - State the actor and action explicitly.
                - Follow a logical order without repetition.
                - Pull conditions and exceptions out of long sentences so they cannot be missed.

                KEEP: meaning, facts, key terms, stylistic tone, and the shape of the argument — context, problem, proposal, consequence.

                NEVER: add new examples, justifications or conclusions.
                """.trimIndent(),
                "auto",
                true,
                false,
                null,
                "2",
            ),
            Row(
                "concise",
                "Shorter",
                "✂",
                "Make this text more concise:\n\n",
                """
                TASK: compress by 30-50 percent. Cut filler, rhetoric, padding and restatements.
                KEEP: requirements, conditions, exceptions, edge cases, numbers, deadlines, names, responsibilities, risks and open questions. if / then constructions stay in full even when long.
                """.trimIndent(),
                "auto",
                true,
                false,
                "mech",
                "3",
            ),
            Row(
                "human",
                "Plain language",
                "🙂",
                "Make this text sound human:\n\n",
                """
                TASK: strip the machine-generated feel and officialese; the result must pass as written by a competent person in a work chat with engineers.
                FORM: varied sentence length, ordinary connectors, direct statements. No stock phrases, no parallel constructions, no lists where prose is enough. Conversational connectors and text smileys in brackets are fine.
                KEEP: facts, numbers, terms, conditions and names exactly.
                """.trimIndent(),
                "auto",
                true,
                false,
                null,
                "5",
            ),
            Row(
                "to_en",
                "→ EN",
                "🌐",
                "Translate this into English:\n\n",
                """
                TASK: translate into the register of issues, pull requests, specs and vendor correspondence. Standard IT terminology, no calques from Russian, no inflated wording. If the input is already English, proofread it instead.
                KEEP: identifiers, paths, commands, product names, field names and code untranslated. Markdown, lists and code blocks intact. must / should / may stay distinct.
                """.trimIndent(),
                "en",
                false,
                false,
                null,
                "7",
            ),
            Row(
                "to_ru",
                "→ RU",
                "🌐",
                "Translate this into Russian:\n\n",
                """
                TASK: translate into natural technical Russian, not word for word. Established English IT terms stay in Latin script (deploy, pull request, endpoint, middleware, webhook) instead of awkward calques. If the input is already Russian, proofread it instead.
                KEEP: identifiers, paths, commands, product names, field names and code untranslated. Markdown and code blocks intact. must / should / may stay distinct.
                """.trimIndent(),
                "ru",
                true,
                false,
                null,
                "8",
            ),
            Row(
                "summary",
                "Summary",
                "📝",
                "Summarize this:\n\n",
                """
                TASK: summarize the material for someone who has to act on it.
                KEEP: who, what, by when and under which conditions; every number and limit; the causal chain if there is one; risks, dependencies, environment and security constraints. What is decided, what is merely proposed and what is still open stay clearly apart.
                FORM: bold for key entities, short lists only for real lists, small headings only for long inputs.
                """.trimIndent(),
                "auto",
                true,
                true,
                null,
                "9",
            ),
            Row(
                "keypoints",
                "Key points",
                "📋",
                "Extract key points from this:\n\n",
                """
                TASK: extract the key points as a working checklist - one idea per bullet, at most two levels, each bullet readable without the original.
                FORM: group under the headings that actually have content: Facts / current state, Requirements, Decisions, Risks and constraints, Dependencies, Who does what and by when, Open questions.
                KEEP: numbers, deadlines, names, statuses and conditions verbatim.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
                "0",
            ),
            Row(
                "table",
                "Table",
                "▦",
                "Convert this into a table:\n\n",
                """
                TASK: turn the text into a Markdown table. First recognise the structure it actually holds, then pick the columns:
                options - option per row, criterion per column; requirements - id, requirement, priority, source, acceptance criterion; mapping - source field, target field, rule, notes; process - step, actor, trigger, result, system; action items - item, owner, deadline, status. Otherwise derive the columns from the text itself.
                KEEP: values exactly as written - no rewording, no normalising. Field names and identifiers unchanged. An empty cell gets a hyphen.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
                "q",
            ),
            Row(
                "spec",
                "Task",
                "📄",
                "Turn this into a development task specification:\n\n",
                """
                TASK: as a systems analyst, turn raw stakeholder material into a specification a developer can pick up. Use these sections, skipping every one the source says nothing about:
                1. Context (as-is) - how it works today, who is involved, what the pain is.
                2. Goal - the business outcome, one or two sentences.
                3. Scope (to-be) - required behaviour, not implementation.
                4. Out of scope - only if stated.
                5. Actors and permissions.
                6. Trigger and flow - starting event, ordered steps, branches.
                7. Data - entities, fields, source of truth, states and transitions.
                8. Integrations and dependencies - systems, direction of exchange, blockers.
                9. Acceptance criteria - numbered, if / then, testable by someone who missed the discussion.
                10. Non-functional - volumes, timing, security and access, environment limits.
                11. Edge cases and errors - only those raised by the source or implied by its own rules.
                12. Questions for the stakeholder - concrete questions on everything that blocks starting.
                HARD RULE: every gap or assumption goes into section 12, never into the body. Do not pick an implementation the source has not fixed. Keep the priority the source gives, assign none if it gives none.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
                "w",
            ),
            Row(
                "code",
                "Code explanation",
                "🔍",
                "Explain what this code does:\n\n",
                """
                TASK: explain what the code does to a developer seeing it for the first time. One or two sentences on its purpose, then the meaningful parts: inputs and outputs, main flow, side effects (I/O, network, global or shared state), error handling, edge cases and pitfalls.
                KEEP: identifiers, functions and paths exactly as written. Do not rewrite or improve the code; point at a problem only where the explanation requires it. Where behaviour depends on context that is missing (callers, schemas, config), say so instead of guessing.
                FORM: prose first, short lists only where they genuinely help.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
                "e",
            ),
            Row(
                "questions",
                "Questions",
                "❓",
                "List clarifying questions for this material:\n\n",
                """
                TASK: as a systems analyst receiving raw material, produce the clarifying questions that must be answered before the work can move forward: ambiguities, contradictions, missing scenarios (errors, permissions, migrations, edge cases), unstated assumptions, absent non-functional constraints (volumes, timing, security, access).
                FORM: group by addressee, using only the groups that have content, with these exact headings: К бизнесу / К разработке / К безопасности. Numbered list inside a group; every question self-contained, specific and ready to be sent as is.
                NEVER: answer them yourself, ask what the text already answers, or pad the list - only questions that actually block or change the work.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
                "r",
            ),
            Row(
                "reply",
                "Reply",
                "✉",
                "Draft a reply to this incoming message:\n\n",
                """
                TASK: the text is an INCOMING message (email or work chat) addressed to the user. Draft a reply on the user's behalf. If the user's instruction states the intent, write ONE reply following it; otherwise write 2-3 labeled variants covering the plausible intents: agreement / confirmation, clarification with questions, polite pushback with reasoning.
                KEEP: the register and tone of the incoming message. Use only facts from the message or from the user's instruction; where a needed fact is missing (dates, names, numbers), insert a bracketed placeholder like [указать срок].
                FORM: ready to send - no meta-comments, nothing around a variant beyond a short label.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
                "t",
            ),
            Row(
                "reqreview",
                "Requirements review",
                "🧐",
                "Review this specification for defects:\n\n",
                """
                TASK: review the text as a senior systems analyst checking a specification before it goes to development. Find real defects: wording that can be read in more than one way; internal contradictions; untestable or unmeasurable statements; missing error and edge-case handling; undefined data, statuses, roles or permissions; missing non-functional requirements (volumes, timing, security, access); hidden assumptions presented as facts.
                FORM: a Markdown table with columns # | Фрагмент (короткая цитата) | Проблема | Серьезность (критично / важно / мелочь) | Что уточнить или как исправить.
                Report every issue found, including uncertain ones - the severity column does the filtering. After the table, one short paragraph: is the text ready to hand over to development.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
                "y",
            ),
        ).map { it.toAction() }

    val custom: Action =
        Row(
            null,
            "Custom",
            "",
            "Apply the user's instruction to the following text:\n\n",
            """
            TASK: apply exactly the change the user describes and nothing else. An explicit user instruction overrides the rules above, but facts absent from the source still must not be invented.
            KEEP: terms, identifiers, paths, commands and code untranslated unless asked. Keep Markdown and code fences. Do not comment on the content.
            """.trimIndent(),
            "auto",
            true,
            false,
            null,
        ).toAction()

    val quick: List<QuickRefine> =
        listOf(
            QuickRefine(
                "Shorter",
                "Make it noticeably shorter. Drop filler and repetition, but keep every requirement, condition, number, name and open question.",
            ),
            QuickRefine(
                "More detail",
                "Expand it: spell out the steps, conditions and consequences the reader needs. Add no facts that are absent from the source material - if something is unknown, say so explicitly.",
            ),
            QuickRefine(
                "More precise",
                "Make the wording more precise: replace vague words with exact terms, name the actor and the condition in every statement, remove ambiguity. Do not add new facts.",
            ),
            QuickRefine(
                "Simpler",
                "Simplify the language for a reader outside the team: shorter sentences, no jargon where a plain word works. Keep all facts, numbers and technical terms that carry meaning.",
            ),
            QuickRefine(
                "More formal",
                "Rewrite in a more formal business register suitable for official correspondence, keeping the content unchanged.",
            ),
            QuickRefine(
                "As a list",
                "Restructure the same content as a compact list, one idea per item. Do not add or drop anything.",
            ),
            QuickRefine(
                "Another version",
                "Produce a different version of the same result: another structure and other wording, same facts and same task. Do not repeat the previous phrasing.",
            ),
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

    private const val CHROME_EXE = "chrome.exe"
    private const val IDE_SOURCE =
        "The text comes from an IDE: treat it as source code, never translate identifiers, and keep formatting and indentation byte-for-byte."
    private const val SQL_SOURCE =
        "The text comes from a database tool: it is SQL or query output, so never change table, column or identifier names."

    val apps: List<AppProfile> =
        listOf(
            AppProfile(
                CHROME_EXE,
                "Jira",
                "Jira (в браузере)",
                "Задачи и требования в Jira: описания, комментарии, критерии приёмки.",
                "The text comes from a Jira issue: keep issue keys (like ABC-123), status names and field labels exactly as written.",
            ),
            AppProfile(
                CHROME_EXE,
                "Confluence",
                "Confluence (в браузере)",
                "Страницы вики: спецификации, регламенты, протоколы встреч.",
                "The text comes from a Confluence page: preserve headings, lists and table structure so the result can be pasted back as a document fragment.",
            ),
            AppProfile(
                CHROME_EXE,
                "Битрикс24",
                "Битрикс24 (в браузере)",
                "Портал Битрикс24: задачи, CRM, комментарии, бизнес-процессы.",
                "The text comes from Bitrix24: keep entity names, field labels and workflow status names exactly as written, they are part of the system's configuration.",
            ),
            AppProfile(
                CHROME_EXE,
                "",
                "Google Chrome",
                "Браузер: веб-страницы, документация, почта, внутренние порталы.",
                "The text was selected on a web page and may contain stray interface labels or navigation fragments; ignore them and work only with the meaningful content.",
            ),
            AppProfile(
                "Cursor.exe",
                "",
                "Cursor",
                "Редактор кода: исходники проекта, конфиги, Markdown-документация.",
                "The text comes from a code editor: treat it as source code or a technical file, never translate identifiers, and keep formatting and indentation byte-for-byte.",
            ),
            AppProfile(
                "idea64.exe",
                "",
                "IntelliJ IDEA",
                "IDE: код, конфигурация, комментарии.",
                IDE_SOURCE,
            ),
            AppProfile(
                "phpstorm64.exe",
                "",
                "PhpStorm",
                "IDE для PHP: код Bitrix24-проекта, конфиги.",
                IDE_SOURCE,
            ),
            AppProfile(
                "pycharm64.exe",
                "",
                "PyCharm",
                "IDE для Python: скрипты, конфиги.",
                IDE_SOURCE,
            ),
            AppProfile(
                "OUTLOOK.EXE",
                "",
                "Outlook",
                "Рабочая почта: письма коллегам, бизнесу, вендорам.",
                "The text is business email: keep the formal register, and leave greetings, sign-offs and addressee names in place unless asked otherwise.",
            ),
            AppProfile(
                "Telegram.exe",
                "",
                "Telegram",
                "Рабочие и личные чаты, обсуждения задач.",
                "The text comes from a work chat: keep it short and direct in an informal but professional register, without letter-style greetings.",
            ),
            AppProfile(
                "claude.exe",
                "",
                "Claude (десктоп)",
                "Диалог с ИИ-ассистентом: запросы и его ответы.",
                "The text comes from a chat with an AI assistant and may already be model output; do not add meta-commentary about the conversation itself.",
            ),
            AppProfile(
                "WINWORD.EXE",
                "",
                "Microsoft Word",
                "Документы: регламенты, спецификации, договоры.",
                "The text comes from a Word document: preserve paragraph structure, lists and numbering so the result can replace the fragment in place.",
            ),
            AppProfile(
                "EXCEL.EXE",
                "",
                "Microsoft Excel",
                "Таблицы: реестры, выгрузки, расчёты.",
                "The text comes from spreadsheet cells separated by tabs: keep the column structure and never alter numbers, dates or codes.",
            ),
            AppProfile(
                "datagrip64.exe",
                "",
                "DataGrip",
                "SQL-клиент: запросы, схемы, результаты выборок.",
                SQL_SOURCE,
            ),
            AppProfile(
                "dbeaver.exe",
                "",
                "DBeaver",
                "SQL-клиент: запросы, схемы, результаты выборок.",
                SQL_SOURCE,
            ),
            AppProfile(
                "heidisql.exe",
                "",
                "HeidiSQL",
                "SQL-клиент для MySQL/MariaDB.",
                SQL_SOURCE,
            ),
            AppProfile(
                "Postman.exe",
                "",
                "Postman",
                "API-клиент: запросы, заголовки, JSON-ответы.",
                "The text comes from an API client: it is JSON, headers or endpoint definitions, so keep the structure syntactically valid and field names untouched.",
            ),
            AppProfile(
                "Insomnia.exe",
                "",
                "Insomnia",
                "API-клиент: запросы и ответы.",
                "The text comes from an API client: it is JSON, headers or endpoint definitions, so keep the structure syntactically valid and field names untouched.",
            ),
            AppProfile(
                "WindowsTerminal.exe",
                "",
                "Windows Terminal",
                "Консоль: команды, вывод, логи.",
                "The text comes from a terminal: it is commands, log lines or program output, so keep commands, paths and log lines byte-for-byte.",
            ),
            AppProfile(
                "putty.exe",
                "",
                "PuTTY",
                "SSH-сессия: вывод команд на серверах.",
                "The text comes from a terminal: it is commands, log lines or program output, so keep commands, paths and log lines byte-for-byte.",
            ),
            AppProfile(
                "notepad.exe",
                "",
                "Блокнот",
                "Черновики и временные заметки.",
                "",
            ),
            AppProfile(
                "notepad++.exe",
                "",
                "Notepad++",
                "Текстовый редактор: черновики, логи, конфиги.",
                "",
            ),
            AppProfile(
                "drawio.exe",
                "",
                "draw.io",
                "Редактор схем: подписи узлов и связей.",
                "The text comes from a diagram editor: these are short node and edge labels, so keep them terse and do not turn them into sentences.",
            ),
        )

    private data class Row(
        val factoryId: String?,
        val name: String,
        val icon: String,
        val prefix: String,
        val task: String,
        val lang: String,
        val styleOn: Boolean,
        val openInWindow: Boolean,
        val role: String?,
        val menuKey: String? = null,
    )

    private fun Row.toAction(): Action = Action(factoryId, name, icon, prefix, task, lang, styleOn, openInWindow, menuKey, null, role)
}
