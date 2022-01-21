package io.curity.bff.exception


class InvalidBFFCookieException(logMessage: String, cause: Throwable?) : BFFException(
    "Access denied due to invalid request details",
    null,
    401,
    "unauthorized_request",
    logMessage
)
{
    constructor(logMessage: String) : this(logMessage, null)
}
