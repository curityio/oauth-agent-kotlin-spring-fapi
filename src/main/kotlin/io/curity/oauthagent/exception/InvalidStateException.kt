package io.curity.oauthagent.exception

class InvalidStateException : OAuthAgentException(
    "State parameter mismatch when completing a login",
    null,
    400,
    "invalid_request",
    null
)
