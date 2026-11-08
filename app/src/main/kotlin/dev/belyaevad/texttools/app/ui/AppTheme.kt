package dev.belyaevad.texttools.app.ui

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Format locale captured before any interface override. */
internal val systemFormatLocale: Locale = Locale.getDefault(Locale.Category.FORMAT)

/** Selects bundled English or Russian. The format category stays [systemFormatLocale]. */
internal fun applyUiLocale(language: String) {
    val selected = if (language.equals("ru", ignoreCase = true)) "ru" else "en"
    val ui = Locale.forLanguageTag(selected)
    Locale.setDefault(ui)
    Locale.setDefault(Locale.Category.DISPLAY, ui)
    Locale.setDefault(Locale.Category.FORMAT, systemFormatLocale)
}

/** Light Material 3 theme. Zero minimum touch size keeps the existing button heights. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp, content = content)
    }
}

/** Desktop resource override. Compose reads [Locale.getDefault]; dates and numbers stay on the startup format locale. */
@Composable
fun AppLocale(
    language: String,
    content: @Composable () -> Unit,
) {
    applyUiLocale(language)
    content()
}
