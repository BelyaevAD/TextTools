package dev.belyaevad.texttools.app.ui.settings

import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.KnownModel
import dev.belyaevad.texttools.domain.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsFormTest {
    @Test
    fun anUntouchedFormKeepsTheKeyTheModelsAndTheirAddress() {
        val form = form(key = "sk-live")
        val draft = ready(form)
        assertEquals("sk-live", draft.apiKey)
        assertEquals(listOf(KnownModel("gpt-old", selected = true)), draft.models)
        assertEquals(OPENAI, draft.modelsUrl)
    }

    @Test
    fun recommendedModelsGoToTheNewAddress() {
        val form = form()
        form.baseUrl = ANTHROPIC
        form.applyRecommended(recommendedModels(ANTHROPIC))
        val draft = ready(form)
        assertEquals(ANTHROPIC, draft.modelsUrl)
        assertTrue(draft.models.none { it.id == "gpt-old" })
        assertTrue(recommendedModels(ANTHROPIC).all { pick -> draft.models.any { it.id == pick.id && it.selected } })
    }

    @Test
    fun recommendedModelsKeepTheStoredRowsOfTheNewAddress() {
        val form = form()
        form.baseUrl = ANTHROPIC
        form.applyRecommended(recommendedModels(ANTHROPIC))
        val draft = ready(form)
        assertEquals(STORED, draft.models.take(STORED.size))
    }

    @Test
    fun retargetedModelsGoToTheNewAddressAndKeepItsStoredRows() {
        val form = form()
        form.baseUrl = ANTHROPIC
        form.tickModels(listOf("claude-x"))
        val draft = ready(form)
        assertEquals(ANTHROPIC, draft.modelsUrl)
        assertEquals(STORED + KnownModel("claude-x", selected = true), draft.models)
    }

    @Test
    fun retargetReachesOnlyTheDraftForTheSameAddress() {
        val form = form()
        assertNull(ready(form).retargetUrl)
        form.baseUrl = ANTHROPIC
        form.retargetedUrl = ANTHROPIC
        assertEquals(ANTHROPIC, ready(form).retargetUrl)
    }

    @Test
    fun aChangedAddressAfterRetargetCarriesNoRetarget() {
        val form = form()
        form.baseUrl = ANTHROPIC
        form.retargetedUrl = ANTHROPIC
        form.baseUrl = "https://api.mistral.ai/v1"
        assertNull(ready(form).retargetUrl)
    }

    private fun form(key: String = ""): SettingsForm =
        SettingsForm(
            Settings(
                baseUrl = OPENAI,
                defaultModel = "gpt-old",
                proxy = "",
                proxyOn = false,
                hotkey = "^!Space",
                hotkeyRepeat = "",
                markdown = false,
                uiLang = "en",
                appProfiling = false,
                clipWaitSeconds = 0.8,
            ),
            initialKey = key,
            initialModels = listOf(KnownModel("gpt-old", selected = true)),
            initialLanguages = Factory.languages,
            storedModels = { url -> if (url == ANTHROPIC) STORED else emptyList() },
        )

    private fun ready(form: SettingsForm): SettingsDraft = (form.trySave() as SaveResult.Ready).draft
}

private const val OPENAI = "https://api.openai.com/v1"
private const val ANTHROPIC = "https://api.anthropic.com/v1"
private val STORED = listOf(KnownModel("claude-old", selected = true), KnownModel("claude-unticked", selected = false))
