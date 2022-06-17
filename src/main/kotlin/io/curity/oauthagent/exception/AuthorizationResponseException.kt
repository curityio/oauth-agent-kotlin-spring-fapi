package io.curity.oauthagent.exception

class AuthorizationResponseException(error: String, description: String) : OAuthAgentException(
    description,
    null,
    502,
    error,
    ""
)