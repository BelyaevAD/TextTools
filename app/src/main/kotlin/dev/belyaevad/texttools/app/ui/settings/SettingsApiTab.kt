package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.settings_api_key
import texttools.app.generated.resources.settings_base_url
import texttools.app.generated.resources.settings_col_context
import texttools.app.generated.resources.settings_col_in
import texttools.app.generated.resources.settings_col_model
import texttools.app.generated.resources.settings_col_out
import texttools.app.generated.resources.settings_load
import texttools.app.generated.resources.settings_make_default
import texttools.app.generated.resources.settings_model
import texttools.app.generated.resources.settings_models
import texttools.app.generated.resources.settings_models_hint
import texttools.app.generated.resources.settings_proxy
import texttools.app.generated.resources.settings_proxy_hint
import texttools.app.generated.resources.settings_recommend
import texttools.app.generated.resources.settings_retarget
import texttools.app.generated.resources.settings_test
import dev.belyaevad.texttools.domain.CatalogEntry
import dev.belyaevad.texttools.domain.providerUrls
import org.jetbrains.compose.resources.stringResource

/** Buttons on the API tab. The form itself owns the field values. */
class ApiActions(
    val onLoad: () -> Unit,
    val onRecommend: () -> Unit,
    val onTest: () -> Unit,
    val onRetarget: () -> Unit,
    val onMakeDefault: (Int?) -> Unit,
    val onDismissError: () -> Unit,
)

/**
 * API tab. A tick does not move the row. A double click, like "Set as default model",
 * writes that row into the default field and the star follows the field.
 * Choosing another address does not replace the rows until Load is pressed.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsApiTab(
    form: SettingsForm,
    catalog: List<CatalogEntry>,
    status: String?,
    statusIsError: Boolean,
    actions: ApiActions,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (status != null) {
                Text(
                    status,
                    color = if (statusIsError) MaterialTheme.colors.error else MaterialTheme.colors.onSurface,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            FieldLabel(stringResource(Res.string.settings_base_url))
            UrlBox(
                value = form.baseUrl,
                onValue = { form.baseUrl = it },
                modifier = Modifier.padding(top = 4.dp),
            )
            FieldLabel(stringResource(Res.string.settings_api_key), Modifier.padding(top = 8.dp))
            LineField(
                value = form.apiKey,
                onValueChange = { form.apiKey = it },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                password = true,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = form.proxyOn,
                    onCheckedChange = {
                        form.proxyOn = it
                        actions.onDismissError()
                    },
                )
                Text(stringResource(Res.string.settings_proxy), fontSize = 13.sp, modifier = Modifier.width(72.dp))
                LineField(
                    value = form.proxy,
                    onValueChange = {
                        form.proxy = it
                        actions.onDismissError()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = form.proxyOn,
                )
            }
            Hint(
                stringResource(Res.string.settings_proxy_hint),
                Modifier.padding(start = 108.dp, top = 2.dp),
            )
            FieldLabel(stringResource(Res.string.settings_model), Modifier.padding(top = 8.dp))
            LineField(
                value = form.defaultModel,
                onValueChange = { form.defaultModel = it },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            FieldLabel(stringResource(Res.string.settings_models), Modifier.padding(top = 8.dp))
            ModelTable(
                form = form,
                catalog = catalog,
                onMakeDefault = actions.onMakeDefault,
                modifier = Modifier.fillMaxWidth().height(168.dp).padding(top = 4.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabButton(stringResource(Res.string.settings_load), !form.busy, actions.onLoad, Modifier.weight(1f))
                TabButton(stringResource(Res.string.settings_recommend), !form.busy, actions.onRecommend, Modifier.weight(1f))
                TabButton(stringResource(Res.string.settings_test), !form.busy, actions.onTest, Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabButton(
                    stringResource(Res.string.settings_make_default),
                    enabled = true,
                    onClick = { actions.onMakeDefault(form.selectedModel) },
                    modifier = Modifier.weight(1f),
                )
                TabButton(
                    stringResource(Res.string.settings_retarget),
                    enabled = !form.busy,
                    onClick = actions.onRetarget,
                    modifier = Modifier.weight(1.4f),
                )
            }
            Hint(stringResource(Res.string.settings_models_hint), Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun UrlBox(
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineField(value = value, onValueChange = onValue, modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = { open = true },
                modifier = Modifier.padding(start = 4.dp).width(36.dp).height(28.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("▾")
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            providerUrls().forEach { (url, _) ->
                DropdownMenuItem(
                    onClick = {
                        onValue(url)
                        open = false
                    },
                ) {
                    Text(url, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun ModelTable(
    form: SettingsForm,
    catalog: List<CatalogEntry>,
    onMakeDefault: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.border(1.dp, Color(0xFFB0B0B0))) {
        ModelHeader()
        Divider(color = Color(0xFFD0D0D0))
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(count = form.models.size, key = { index -> "$index:${form.models[index].id}" }) { index ->
                val model = form.models[index]
                val priced = catalogRow(catalog, model.id, form.modelsUrl)
                ModelRow(
                    id = model.id,
                    selected = model.selected,
                    starred = model.id.equals(form.defaultModel.trim(), ignoreCase = true),
                    rowSelected = form.selectedModel == index,
                    context = contextText(priced),
                    promptPrice = priceText(priced?.promptPerToken),
                    completionPrice = priceText(priced?.completionPerToken),
                    onChecked = { form.setModelSelected(index, it) },
                    onSelect = { form.selectedModel = index },
                    onDoubleClick = { onMakeDefault(index) },
                )
            }
        }
    }
}

@Composable
private fun ModelHeader() {
    Row(
        Modifier.fillMaxWidth().height(28.dp).background(Color(0xFFF3F3F3)).padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(28.dp))
        HeaderCell(stringResource(Res.string.settings_col_model), Modifier.weight(3.2f))
        HeaderCell(stringResource(Res.string.settings_col_context), Modifier.weight(1f))
        HeaderCell(stringResource(Res.string.settings_col_in), Modifier.weight(1f))
        HeaderCell(stringResource(Res.string.settings_col_out), Modifier.weight(1f))
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
private fun ModelRow(
    id: String,
    selected: Boolean,
    starred: Boolean,
    rowSelected: Boolean,
    context: String,
    promptPrice: String,
    completionPrice: String,
    onChecked: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .background(if (rowSelected) Color(0xFFD6EAF8) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = onChecked, modifier = Modifier.width(28.dp))
        Row(
            Modifier
                .weight(1f)
                .pointerInput(id) {
                    detectTapGestures(
                        onTap = { onSelect() },
                        onDoubleTap = { onDoubleClick() },
                    )
                }
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cell(if (starred) "★ $id" else id, Modifier.weight(3.2f))
            Cell(context, Modifier.weight(1f))
            Cell(promptPrice, Modifier.weight(1f))
            Cell(completionPrice, Modifier.weight(1f))
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
