package dev.belyaevad.texttools.data

import app.cash.sqldelight.db.SqlDriver
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.QuickRefine

/** SQLite store. An empty action table is seeded so the hotkey still has a menu. */
class TextToolsStore(
    driver: SqlDriver,
) {
    private val db = TextToolsDatabase(driver)

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
}
