package io.curity.bff.exception

class InvalidRequestException(message: String) : BFFException(
    message,
    null,
    401,
    "invalid_request",
    null
)
