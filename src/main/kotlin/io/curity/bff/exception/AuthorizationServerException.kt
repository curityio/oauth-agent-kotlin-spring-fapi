package io.curity.bff.exception

class AuthorizationServerException(logMessage: String, cause: Throwable?) : BFFException(
    "A problem occurred with a request to the Authorization Server",
    cause,
    502,
    "authorization_server_error",
    logMessage
)
{
    constructor(logMessage: String) : this(logMessage, null)
}
