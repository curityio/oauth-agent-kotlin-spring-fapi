package io.curity.oauthagent.exception

class InvalidResponseJwtException(cause: Throwable?) : OAuthAgentException(
    "Response JWT is invalid",
    cause,
    400,
    "invalid_request",
    "The received response JWT is not valid. See attached exception for details."
)
