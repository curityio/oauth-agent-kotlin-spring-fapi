package io.curity.bff.exception

class MissingTempLoginDataException : BFFException(
    "Missing code verifier when completing a login",
    null,
    400,
    "invalid_request",
    null
)
