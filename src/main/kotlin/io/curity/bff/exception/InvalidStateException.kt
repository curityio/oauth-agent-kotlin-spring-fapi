package io.curity.bff.exception

class InvalidStateException : BFFException(
    "State parameter mismatch when completing a login",
    null,
    400,
    "invalid_request",
    null
)
