package io.curity.oauthagent.exception

class MissingTempLoginDataException : OAuthAgentException(
    "Missing code verifier when completing a login",
    null,
    400,
    "invalid_request",
    null
)
