package dev.belyaevad.texttools.domain

/** Machine-readable failure. UI translations belong to the app module. */
enum class ErrorReason {
    EMPTY_RESPONSE,
    EMPTY_URL,
    INVALID_URL,
    INVALID_PROXY,
    PROVIDER_UNAVAILABLE,
    UNAUTHORIZED,
    INSUFFICIENT_FUNDS,
    NOT_FOUND,
    RATE_LIMITED,
    HTTP_ERROR,
    SECRET_STORE_UNAVAILABLE,
    UNKNOWN,
}

/** Technical details are never request text or credentials. */
class AppException(
    val reason: ErrorReason,
    val httpStatus: Int? = null,
    technicalMessage: String = reason.name,
    cause: Throwable? = null,
) : IllegalArgumentException(technicalMessage, cause)

fun Throwable.errorReason(): ErrorReason = (this as? AppException)?.reason ?: ErrorReason.UNKNOWN

fun Throwable.errorHttpStatus(): Int? = (this as? AppException)?.httpStatus
