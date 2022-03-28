package io.curity.oauthagent.exception

class UnauthorizedException(logMessage: String) : OAuthAgentException(
    "Access denied due to invalid request details",
    null,
    401,
    "unauthorized_request",
    logMessage
)
