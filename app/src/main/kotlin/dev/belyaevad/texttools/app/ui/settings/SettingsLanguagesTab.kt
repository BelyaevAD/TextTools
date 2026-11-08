package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.belyaevad.texttools.app.ui.AppDangerButton
import dev.belyaevad.texttools.app.ui.AppSecondaryButton
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
@Composable
fun SettingsLanguagesTab(
    form: SettingsForm,
    status: String?,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Hint(stringResource(Res.string.settings_langs_hint))
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 160.dp)
                .padding(top = 8.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            val rows = rememberLazyListState()
            LanguageHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(state = rows, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
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
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(rows),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    style = listScrollbar,
                )
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppSecondaryButton(
                text = stringResource(Res.string.settings_add_own),
                onClick = onAdd,
                modifier = Modifier.width(210.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                fontSize = 13.sp,
            )
            AppDangerButton(
                text = stringResource(Res.string.settings_delete),
                onClick = onDelete,
                modifier = Modifier.width(150.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                fontSize = 13.sp,
            )
        }
        if (status != null) {
            Text(status, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun LanguageHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(end = 12.dp),
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
            .background(if (rowSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = enabled, onCheckedChange = onChecked, modifier = Modifier.width(28.dp))
        Row(
            Modifier
                .weight(1f)
                .pointerInput(code) { detectTapGestures(onTap = { onSelect() }) },
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
