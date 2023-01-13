package io.curity.oauthagent.exception

import org.springframework.web.server.ResponseStatusException

fun getExceptionStatusCode(ex: Throwable): Int {

    // If Spring has thrown the error, look for a response status, such as for malformed JSON input
    if (ex is ResponseStatusException) {
        return ex.statusCode.value()
    }

    // Default to 500 if we cannot determine an HTTP status
    return 500
}

class UnhandledException(ex: Throwable) : OAuthAgentException(
    "A technical problem occurred in the OAuth Agent",
    ex,
    getExceptionStatusCode(ex),
    "server_error",
    ex.message
)