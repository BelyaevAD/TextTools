package dev.belyaevad.texttools.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import dev.belyaevad.texttools.domain.ErrorReason
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.error_empty_response
import texttools.app.generated.resources.error_empty_url
import texttools.app.generated.resources.error_http
import texttools.app.generated.resources.error_insufficient_funds
import texttools.app.generated.resources.error_invalid_proxy
import texttools.app.generated.resources.error_invalid_url
import texttools.app.generated.resources.error_not_found
import texttools.app.generated.resources.error_provider_unavailable
import texttools.app.generated.resources.error_rate_limited
import texttools.app.generated.resources.error_secret_store
import texttools.app.generated.resources.error_unauthorized
import texttools.app.generated.resources.error_unknown

/** A message resolved only while composing, so a language change updates it. */
@Stable
data class UiText(
    val resource: StringResource,
    val args: List<Any> = emptyList(),
)

fun uiText(
    resource: StringResource,
    vararg args: Any,
): UiText = UiText(resource, args.toList())

@Composable
fun resolve(value: UiText): String = stringResource(value.resource, *value.args.toTypedArray())

internal val ErrorReason.resource: StringResource
    get() =
        when (this) {
            ErrorReason.EMPTY_RESPONSE -> Res.string.error_empty_response
            ErrorReason.EMPTY_URL -> Res.string.error_empty_url
            ErrorReason.INVALID_URL -> Res.string.error_invalid_url
            ErrorReason.INVALID_PROXY -> Res.string.error_invalid_proxy
            ErrorReason.PROVIDER_UNAVAILABLE -> Res.string.error_provider_unavailable
            ErrorReason.UNAUTHORIZED -> Res.string.error_unauthorized
            ErrorReason.INSUFFICIENT_FUNDS -> Res.string.error_insufficient_funds
            ErrorReason.NOT_FOUND -> Res.string.error_not_found
            ErrorReason.RATE_LIMITED -> Res.string.error_rate_limited
            ErrorReason.HTTP_ERROR -> Res.string.error_http
            ErrorReason.SECRET_STORE_UNAVAILABLE -> Res.string.error_secret_store
            ErrorReason.UNKNOWN -> Res.string.error_unknown
        }

@Composable
fun resolveError(
    reason: ErrorReason,
    status: Int? = null,
): String =
    if (reason == ErrorReason.HTTP_ERROR) {
        stringResource(reason.resource, status?.toString().orEmpty())
    } else {
        stringResource(reason.resource)
    }
