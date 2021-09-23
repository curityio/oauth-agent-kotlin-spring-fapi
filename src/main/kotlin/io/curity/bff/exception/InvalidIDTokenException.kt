package io.curity.bff.exception

class InvalidIDTokenException(cause: Throwable?) : BFFException(
    "ID Token missing or invalid",
    cause,
    400,
    "invalid_request",
    null
)
{
    constructor() : this(null)
}
