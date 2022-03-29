package io.curity.oauthagent.exception

sealed class OAuthAgentException(
    message: String,
    cause: Throwable?,
    val statusCode: Int,
    val code: String,
    val logMessage: String?
) : RuntimeException(message, cause)
