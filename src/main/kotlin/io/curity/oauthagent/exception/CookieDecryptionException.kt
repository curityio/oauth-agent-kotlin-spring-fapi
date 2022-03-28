package io.curity.oauthagent.exception

class CookieDecryptionException(cause: Throwable?) : OAuthAgentException(
    "Access denied due to invalid request details",
    null,
    401,
    "unauthorized_request",
    "A received cookie failed decryption"
)