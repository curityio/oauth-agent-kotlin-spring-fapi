package io.curity.oauthagent.exception

class InvalidRequestException(message: String, cause: Throwable?, logMessage: String?) : OAuthAgentException(
    message,
    cause,
    401,
    "invalid_request",
    logMessage
)
{
    constructor(message: String) : this(message, null, null)
}
