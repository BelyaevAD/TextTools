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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.belyaevad.texttools.app.ui.AppSecondaryButton
import dev.belyaevad.texttools.app.ui.AppSelect
import dev.belyaevad.texttools.domain.CatalogEntry
import dev.belyaevad.texttools.domain.providerUrls
import org.jetbrains.compose.resources.stringResource
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
import texttools.app.generated.resources.settings_models_side
import texttools.app.generated.resources.settings_proxy
import texttools.app.generated.resources.settings_proxy_hint
import texttools.app.generated.resources.settings_recommend
import texttools.app.generated.resources.settings_retarget
import texttools.app.generated.resources.settings_test

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
@Composable
fun SettingsApiTab(
    form: SettingsForm,
    catalog: List<CatalogEntry>,
    status: String?,
    statusIsError: Boolean,
    actions: ApiActions,
) {
    Column(Modifier.fillMaxSize()) {
        if (status != null) {
            Text(
                status,
                color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
        FieldLabel(stringResource(Res.string.settings_api_key), Modifier.padding(top = 4.dp))
        LineField(
            value = form.apiKey,
            onValueChange = { form.apiKey = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            password = true,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
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
        FieldLabel(stringResource(Res.string.settings_model), Modifier.padding(top = 4.dp))
        LineField(
            value = form.defaultModel,
            onValueChange = { form.defaultModel = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        modelList(
            form = form,
            catalog = catalog,
            onMakeDefault = actions.onMakeDefault,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
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

@Composable
private fun UrlBox(
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val urls = providerUrls().map { it.first }
    val options = if (value.isBlank() || value in urls) urls else urls + value
    AppSelect(
        options = options,
        selected = options.indexOf(value),
        onSelect = { index -> onValue(options[index]) },
        modifier = modifier.fillMaxWidth(),
        editable = true,
        onValueChange = onValue,
    )
}

@Composable
private fun TabButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        fontSize = 12.sp,
    )
}

@Composable
private fun modelList(
    form: SettingsForm,
    catalog: List<CatalogEntry>,
    onMakeDefault: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    Column(modifier) {
        Hint(
            stringResource(Res.string.settings_models_side),
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Column(Modifier.weight(1f).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            ModelHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (form.models.isEmpty()) {
                    Hint(
                        stringResource(Res.string.settings_models),
                        modifier = Modifier.padding(8.dp),
                    )
                } else {
                    LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
                        items(count = form.models.size, key = { index -> "$index:${form.models[index].id}" }) { index ->
                            val model = form.models[index]
                            val priced = catalogRow(catalog, model.id, form.modelsUrl)
                            ModelRow(
                                id = model.id,
                                context = contextText(priced),
                                prompt = priceText(priced?.promptPerToken),
                                completion = priceText(priced?.completionPerToken),
                                checked = model.selected,
                                starred = model.id.equals(form.defaultModel.trim(), ignoreCase = true),
                                rowSelected = form.selectedModel == index,
                                onChecked = { form.setModelSelected(index, it) },
                                onSelect = { form.selectedModel = index },
                                onDoubleClick = { onMakeDefault(index) },
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(state),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        style = listScrollbar,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(28.dp))
        HeaderCell(stringResource(Res.string.settings_col_model), Modifier.weight(3f), TextAlign.Start)
        HeaderCell(stringResource(Res.string.settings_col_context), Modifier.weight(1f), TextAlign.End)
        HeaderCell(stringResource(Res.string.settings_col_in), Modifier.weight(1f), TextAlign.End)
        HeaderCell(stringResource(Res.string.settings_col_out), Modifier.weight(1f), TextAlign.End)
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
    align: TextAlign,
) {
    Text(
        text,
        modifier.padding(horizontal = 6.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ModelRow(
    id: String,
    context: String,
    prompt: String,
    completion: String,
    checked: Boolean,
    starred: Boolean,
    rowSelected: Boolean,
    onChecked: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(if (rowSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.width(28.dp))
        Row(
            Modifier
                .weight(1f)
                .pointerInput(id) {
                    detectTapGestures(onTap = { onSelect() }, onDoubleTap = { onDoubleClick() })
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cell(if (starred) "★ $id" else id, Modifier.weight(3f), TextAlign.Start)
            Cell(context, Modifier.weight(1f), TextAlign.End)
            Cell(prompt, Modifier.weight(1f), TextAlign.End)
            Cell(completion, Modifier.weight(1f), TextAlign.End)
        }
    }
}

@Composable
private fun Cell(
    text: String,
    modifier: Modifier,
    align: TextAlign,
) {
    Text(
        text,
        modifier.padding(horizontal = 6.dp),
        fontSize = 13.sp,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
    )
}
