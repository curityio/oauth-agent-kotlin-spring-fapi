package io.curity.bff

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.curity.bff.exception.AuthorizationServerException
import io.curity.bff.exception.InvalidRequestException
import io.curity.bff.exception.InvalidStateException
import io.curity.bff.exception.MissingTempLoginDataException
import io.curity.bff.exception.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono
import java.lang.RuntimeException
import java.time.Duration
import java.util.Base64

@Service
class AuthorizationServerClient(private val client: WebClient, private val objectMapper: ObjectMapper, private val cookieEncrypter: CookieEncrypter, private val config: BFFConfiguration, private val cookieName: CookieName) {
    private val idTokenOptions = config.cookieSerializeOptions.copy(path = "${config.bffEndpointsPrefix}/userInfo")
    private val refreshTokenOptions = config.cookieSerializeOptions.copy(path = "${config.bffEndpointsPrefix}/refresh")

    fun getTokens(tempLoginData: String?, code: String, state: String): TokenResponse {
        if (tempLoginData == null) {
            throw MissingTempLoginDataException()
        }

        val loginData: AuthorizationRequestData?

        try
        {
            loginData = objectMapper.readValue(
                cookieEncrypter.decryptValueFromCookie(tempLoginData),
                AuthorizationRequestData::class.java
            )
        } catch (exception: RuntimeException) {
            throw InvalidRequestException("Cookie value can't be decrypted or deserialized")
        }

        if (loginData != null && loginData.state != state) {
            throw InvalidStateException()
        }

        // @TODO: mutual TLS
        return client.post()
            .uri(config.tokenEndpoint)
            .header(
                "Authorization",
                "Basic ${String(Base64.getEncoder().encode("${config.clientID}:${config.clientSecret}".toByteArray()))}"
            )
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue("grant_type=authorization_code&redirect_uri=${config.redirectUri}&code=${code}&code_verifier=${loginData.codeVerifier}")
            .exchangeToMono { response ->

                val bodyText = response.bodyToMono(String::class.java)

                if (response.statusCode().is5xxServerError)
                {
                    return@exchangeToMono response.createException().map { exception ->
                        throw AuthorizationServerException("Server error response in an Authorization Code Grant: ${exception.responseBodyAsString}")
                    }
                }

                if (response.statusCode().is4xxClientError)
                {
                    return@exchangeToMono response.createException().map { exception ->
                        throw UnauthorizedException("Authorization Code Grant request was rejected: ${exception.responseBodyAsString}")
                    }
                }

                bodyText
            }
            .map { objectMapper.readValue(it, TokenResponse::class.java) }
            .block()
            ?: throw AuthorizationServerException("Connectivity problem during an Authorization Code Grant")
    }

    fun refreshAccessToken(refreshToken: String): TokenResponse {
        // @TODO: mutual TLS
        return client.post()
            .uri(config.tokenEndpoint)
            .header("Authorization", "Basic ${String(Base64.getEncoder().encode("${config.clientID}:${config.clientSecret}".toByteArray()))}")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue("grant_type=refresh_token&refresh_token=$refreshToken")
            .exchangeToMono { response ->
                val bodyText = response.bodyToMono(String::class.java)

                if (response.statusCode().is5xxServerError) {
                    return@exchangeToMono response.createException().map { exception ->
                        throw AuthorizationServerException("Server error response in a Refresh Token Grant: ${exception.responseBodyAsString}")
                    }
                }

                if (response.statusCode().is4xxClientError) {
                    return@exchangeToMono response.createException().map { exception ->
                        throw UnauthorizedException("Refresh Token Grant request was rejected: ${exception.responseBodyAsString}")
                    }
                }

                bodyText
            }.map { objectMapper.readValue(it, TokenResponse::class.java) }
            .block()
            ?: throw AuthorizationServerException("Connectivity problem during an Refresh Token Grant")
    }

    fun getCookiesForTokenResponse(response: TokenResponse, unsetTempLoginDataCookie: Boolean, csrfCookieValue: String?): List<String> {
        val cookiesList = mutableListOf<String>()
        cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.accessToken, response.accessToken))

        if (csrfCookieValue != null) {
            cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.csrf, csrfCookieValue))
        }

        if (unsetTempLoginDataCookie) {
            cookiesList.add(cookieEncrypter.getCookieForUnset(cookieName.tempLoginData))
        }

        if (response.refreshToken != null) {
            cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.auth, response.refreshToken, refreshTokenOptions))
        }

        if (response.idToken != null) {
            cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.idToken, response.idToken, idTokenOptions))
        }

        return cookiesList
    }
}


class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("refresh_token") val refreshToken: String?,
    @JsonProperty("id_token") val idToken: String?
)
