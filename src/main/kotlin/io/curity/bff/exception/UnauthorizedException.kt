package io.curity.bff.exception

class UnauthorizedException(logMessage: String) : BFFException(
    "Access denied due to invalid request details",
    null,
    401,
    "unauthorized_request",
    logMessage
)
