package io.curity.oauthagent.exception

class AuthorizationClientException() : OAuthAgentException(
    "A request sent to the Authorization Server was rejected",
    null,
    400,
    "authorization_error",
    ""
)