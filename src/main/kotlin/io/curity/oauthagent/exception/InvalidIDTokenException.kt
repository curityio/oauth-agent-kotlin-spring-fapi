package io.curity.oauthagent.exception

class InvalidIDTokenException(cause: Throwable?) : OAuthAgentException(
    "ID Token missing or invalid",
    cause,
    400,
    "invalid_request",
    null
)
{
    constructor() : this(null)
}
