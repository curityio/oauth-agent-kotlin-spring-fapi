package io.curity.oauthagent

import org.springframework.stereotype.Service

@Service
class CookieName(private val config: OAuthAgentConfiguration)
{
    val tempLoginData = "${config.cookieNamePrefix}-login"
    final val auth = "${config.cookieNamePrefix}-auth"
    final val accessToken = "${config.cookieNamePrefix}-at"
    final val idToken = "${config.cookieNamePrefix}-id"
    final val csrf = "${config.cookieNamePrefix}-csrf"

    val cookiesForUnset =
        listOf(auth, accessToken, idToken, csrf)
}
