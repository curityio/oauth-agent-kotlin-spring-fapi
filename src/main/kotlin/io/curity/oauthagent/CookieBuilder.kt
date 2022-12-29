package io.curity.oauthagent

import org.springframework.stereotype.Service

@Service
class CookieBuilder(
        config: OAuthAgentConfiguration,
        private val cookieEncrypter: CookieEncrypter,
        private val cookieName: CookieName
) {

    private val idTokenOptions = config.cookieSerializeOptions.copy(path = "/${config.endpointsPrefix}/claims")
    private val refreshTokenOptions = config.cookieSerializeOptions.copy(path = "/${config.endpointsPrefix}/refresh")

    suspend fun createCookies(response: TokenResponse, csrfCookieValue: String): List<String> {

        val cookiesList = mutableListOf<String>()
        cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.csrf, csrfCookieValue))
        cookiesList.add(cookieEncrypter.getCookieForUnset(cookieName.tempLoginData))
        addTokensToCookies(cookiesList, response)
        return cookiesList
    }

    suspend fun refreshCookies(response: TokenResponse): List<String> {

        val cookiesList = mutableListOf<String>()
        addTokensToCookies(cookiesList, response)
        return cookiesList
    }

    private suspend fun addTokensToCookies(cookiesList: MutableList<String>, response: TokenResponse) {

        cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.accessToken, response.accessToken))

        if (response.refreshToken != null) {
            cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.auth, response.refreshToken, refreshTokenOptions))
        }

        if (response.idToken != null) {
            cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.idToken, response.idToken, idTokenOptions))
        }
    }
}
