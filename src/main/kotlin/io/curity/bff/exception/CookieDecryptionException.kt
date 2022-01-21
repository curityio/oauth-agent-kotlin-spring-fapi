package io.curity.bff.exception

class CookieDecryptionException(cause: Throwable?) : BFFException(
    "Access denied due to invalid request details",
    null,
    401,
    "unauthorized_request",
    "A received cookie failed decryption"
)