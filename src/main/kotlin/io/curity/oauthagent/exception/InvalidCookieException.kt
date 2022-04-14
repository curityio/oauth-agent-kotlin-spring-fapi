package io.curity.oauthagent.exception

class InvalidCookieException(logMessage: String, cause: Throwable?) : OAuthAgentException(
    "Access denied due to invalid request details",
    cause,
    401,
    "unauthorized_request",
    logMessage
)
{
    constructor(logMessage: String) : this(logMessage, null)
}
