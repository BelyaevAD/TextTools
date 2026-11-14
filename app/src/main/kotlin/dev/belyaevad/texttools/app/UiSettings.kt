package dev.belyaevad.texttools.app

import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.Settings
import dev.belyaevad.texttools.domain.clipWaitSeconds
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

internal const val UI_SETTINGS_VERSION = 1

internal fun uiSettingsVersion(props: Properties): Int = props.getProperty("version")?.trim()?.toIntOrNull() ?: 0

internal fun uiSettingsStale(props: Properties): Boolean = uiSettingsVersion(props) < UI_SETTINGS_VERSION

/**
 * Values already stored stay. A missing key, and a blank menu hotkey, take the factory value.
 * [proxyUnreadable] keeps the proxy switched on, so an undecryptable proxy file is never read as "no proxy".
 */
internal fun settingsFromProperties(
    props: Properties,
    proxy: String,
    factory: Settings = Factory.defaultSettings,
    proxyUnreadable: Boolean = false,
): Settings {
    val clip = props.getProperty("clipWaitSeconds")?.toDoubleOrNull() ?: factory.clipWaitSeconds
    return factory.copy(
        baseUrl = textOr(props, "baseUrl", factory.baseUrl).ifBlank { factory.baseUrl },
        defaultModel = textOr(props, "defaultModel", factory.defaultModel).ifBlank { factory.defaultModel },
        proxy = proxy,
        proxyOn = if (proxyUnreadable) proxyFlag(props, true) ?: true else proxyFlag(props, proxy.isNotEmpty()),
        hotkey = textOr(props, "hotkey", factory.hotkey).ifBlank { factory.hotkey },
        hotkeyRepeat = textOr(props, "hotkeyRepeat", factory.hotkeyRepeat),
        markdown = boolOr(props, "markdown", factory.markdown),
        uiLang = textOr(props, "uiLang", factory.uiLang).ifBlank { factory.uiLang },
        appProfiling = boolOr(props, "appProfiling", factory.appProfiling),
        clipWaitSeconds = clipWaitSeconds(clip),
        sentryErrors = boolOr(props, "sentryErrors", factory.sentryErrors),
        autostart = props.getProperty("autostart")?.equals("true", ignoreCase = true) ?: factory.autostart,
        profileModel = textOr(props, "profileModel", factory.profileModel),
    )
}

/** [previous] keys that this version does not know are kept. [version] becomes the current one. */
internal fun currentUiProperties(
    previous: Properties,
    settings: Settings,
    persistAutostart: Boolean,
): Properties {
    val props = Properties()
    for (name in previous.stringPropertyNames()) {
        props.setProperty(name, previous.getProperty(name).orEmpty())
    }
    props.setProperty("version", UI_SETTINGS_VERSION.toString())
    props.setProperty("baseUrl", settings.baseUrl)
    props.setProperty("defaultModel", settings.defaultModel)
    props.setProperty("proxyOn", settings.proxyOn?.toString().orEmpty())
    props.setProperty("hotkey", settings.hotkey)
    props.setProperty("hotkeyRepeat", settings.hotkeyRepeat)
    props.setProperty("markdown", settings.markdown.toString())
    props.setProperty("uiLang", settings.uiLang)
    props.setProperty("appProfiling", settings.appProfiling.toString())
    props.setProperty("clipWaitSeconds", settings.clipWaitSeconds.toString())
    props.setProperty("sentryErrors", settings.sentryErrors.toString())
    props.setProperty("profileModel", settings.profileModel)
    if (persistAutostart || previous.containsKey("autostart")) {
        props.setProperty("autostart", settings.autostart.toString())
    } else {
        props.remove("autostart")
    }
    return props
}

/** Writes [props] beside [file] and then moves that file into place. A live file that cannot be read is left as it is. */
internal fun writeUiFile(
    file: Path,
    props: Properties,
) {
    if (Files.isRegularFile(file)) {
        try {
            Files.newBufferedReader(file, Charsets.UTF_8).use { reader -> Properties().load(reader) }
        } catch (_: java.io.IOException) {
            return
        }
    }
    val tmp = file.resolveSibling("${file.fileName}.tmp")
    try {
        file.parent?.let { Files.createDirectories(it) }
        Files.newBufferedWriter(tmp, Charsets.UTF_8).use { props.store(it, null) }
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: java.nio.file.FileAlreadyExistsException) {
            // ATOMIC_MOVE ignores REPLACE_EXISTING, so an existing file fails this way.
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
        }
    } catch (error: java.io.IOException) {
        Files.deleteIfExists(tmp)
        throw error
    }
}

/** A blank value must not replace a stored secret that cannot be read here, unless [cleared] says the user turned it off. */
internal fun secretWritable(
    value: String,
    storedUnreadable: Boolean,
    cleared: Boolean = false,
): Boolean = value.isNotBlank() || !storedUnreadable || cleared

private fun textOr(
    props: Properties,
    key: String,
    fallback: String,
): String = if (props.containsKey(key)) props.getProperty(key).orEmpty().trim() else fallback

private fun boolOr(
    props: Properties,
    key: String,
    fallback: Boolean,
): Boolean = props.getProperty(key)?.toBooleanStrictOrNull() ?: fallback

private fun proxyFlag(
    props: Properties,
    fallback: Boolean,
): Boolean? {
    if (!props.containsKey("proxyOn")) return fallback
    val raw = props.getProperty("proxyOn").orEmpty()
    if (raw.isEmpty()) return null
    return raw.toBooleanStrictOrNull() ?: fallback
}
