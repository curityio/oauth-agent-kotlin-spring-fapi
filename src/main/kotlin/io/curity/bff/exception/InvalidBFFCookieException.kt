package io.curity.bff.exception


class InvalidBFFCookieException(logMessage: String, cause: Throwable?) : BFFException(
    "The session is invalid or expired",
    cause,
    401,
    "session_expired",
    logMessage
)
{
    constructor(logMessage: String) : this(logMessage, null)
}
