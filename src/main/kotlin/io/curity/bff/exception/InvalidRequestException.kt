package io.curity.bff.exception

class InvalidRequestException(message: String, cause: Throwable?, logMessage: String?) : BFFException(
    message,
    cause,
    401,
    "invalid_request",
    logMessage
)
{
    constructor(message: String) : this(message, null, null)
}
