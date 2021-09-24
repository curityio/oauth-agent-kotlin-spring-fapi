package io.curity.bff

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.curity.bff.exception.AuthorizationServerException
import io.curity.bff.exception.InvalidRequestException
import io.curity.bff.exception.InvalidStateException
import io.curity.bff.exception.MissingTempLoginDataException
import io.curity.bff.exception.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.Base64

@Service
class AuthorizationServerClient(
    private val client: WebClient,
    private val objectMapper: ObjectMapper,
    private val cookieEncrypter: CookieEncrypter,
    private val config: BFFConfiguration,
    private val cookieName: CookieName
)
{
    private val idTokenOptions = config.cookieSerializeOptions.copy(path = "${config.bffEndpointsPrefix}/userInfo")
    private val refreshTokenOptions = config.cookieSerializeOptions.copy(path = "${config.bffEndpointsPrefix}/refresh")

    fun getTokens(tempLoginData: String?, code: String, state: String): TokenResponse
    {
        if (tempLoginData == null)
        {
            throw MissingTempLoginDataException()
        }

        val loginData: AuthorizationRequestData?

        try
        {
            loginData = objectMapper.readValue(
                cookieEncrypter.decryptValueFromCookie(tempLoginData),
                AuthorizationRequestData::class.java
            )
        } catch (exception: RuntimeException)
        {
            throw InvalidRequestException("Cookie value can't be decrypted or deserialized")
        }

        if (loginData != null && loginData.state != state)
        {
            throw InvalidStateException()
        }

        return client.post()
            .uri(config.tokenEndpoint)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue("grant_type=authorization_code&redirect_uri=${config.redirectUri}&code=${code}&code_verifier=${loginData.codeVerifier}")
            .exchangeToMono { response -> handleAuthorizationServerResponse(response, "Authorization Code Grant") }
            .map { objectMapper.readValue(it, TokenResponse::class.java) }
            .block()
            ?: throw AuthorizationServerException("Connectivity problem during an Authorization Code Grant")
    }

    private fun handleAuthorizationServerResponse(response: ClientResponse, grant: String): Mono<String>
    {
        if (response.statusCode().is5xxServerError)
        {
            return response.createException().map { exception ->
                throw AuthorizationServerException("Server error response in $grant: ${exception.responseBodyAsString}")
            }
        }

        if (response.statusCode().is4xxClientError)
        {
            return response.createException().map { exception ->
                throw UnauthorizedException("$grant request was rejected: ${exception.responseBodyAsString}")
            }
        }

        return response.bodyToMono(String::class.java)
    }

    fun refreshAccessToken(refreshToken: String): TokenResponse
    {
        return client.post()
            .uri(config.tokenEndpoint)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue("grant_type=refresh_token&refresh_token=$refreshToken")
            .exchangeToMono { response -> handleAuthorizationServerResponse(response, "Refresh Token Grant") }
            .map { objectMapper.readValue(it, TokenResponse::class.java) }
            .block()
            ?: throw AuthorizationServerException("Connectivity problem during an Refresh Token Grant")
    }

    fun getCookiesForTokenResponse(
        response: TokenResponse,
        unsetTempLoginDataCookie: Boolean,
        csrfCookieValue: String?
    ): List<String>
    {
        val cookiesList = mutableListOf<String>()
        cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.accessToken, response.accessToken))

        if (csrfCookieValue != null)
        {
            cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.csrf, csrfCookieValue))
        }

        if (unsetTempLoginDataCookie)
        {
            cookiesList.add(cookieEncrypter.getCookieForUnset(cookieName.tempLoginData))
        }

        if (response.refreshToken != null)
        {
            cookiesList.add(
                cookieEncrypter.getEncryptedCookie(
                    cookieName.auth,
                    response.refreshToken,
                    refreshTokenOptions
                )
            )
        }

        if (response.idToken != null)
        {
            cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.idToken, response.idToken, idTokenOptions))
        }

        return cookiesList
    }

    fun getAuthorizationRequestObjectUri(state: String, codeVerifier: String): String
    {
        var body =
            "client_id=${config.clientID}&state=${state}&response_mode=jwt&response_type=code&redirect_uri=${config.redirectUri}&code_challenge=${codeVerifier.hash()}&code_challenge_method=S256"

        if (config.scope != null)
        {
            body += "&scope=${config.scope}"
        }

        val parResponse = client.post()
            .uri(config.authorizeEndpoint + "/par")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue(body)
            .exchangeToMono { response -> handleAuthorizationServerResponse(response, "PAR") }
            .map { objectMapper.readValue(it, PARResponse::class.java) }
            .block()
            ?: throw AuthorizationServerException("Connectivity problem during PAR request")

        return "${config.authorizeEndpoint}?client_id=${config.clientID}&request_uri=${parResponse.requestUri}"
    }
}

class PARResponse(
    @JsonProperty("request_uri") val requestUri: String,
    @JsonProperty("expires_in") val expiresIn: Int
)

class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("refresh_token") val refreshToken: String?,
    @JsonProperty("id_token") val idToken: String?
)
