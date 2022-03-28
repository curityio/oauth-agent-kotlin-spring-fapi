package io.curity.oauthagent

data class AuthorizationRequestData(
    val authorizationRequestUrl: String?,
    val codeVerifier: String,
    val state: String
)
{
    fun toJSONString(): String =
        "{\"codeVerifier\": \"$codeVerifier\", \"state\": \"$state\"}"
}
