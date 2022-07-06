package io.curity.oauthagent

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

fun String.encodeURI(): String
{
    return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
}

fun String.hash(): String
{
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(this.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes)
}
