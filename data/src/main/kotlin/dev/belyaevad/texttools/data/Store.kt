package dev.belyaevad.texttools.data

import app.cash.sqldelight.db.SqlDriver
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.AppProfile
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.HISTORY_LIMIT
import dev.belyaevad.texttools.domain.KnownModel
import dev.belyaevad.texttools.domain.OutputLanguage
import dev.belyaevad.texttools.domain.QuickRefine
import dev.belyaevad.texttools.domain.trimStored

/** SQLite store. An empty action table is seeded so the hotkey still has a menu. */
class TextToolsStore(
    driver: SqlDriver,
) {
    private val db = TextToolsDatabase(driver)
    private val usageGate = Any()

    /** Loads actions. The role is read back from the factory seed, not from a column. */
    fun actions(): List<Action> {
        val rows = db.textToolsQueries.selectAllActions().executeAsList()
        if (rows.isEmpty()) {
            saveActions(Factory.actions)
            return Factory.actions
        }
        return rows.map { row ->
            Action(
                factoryId = row.factory_id,
                name = row.name,
                icon = row.icon,
                prefix = row.prefix,
                task = row.task,
                lang = row.lang,
                styleOn = row.style_on == 1L,
                openInWindow = row.open_in_window == 1L,
                menuKey = row.menu_key,
                model = row.model,
                role = Factory.actions.firstOrNull { it.factoryId == row.factory_id }?.role,
            )
        }
    }

    /** Replaces the action table. There is no role column. */
    fun saveActions(actions: List<Action>) {
        val queries = db.textToolsQueries
        queries.transaction {
            queries.deleteActions()
            actions.take(Factory.MAX_ACTIONS).forEachIndexed { index, action ->
                queries.insertAction(
                    index.toLong(),
                    action.factoryId,
                    action.name,
                    action.icon,
                    action.prefix,
                    action.task,
                    action.menuKey,
                    action.model,
                    action.lang,
                    if (action.styleOn) 1L else 0L,
                    if (action.openInWindow) 1L else 0L,
                )
            }
        }
    }

    /**
     * Loads chips. No flag means the factory set. Flag 1 with zero rows means the user cleared them.
     */
    fun quick(): List<QuickRefine> {
        val flag = db.textToolsQueries.selectSetting("quick_saved").executeAsOneOrNull()
        if (flag == null) return Factory.quick
        return db.textToolsQueries
            .selectQuick()
            .executeAsList()
            .map { QuickRefine(it.label, it.instruction) }
    }

    /** Persists chips, including an intentional empty list. */
    fun saveQuick(items: List<QuickRefine>) {
        val queries = db.textToolsQueries
        queries.transaction {
            queries.deleteQuick()
            items.forEachIndexed { index, item -> queries.insertQuick(index.toLong(), item.label, item.instruction) }
            queries.upsertSetting("quick_saved", "1")
        }
    }

    /** Loads profiles in stored order. An empty title and an empty hint come back as empty strings. */
    fun apps(): List<AppProfile> =
        db.textToolsQueries.selectApps().executeAsList().map { row ->
            AppProfile(row.proc, row.title, row.name, row.description, row.hint)
        }

    /** Replaces profiles. An empty title and an empty hint are stored as empty strings. */
    fun saveApps(apps: List<AppProfile>) {
        val queries = db.textToolsQueries
        queries.transaction {
            queries.deleteApps()
            apps.forEachIndexed { index, app ->
                queries.insertApp(
                    index.toLong(),
                    app.proc,
                    app.title,
                    app.name,
                    app.description,
                    app.hint,
                )
            }
        }
    }

    /** Loads output languages in stored order. */
    fun languages(): List<OutputLanguage> =
        db.textToolsQueries.selectLanguages().executeAsList().map { row ->
            OutputLanguage(row.code, row.name, row.prompt, row.enabled == 1L)
        }

    /** Replaces output languages, including a list where every language is disabled. */
    fun saveLanguages(languages: List<OutputLanguage>) {
        val queries = db.textToolsQueries
        queries.transaction {
            queries.deleteLanguages()
            languages.forEachIndexed { index, language ->
                queries.insertLanguage(
                    index.toLong(),
                    language.code,
                    language.name,
                    language.prompt,
                    if (language.enabled) 1L else 0L,
                )
            }
        }
    }

    /** Loads the model list of this address. Another address keeps its own rows. */
    fun models(baseUrl: String): List<KnownModel> =
        db.textToolsQueries.selectModels(baseUrl).executeAsList().map { row ->
            KnownModel(row.model_id, row.selected == 1L)
        }

    /** Replaces the model list of this address. Lists stored for other addresses stay. */
    fun saveModels(
        baseUrl: String,
        models: List<KnownModel>,
    ) {
        val queries = db.textToolsQueries
        queries.transaction {
            queries.deleteModels(baseUrl)
            models.forEachIndexed { index, model ->
                queries.insertModel(
                    baseUrl,
                    index.toLong(),
                    model.id,
                    if (model.selected) 1L else 0L,
                )
            }
        }
    }

    /** Loads history in stored order. */
    fun history(): List<HistoryRecord> =
        db.textToolsQueries.selectHistory().executeAsList().map { row ->
            HistoryRecord(
                row.at_epoch_ms,
                row.action_name,
                row.model,
                row.prompt_tokens?.toInt(),
                row.completion_tokens?.toInt(),
                row.cost,
                row.source,
                row.result,
            )
        }

    /**
     * Replaces history. Keeps the last 20 rows of [entries].
     * Source and result are cut to 8000 characters before they are written.
     */
    fun saveHistory(entries: List<HistoryRecord>) {
        val kept =
            entries.takeLast(HISTORY_LIMIT).map { entry ->
                entry.copy(source = trimStored(entry.source), result = trimStored(entry.result))
            }
        val queries = db.textToolsQueries
        queries.transaction {
            queries.deleteHistory()
            kept.forEachIndexed { index, entry ->
                queries.insertHistory(
                    index.toLong(),
                    entry.atEpochMs,
                    entry.actionName,
                    entry.model,
                    entry.promptTokens?.toLong(),
                    entry.completionTokens?.toLong(),
                    entry.cost,
                    entry.source,
                    entry.result,
                )
            }
        }
    }

    /** Loads the usage log in stored order. */
    fun usage(): List<UsageRecord> =
        db.textToolsQueries.selectUsage().executeAsList().map { row ->
            UsageRecord(
                row.at_epoch_ms,
                row.label,
                row.model,
                row.prompt_tokens.toInt(),
                row.completion_tokens.toInt(),
            )
        }

    /** Replaces the usage log. */
    fun saveUsage(events: List<UsageRecord>) {
        val queries = db.textToolsQueries
        queries.transaction {
            queries.deleteUsage()
            events.forEachIndexed { index, event ->
                queries.insertUsage(
                    index.toLong(),
                    event.atEpochMs,
                    event.label,
                    event.model,
                    event.promptTokens.toLong(),
                    event.completionTokens.toLong(),
                )
            }
        }
    }

    /** Adds one row with the next position. The lock keeps two writers from taking the same position. */
    fun appendUsage(event: UsageRecord) {
        synchronized(usageGate) {
            db.textToolsQueries.appendUsage(
                event.atEpochMs,
                event.label,
                event.model,
                event.promptTokens.toLong(),
                event.completionTokens.toLong(),
            )
        }
    }

    /** Loads the price catalogue, or null when nothing has been stored. */
    fun prices(): PriceCache? {
        val row = db.textToolsQueries.selectPriceCache().executeAsOneOrNull() ?: return null
        return PriceCache(row.body, row.fetched_at)
    }

    /** Replaces the price catalogue body and the time it was loaded. */
    fun savePrices(cache: PriceCache) {
        val queries = db.textToolsQueries
        queries.transaction {
            queries.deletePriceCache()
            queries.insertPriceCache(cache.body, cache.fetchedAtEpochMs)
        }
    }

    /** Appends one privacy-safe request measurement. No prompt or answer text is stored. */
    fun appendUsageEvent(event: UsageEvent) {
        db.textToolsQueries.insertUsageEvent(
            event.atEpochMs,
            event.kind,
            event.model,
            event.outcome,
            event.inputChars.toLong(),
            event.extraChars.toLong(),
            event.outputChars.toLong(),
            event.durationMs,
            event.promptTokens?.toLong(),
            event.completionTokens?.toLong(),
            null,
        )
    }

    fun usageEvents(): List<UsageEvent> = db.textToolsQueries.selectUsageEvents().executeAsList().map(::usageEvent)

    private fun usageEvent(row: dev.belyaevad.texttools.data.db.Usage_event): UsageEvent =
        UsageEvent(
            id = row.id,
            atEpochMs = row.at_epoch_ms,
            kind = row.kind,
            model = row.model,
            outcome = row.outcome,
            inputChars = row.input_chars.toInt(),
            extraChars = row.extra_chars.toInt(),
            outputChars = row.output_chars.toInt(),
            durationMs = row.duration_ms,
            promptTokens = row.prompt_tokens?.toInt(),
            completionTokens = row.completion_tokens?.toInt(),
        )
}

/** One model call measured by monotonic duration. Texts and credentials are not fields. */
data class UsageEvent(
    val atEpochMs: Long,
    val kind: String,
    val model: String,
    val outcome: String,
    val inputChars: Int,
    val extraChars: Int,
    val outputChars: Int,
    val durationMs: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val id: Long = 0,
)

/** One history row. [cost] is null when the price was unknown. Text is trimmed on write. */
data class HistoryRecord(
    val atEpochMs: Long,
    val actionName: String,
    val model: String,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val cost: Double?,
    val source: String,
    val result: String,
)

/** One usage-log row. The cost estimate is not stored here. */
data class UsageRecord(
    val atEpochMs: Long,
    val label: String,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
)

/** The stored price catalogue and the Unix millisecond time it was loaded. */
data class PriceCache(
    val body: String,
    val fetchedAtEpochMs: Long,
)
