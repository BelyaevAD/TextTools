package dev.belyaevad.texttools.domain

/** Factory prompts, actions, languages and app profiles copied from the AHK release. */
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
        )

    val actions: List<Action> =
        listOf(
            row(
                "proofread",
                "Вычитать",
                "✏",
                "Proofread the following text:\n\n",
                """
                TASK: correct only spelling, punctuation, agreement and typos.
                KEEP: structure, tone, wording and layout from the author. Do not change substance - a dubious requirement or odd term stays. Leave terms, identifiers, paths, commands, field names, SQL, code and log lines unchanged, including case. Preserve Markdown, line breaks and numbering.
                """.trimIndent(),
                "auto",
                true,
                false,
                "mech",
            ),
            row(
                "rewrite",
                "Переписать",
                "🔄",
                "Clarify the following text:\n\n",
                """
                TASK: rephrase for clarity for a developer, architect or analyst. One idea per sentence, clear actor and verb, logical order, no duplication. Lift conditions and exceptions out of dense sentences so they stay visible.
                KEEP: meaning, facts and the argument shape (context, problem, proposal, effect).
                NEVER: add examples, justifications or conclusions that were not in the source.
                """.trimIndent(),
                "auto",
                true,
                false,
                null,
            ),
            row(
                "concise",
                "Короче",
                "✂",
                "Shorten the following text:\n\n",
                """
                TASK: shorten by roughly one third to one half. Remove padding, rhetoric and repeated statements.
                KEEP: requirements, conditions, exceptions, edge cases, numbers, deadlines, names, owners, risks and open questions. Keep if/then constructions complete even when long.
                """.trimIndent(),
                "auto",
                true,
                false,
                "mech",
            ),
            row(
                "business",
                "Деловой тон",
                "💼",
                "Rewrite the following as business correspondence:\n\n",
                """
                TASK: rewrite as calm professional correspondence for a manager, peer team, architect or security contact. No emotion, no blame, no hedging that hides the ask.
                FORM: situation; then the request or proposal; then impact and cost of inaction; then what you need from them and by when. Omit any part the source does not support.
                KEEP: deadlines, system names, constraints, risks and dependencies. Keep technical terms; drop slang.
                """.trimIndent(),
                "auto",
                true,
                false,
                null,
            ),
            row(
                "human",
                "По-человечески",
                "🙂",
                "Make the following sound like a natural work message:\n\n",
                """
                TASK: remove stiff or machine-like wording so the text reads like a competent engineer writing in a work chat.
                FORM: mix sentence length, use ordinary connectors, state things directly. Avoid cliches, forced parallelism and bullet lists when prose is enough. Light conversational markers and smileys in parentheses are fine.
                KEEP: facts, numbers, terms, conditions and names unchanged.
                """.trimIndent(),
                "auto",
                true,
                false,
                null,
            ),
            row(
                "prompt",
                "Промпт",
                "🤖",
                "Turn the following into a structured LLM prompt:\n\n",
                """
                TASK: turn a rough note into a structured LLM prompt: role and context, task, input data (only if present), requirements and constraints, output format.
                KEEP: every concrete detail - numbers, names, systems, limits. Make implied expectations explicit (audience, depth, answer language) without inventing new subject matter.
                FORM: short headings or blank lines between blocks. Keep the prompt in the note's language. No wrapping backticks around the whole prompt.
                """.trimIndent(),
                "auto",
                true,
                false,
                null,
            ),
            row(
                "to_en",
                "→ EN",
                "🌐",
                "Produce English for the following text:\n\n",
                """
                TASK: produce English in the register of tickets, pull requests, specs and vendor mail. Use standard IT wording; avoid calques from Russian and inflated phrasing. If the input is already English, proofread instead of translating.
                KEEP: identifiers, paths, commands, product names, field names and code untranslated. Keep Markdown, lists and code fences. Distinguish must / should / may.
                """.trimIndent(),
                "en",
                false,
                false,
                null,
            ),
            row(
                "to_ru",
                "→ RU",
                "🌐",
                "Produce Russian for the following text:\n\n",
                """
                TASK: produce natural technical Russian, not a literal gloss. Keep established English IT terms in Latin script (deploy, pull request, endpoint, middleware, webhook) rather than awkward calques. If the input is already Russian, proofread instead of translating.
                KEEP: identifiers, paths, commands, product names, field names and code untranslated. Keep Markdown and code fences. Distinguish must / should / may.
                """.trimIndent(),
                "ru",
                true,
                false,
                null,
            ),
            row(
                "summary",
                "Суммаризация",
                "📝",
                "Summarize the following for action:\n\n",
                """
                TASK: summarize so a reader can act on the material.
                KEEP: who, what, by when and under which conditions; every number and limit; causal links when present; risks, dependencies, environment and security constraints. Separate decided items, proposals and open points.
                FORM: bold key entities; short lists only for true lists; small headings only for long inputs.
                """.trimIndent(),
                "auto",
                true,
                true,
                null,
            ),
            row(
                "keypoints",
                "Тезисы",
                "📋",
                "Extract working bullets from the following:\n\n",
                """
                TASK: pull out working bullets - one idea each, at most two nesting levels, each bullet understandable alone.
                FORM: use only headings that have content among: Facts / current state, Requirements, Decisions, Risks and constraints, Dependencies, Who does what and by when, Open questions.
                KEEP: numbers, deadlines, names, statuses and conditions as written.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "table",
                "Таблица",
                "▦",
                "Turn the following into a Markdown table:\n\n",
                """
                TASK: convert the text into a Markdown table. Detect the real structure first, then choose columns:
                comparison - one option per row, one criterion per column; requirements - id, requirement, priority, source, acceptance criterion; mapping - source field, target field, rule, notes; process - step, actor, trigger, result, system; action items - item, owner, deadline, status. Otherwise invent columns from the text itself.
                KEEP: cell values exactly as written - no paraphrase, no normalization. Leave field names and identifiers unchanged. Put a hyphen in empty cells.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "spec",
                "Задача",
                "📄",
                "Turn the following into a development specification:\n\n",
                """
                TASK: as a systems analyst, turn raw stakeholder notes into a hand-off spec a developer can start from. Use these sections and skip any the source does not support:
                1. Context (as-is) - current behaviour, people involved, pain.
                2. Goal - business outcome in one or two sentences.
                3. Scope (to-be) - required behaviour, not implementation.
                4. Out of scope - only when stated.
                5. Actors and permissions.
                6. Trigger and flow - start event, ordered steps, branches.
                7. Data - entities, fields, source of truth, states and transitions.
                8. Integrations and dependencies - systems, direction, blockers.
                9. Acceptance criteria - numbered, if/then, testable without the original discussion.
                10. Non-functional - volume, timing, security, environment limits.
                11. Edge cases and errors - only those stated or implied by the source rules.
                12. Questions for the stakeholder - concrete blockers to starting work.
                HARD RULE: put every gap or assumption only in section 12, never in the body. Do not choose an implementation the source left open. Keep priorities the source gives; invent none.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "code",
                "Разбор кода",
                "🔍",
                "Explain the following code:\n\n",
                """
                TASK: explain the code for a developer seeing it the first time. One or two sentences on purpose, then the important parts: inputs and outputs, main flow, side effects (I/O, network, shared state), error handling, edge cases and traps.
                KEEP: identifiers, functions and paths exactly as written. Do not rewrite or 'improve' the code; mention a defect only when needed for the explanation. If behaviour depends on missing callers, schemas or config, say so instead of guessing.
                FORM: prose first; short lists only when they help.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "questions",
                "Вопросы",
                "❓",
                "List clarifying questions for the following material:\n\n",
                """
                TASK: as a systems analyst receiving raw material, list clarifying questions that must be answered before work continues: ambiguities, contradictions, missing scenarios (errors, permissions, migrations, edge cases), hidden assumptions, missing non-functional limits (volume, timing, security, access).
                FORM: group by addressee using only groups that have content, with these exact headings: К бизнесу / К разработке / К безопасности. Numbered items inside each group; each question self-contained and ready to send.
                NEVER: answer the questions yourself, ask what the text already answers, or pad the list - only questions that block or change the work.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "reply",
                "Ответ",
                "✉",
                "Draft a reply to the following incoming message:\n\n",
                """
                TASK: the text is an INCOMING message (email or work chat) to the user. Draft a reply as the user. If the user's instruction states intent, write ONE reply that follows it; otherwise write 2-3 labeled variants for plausible intents: agreement / confirmation, clarification with questions, polite pushback with reasons.
                KEEP: the register and tone of the incoming message. Use only facts from the message or the user instruction; where a needed fact is missing (dates, names, numbers), insert a bracketed placeholder like [указать срок].
                FORM: ready to paste - no meta comments; label each variant briefly if there are several.
                """.trimIndent(),
                "auto",
                true,
                true,
                "deep",
            ),
            row(
                "reqreview",
                "Ревью требований",
                "🧐",
                "Review the following specification for defects:\n\n",
                """
                TASK: review the text as a senior systems analyst checking a specification before development. Find real defects: wording with more than one reading; internal contradictions; untestable claims; missing error and edge-case handling; undefined data, statuses, roles or permissions; missing non-functional needs (volume, timing, security, access); assumptions presented as facts.
                FORM: a Markdown table with columns # | Фрагмент (короткая цитата) | Проблема | Серьезность (критично / важно / мелочь) | Что уточнить или как исправить.
                List every issue found, including uncertain ones - severity does the filtering. After the table, one short paragraph on whether the text is ready for development.
                """.trimIndent(),
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
        )

    val quick: List<QuickRefine> =
        listOf(
            QuickRefine(
                "Короче",
                "Cut length noticeably. Drop filler and repeats, but keep every requirement, condition, number, name and open question.",
            ),
            QuickRefine(
                "Подробнее",
                "Expand with the steps, conditions and consequences a reader needs. Add no facts missing from the source; if something is unknown, say so.",
            ),
            QuickRefine(
                "Точнее",
                "Tighten wording: replace vague words with exact terms, name the actor and condition in each claim, remove ambiguity. Invent no new facts.",
            ),
            QuickRefine(
                "Проще",
                "Simplify for a reader outside the team: shorter sentences, plain words where jargon is optional. Keep facts, numbers and meaningful technical terms.",
            ),
            QuickRefine(
                "Формальнее",
                "Raise the register to formal business correspondence without changing the content.",
            ),
            QuickRefine(
                "Списком",
                "Reshape the same content as a compact list, one idea per item. Do not add or drop anything.",
            ),
            QuickRefine(
                "Другой вариант",
                "Give another version of the same result: different structure and wording, same facts and same task. Avoid repeating the previous phrasing.",
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

    val apps: List<AppProfile> =
        listOf(
            AppProfile(
                "chrome.exe",
                "Jira",
                "Jira (в браузере)",
                "Задачи и требования в Jira: описания, комментарии, критерии приёмки.",
                "The text comes from a Jira issue: keep issue keys (like ABC-123), status names and field labels exactly as written.",
            ),
            AppProfile(
                "chrome.exe",
                "Confluence",
                "Confluence (в браузере)",
                "Страницы вики: спецификации, регламенты, протоколы встреч.",
                "The text comes from a Confluence page: preserve headings, lists and table structure so the result can be pasted back as a document fragment.",
            ),
            AppProfile(
                "chrome.exe",
                "Битрикс24",
                "Битрикс24 (в браузере)",
                "Портал Битрикс24: задачи, CRM, комментарии, бизнес-процессы.",
                "The text comes from Bitrix24: keep entity names, field labels and workflow status names exactly as written, they are part of the system's configuration.",
            ),
            AppProfile(
                "chrome.exe",
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
                "The text comes from an IDE: treat it as source code, never translate identifiers, and keep formatting and indentation byte-for-byte.",
            ),
            AppProfile(
                "phpstorm64.exe",
                "",
                "PhpStorm",
                "IDE для PHP: код Bitrix24-проекта, конфиги.",
                "The text comes from an IDE: treat it as source code, never translate identifiers, and keep formatting and indentation byte-for-byte.",
            ),
            AppProfile(
                "pycharm64.exe",
                "",
                "PyCharm",
                "IDE для Python: скрипты, конфиги.",
                "The text comes from an IDE: treat it as source code, never translate identifiers, and keep formatting and indentation byte-for-byte.",
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
                "The text comes from a database tool: it is SQL or query output, so never change table, column or identifier names.",
            ),
            AppProfile(
                "dbeaver.exe",
                "",
                "DBeaver",
                "SQL-клиент: запросы, схемы, результаты выборок.",
                "The text comes from a database tool: it is SQL or query output, so never change table, column or identifier names.",
            ),
            AppProfile(
                "heidisql.exe",
                "",
                "HeidiSQL",
                "SQL-клиент для MySQL/MariaDB.",
                "The text comes from a database tool: it is SQL or query output, so never change table, column or identifier names.",
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

    private fun row(
        factoryId: String?,
        name: String,
        icon: String,
        prefix: String,
        task: String,
        lang: String,
        styleOn: Boolean,
        openInWindow: Boolean,
        role: String?,
    ): Action = Action(factoryId, name, icon, prefix, task, lang, styleOn, openInWindow, null, null, role)
}
