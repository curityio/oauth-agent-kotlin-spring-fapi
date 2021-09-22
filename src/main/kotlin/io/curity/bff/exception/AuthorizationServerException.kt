package io.curity.bff.exception

class AuthorizationServerException(logMessage: String) : BFFException(
    "A problem occurred with a request to the Authorization Server",
    null,
    502,
    "authorization_server_error",
    logMessage
)
