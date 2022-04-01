package io.curity.oauthagent.exception

class UnhandledException(ex: Throwable) : OAuthAgentException(
    "A technical problem occurred in the OAuth Agent",
    ex,
    500,
    "server_error",
    ex.message
) {
}