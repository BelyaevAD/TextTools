package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.settings_add_own
import texttools.app.generated.resources.settings_col_code
import texttools.app.generated.resources.settings_col_lang
import texttools.app.generated.resources.settings_col_prompt
import texttools.app.generated.resources.settings_delete
import texttools.app.generated.resources.settings_langs_hint

/**
 * Output languages. A tick stays on its row. Delete removes the selected row,
 * refuses the last language, and adding a code that already exists is refused by the window.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsLanguagesTab(
    form: SettingsForm,
    status: String?,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        Column(Modifier.fillMaxSize()) {
            Text(stringResource(Res.string.settings_langs_hint), fontSize = 13.sp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 160.dp)
                    .padding(top = 8.dp)
                    .border(1.dp, Color(0xFFB0B0B0)),
            ) {
                LanguageHeader()
                Divider(color = Color(0xFFD0D0D0))
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(count = form.languages.size, key = { index -> "$index:${form.languages[index].code}" }) { index ->
                        val language = form.languages[index]
                        LanguageRow(
                            name = language.name,
                            code = language.code,
                            prompt = language.prompt,
                            enabled = language.enabled,
                            rowSelected = form.selectedLanguage == index,
                            onChecked = { form.setLanguageEnabled(index, it) },
                            onSelect = { form.selectedLanguage = index },
                        )
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAdd,
                    modifier = Modifier.width(210.dp).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(stringResource(Res.string.settings_add_own), fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.width(150.dp).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(stringResource(Res.string.settings_delete), fontSize = 13.sp)
                }
            }
            if (status != null) {
                Text(status, color = MaterialTheme.colors.error, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun LanguageHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(Color(0xFFF3F3F3))
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(28.dp))
        HeaderCell(stringResource(Res.string.settings_col_lang), Modifier.weight(2f))
        HeaderCell(stringResource(Res.string.settings_col_code), Modifier.weight(0.8f))
        HeaderCell(stringResource(Res.string.settings_col_prompt), Modifier.weight(2.2f))
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
) {
    Text(text, modifier, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun LanguageRow(
    name: String,
    code: String,
    prompt: String,
    enabled: Boolean,
    rowSelected: Boolean,
    onChecked: (Boolean) -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .background(if (rowSelected) Color(0xFFD6EAF8) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = enabled, onCheckedChange = onChecked, modifier = Modifier.width(28.dp))
        Row(
            Modifier
                .weight(1f)
                .pointerInput(code) { detectTapGestures(onTap = { onSelect() }) }
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cell(name, Modifier.weight(2f))
            Cell(code, Modifier.weight(0.8f))
            Cell(prompt, Modifier.weight(2.2f))
        }
    }
}

@Composable
private fun Cell(
    text: String,
    modifier: Modifier,
) {
    Text(text, modifier, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
}
