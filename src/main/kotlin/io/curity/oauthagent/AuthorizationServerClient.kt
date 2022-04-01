package io.curity.oauthagent

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.curity.oauthagent.exception.AuthorizationServerException
import io.curity.oauthagent.exception.InvalidRequestException
import io.curity.oauthagent.exception.InvalidStateException
import io.curity.oauthagent.exception.MissingTempLoginDataException
import io.curity.oauthagent.exception.UnauthorizedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@Service
class AuthorizationServerClient(
    private val client: WebClient,
    private val objectMapper: ObjectMapper,
    private val cookieEncrypter: CookieEncrypter,
    private val config: OAuthAgentConfiguration,
    private val cookieName: CookieName
)
{
    private val idTokenOptions = config.cookieSerializeOptions.copy(path = "/${config.endpointsPrefix}/claims")
    private val refreshTokenOptions = config.cookieSerializeOptions.copy(path = "/${config.endpointsPrefix}/refresh")

    suspend fun getTokens(tempLoginData: String?, code: String, state: String): TokenResponse
    {
        if (tempLoginData == null)
        {
            throw MissingTempLoginDataException()
        }

        val loginData = withContext(Dispatchers.Default) {
            val result = kotlin.runCatching {
                return@runCatching objectMapper.readValue(
                    cookieEncrypter.decryptValueFromCookie(tempLoginData),
                    AuthorizationRequestData::class.java
                )
            }

            result.getOrNull() ?: throw InvalidRequestException("Cookie value can't be decrypted or deserialized")
        }

        if (loginData.state != state)
        {
            throw InvalidStateException()
        }

        try
        {
            return client.post()
                .uri(config.tokenEndpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue("client_id=${config.clientID}&grant_type=authorization_code&redirect_uri=${config.redirectUri}&code=${code}&code_verifier=${loginData.codeVerifier}")
                .awaitExchange { response -> handleAuthorizationServerResponse(response, "Authorization Code Grant") }
        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Connectivity problem during an Authorization Code Grant", exception)
        }
    }

    private suspend inline fun <reified T : Any> handleAuthorizationServerResponse(
        response: ClientResponse,
        grant: String
    ): T
    {
        if (response.statusCode().is5xxServerError)
        {
            throw AuthorizationServerException("Server error response in $grant: ${response.awaitBody<String>()}")
        }

        if (response.statusCode().is4xxClientError)
        {
            throw UnauthorizedException("$grant request was rejected: ${response.awaitBody<String>()}")
        }

        return response.awaitBody()
    }

    suspend fun getUserInfo(accessToken: String): Map<String, Any>
    {
        try
        {
            return client.post()
                .uri(config.userInfoEndpoint)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .awaitExchange { response -> handleAuthorizationServerResponse(response, "User Info Download") }

        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Connectivity problem during a User Info request", exception)
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): TokenResponse
    {
        try
        {
            return client.post()
                .uri(config.tokenEndpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue("grant_type=refresh_token&refresh_token=$refreshToken&client_id=${config.clientID}")
                .awaitExchange { response -> handleAuthorizationServerResponse(response, "Refresh Token Grant") }

        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Connectivity problem during a Refresh Token Grant", exception)
        }
    }

    suspend fun getCookiesForTokenResponse(
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
                cookieEncrypter.getEncryptedCookie(cookieName.auth, response.refreshToken, refreshTokenOptions))
        }

        if (response.idToken != null)
        {
            cookiesList.add(cookieEncrypter.getEncryptedCookie(cookieName.idToken, response.idToken, idTokenOptions))
        }

        return cookiesList
    }

    suspend fun getAuthorizationRequestObjectUri(state: String, codeVerifier: String): String
    {
        var body =
            "client_id=${config.clientID}&state=${state}&response_mode=jwt&response_type=code&redirect_uri=${config.redirectUri}&code_challenge=${codeVerifier.hash()}&code_challenge_method=S256"

        if (config.scope != null)
        {
            body += "&scope=${config.scope}"
        }

        try
        {
            val parResponse = client.post()
                .uri(config.authorizeEndpoint + "/par")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(body)
                .awaitExchange { response -> handleAuthorizationServerResponse<PARResponse>(response, "PAR") }

            return "${config.authorizeExternalEndpoint}?client_id=${config.clientID}&request_uri=${parResponse.requestUri}"
        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Exception encountered when calling authorization server", exception)
        }
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
