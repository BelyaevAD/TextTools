package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.belyaevad.texttools.app.ui.AppSelect
import org.jetbrains.compose.resources.stringResource
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.settings_autostart
import texttools.app.generated.resources.settings_clip_wait
import texttools.app.generated.resources.settings_endpoints
import texttools.app.generated.resources.settings_hotkey
import texttools.app.generated.resources.settings_hotkey_repeat
import texttools.app.generated.resources.settings_markdown
import texttools.app.generated.resources.settings_profile_model
import texttools.app.generated.resources.settings_profile_model_default
import texttools.app.generated.resources.settings_profile_model_hint
import texttools.app.generated.resources.settings_profiling
import texttools.app.generated.resources.settings_sentry_errors
import texttools.app.generated.resources.settings_sentry_hint
import texttools.app.generated.resources.settings_sentry_traces
import texttools.app.generated.resources.settings_ui_lang
import texttools.app.generated.resources.settings_ui_lang_hint

/**
 * Interface tab. Hotkey fields keep AutoHotkey text such as `^!Space`.
 * An empty menu hotkey, two identical hotkeys, and a non-numeric clipboard wait do not save.
 */
@Composable
fun SettingsInterfaceTab(
    form: SettingsForm,
    uiLanguages: List<UiLanguageChoice>,
    status: String?,
) {
    val choices = uiChoices(form.uiLang, uiLanguages)
    Column(Modifier.fillMaxSize()) {
        FieldLabel(stringResource(Res.string.settings_ui_lang))
        LanguageBox(form = form, choices = choices, modifier = Modifier.padding(top = 4.dp))
        Hint(stringResource(Res.string.settings_ui_lang_hint), Modifier.fillMaxWidth().padding(top = 4.dp))
        FieldLabel(stringResource(Res.string.settings_hotkey), Modifier.padding(top = 12.dp))
        LineField(
            value = form.hotkey,
            onValueChange = { form.hotkey = it },
            modifier = Modifier.width(280.dp).padding(top = 4.dp),
        )
        FieldLabel(stringResource(Res.string.settings_hotkey_repeat), Modifier.padding(top = 12.dp))
        LineField(
            value = form.hotkeyRepeat,
            onValueChange = { form.hotkeyRepeat = it },
            modifier = Modifier.width(280.dp).padding(top = 4.dp),
        )
        CheckLine(
            checked = form.markdown,
            label = stringResource(Res.string.settings_markdown),
            onCheckedChange = { form.markdown = it },
            modifier = Modifier.padding(top = 12.dp),
        )
        CheckLine(
            checked = form.autostart,
            label = stringResource(Res.string.settings_autostart),
            onCheckedChange = { form.autostart = it },
            modifier = Modifier.padding(top = 4.dp),
        )
        CheckLine(
            checked = form.profiling,
            label = stringResource(Res.string.settings_profiling),
            onCheckedChange = { form.profiling = it },
            modifier = Modifier.padding(top = 4.dp),
        )
        ProfileModelBox(form)
        Hint(stringResource(Res.string.settings_profile_model_hint), Modifier.fillMaxWidth().padding(top = 4.dp))
        CheckLine(
            checked = form.sentryErrors,
            label = stringResource(Res.string.settings_sentry_errors),
            onCheckedChange = { form.sentryErrors = it },
            modifier = Modifier.padding(top = 12.dp),
        )
        CheckLine(
            checked = form.sentryTraces,
            label = stringResource(Res.string.settings_sentry_traces),
            onCheckedChange = { form.sentryTraces = it },
            modifier = Modifier.padding(top = 4.dp),
        )
        Hint(stringResource(Res.string.settings_sentry_hint), Modifier.fillMaxWidth().padding(top = 4.dp))
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.settings_clip_wait), fontSize = 13.sp, modifier = Modifier.weight(1f))
            LineField(
                value = form.clipWait,
                onValueChange = { form.clipWait = it },
                modifier = Modifier.width(80.dp),
            )
        }
        Hint(stringResource(Res.string.settings_endpoints), Modifier.fillMaxWidth().padding(top = 16.dp))
        if (status != null) {
            Text(status, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ProfileModelBox(form: SettingsForm) {
    val fallback = stringResource(Res.string.settings_profile_model_default)
    val ids = form.models.map { it.id }.distinct()
    val options = listOf(fallback) + ids
    val selected = if (form.profileModel.isBlank()) 0 else ids.indexOf(form.profileModel) + 1
    FieldLabel(stringResource(Res.string.settings_profile_model), Modifier.padding(top = 4.dp))
    AppSelect(
        options = options,
        selected = selected.coerceAtLeast(0),
        onSelect = { index -> form.profileModel = if (index == 0) "" else ids[index - 1] },
        modifier = Modifier.width(360.dp).padding(top = 4.dp),
    )
}

@Composable
private fun LanguageBox(
    form: SettingsForm,
    choices: List<UiLanguageChoice>,
    modifier: Modifier = Modifier,
) {
    if (choices.isEmpty()) return
    val selected = choices.indexOfFirst { it.code.equals(form.uiLang, ignoreCase = true) }.coerceAtLeast(0)
    AppSelect(
        options = choices.map { "${it.name} (${it.code})" },
        selected = selected,
        onSelect = { index -> form.uiLang = choices[index].code },
        modifier = modifier.width(280.dp),
    )
}
