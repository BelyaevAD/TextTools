package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.settings_clip_wait
import texttools.app.generated.resources.settings_endpoints
import texttools.app.generated.resources.settings_hotkey
import texttools.app.generated.resources.settings_hotkey_repeat
import texttools.app.generated.resources.settings_markdown
import texttools.app.generated.resources.settings_profiling
import texttools.app.generated.resources.settings_ui_lang
import texttools.app.generated.resources.settings_ui_lang_hint
import org.jetbrains.compose.resources.stringResource

/**
 * Interface tab. Hotkey fields keep AutoHotkey text such as `^!Space`.
 * An empty menu hotkey, two identical hotkeys, and a non-numeric clipboard wait do not save.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsInterfaceTab(
    form: SettingsForm,
    uiLanguages: List<UiLanguageChoice>,
    status: String?,
) {
    val choices = uiChoices(form.uiLang, uiLanguages)
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
                checked = form.profiling,
                label = stringResource(Res.string.settings_profiling),
                onCheckedChange = { form.profiling = it },
                modifier = Modifier.padding(top = 4.dp),
            )
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
                Text(status, color = MaterialTheme.colors.error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun LanguageBox(
    form: SettingsForm,
    choices: List<UiLanguageChoice>,
    modifier: Modifier = Modifier,
) {
    if (choices.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.code.equals(form.uiLang, ignoreCase = true) } ?: choices.first()
    Box(modifier) {
        Row(
            Modifier
                .width(280.dp)
                .height(28.dp)
                .border(1.dp, Color(0xFF808080))
                .clickable { open = true }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${selected.name} (${selected.code})",
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("▾", fontSize = 13.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    onClick = {
                        form.uiLang = choice.code
                        open = false
                    },
                ) {
                    Text("${choice.name} (${choice.code})")
                }
            }
        }
    }
}
