package io.curity.oauthagent

import com.fasterxml.jackson.annotation.JsonProperty
import io.curity.oauthagent.controller.StartAuthorizationParameters
import io.curity.oauthagent.exception.*
import io.curity.oauthagent.utilities.Grant
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@Service
class AuthorizationServerClient(
    private val client: WebClient,
    private val config: OAuthAgentConfiguration
) {

    suspend fun getAuthorizationRequestObjectUri(state: String, codeVerifier: String, parameters: StartAuthorizationParameters?): String
    {
        var body =
                "client_id=${config.clientID}&state=${state}&response_mode=jwt&response_type=code&redirect_uri=${config.redirectUri}&code_challenge=${codeVerifier.hash()}&code_challenge_method=S256"

        if (config.scope != null)
        {
            body += "&scope=${config.scope}"
        }

        parameters?.extraParams?.forEach {
            body += "&${it.key}=${it.value}"
        }

        try
        {
            val parResponse = client.post()
                    .uri(config.authorizeEndpoint + "/par")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(body)
                    .awaitExchange { response -> handleAuthorizationServerResponse<PARResponse>(response, Grant.PAR) }

            return "${config.authorizeExternalEndpoint}?client_id=${config.clientID}&request_uri=${parResponse.requestUri}"
        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Exception encountered when calling authorization server", exception)
        }
    }

    suspend fun redeemCodeForTokens(code: String, codeVerifier: String): TokenResponse
    {
        try
        {
            val body = "client_id=${config.clientID}&grant_type=authorization_code&redirect_uri=${config.redirectUri}&code=${code}&code_verifier=${codeVerifier}"
            return client.post()
                .uri(config.tokenEndpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(body)
                .awaitExchange { response -> handleAuthorizationServerResponse(response, Grant.AuthorizationCode) }

        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Connectivity problem during an Authorization Code Grant", exception)
        }
    }

    suspend fun getUserInfo(accessToken: String): Map<String, Any>
    {
        try
        {
            return client.post()
                .uri(config.userInfoEndpoint)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .awaitExchange { response -> handleAuthorizationServerResponse(response, Grant.UserInfo) }

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
                .awaitExchange { response -> handleAuthorizationServerResponse(response, Grant.RefreshToken) }

        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Connectivity problem during a Refresh Token Grant", exception)
        }
    }

    private suspend inline fun <reified T : Any> handleAuthorizationServerResponse(
            response: ClientResponse,
            grant: Grant
    ): T
    {
        if (response.statusCode().is5xxServerError)
        {
            val text = response.awaitBody<String>()
            throw AuthorizationServerException("Server error response in $grant: $text")
        }

        if (response.statusCode().is4xxClientError)
        {
            val text = response.awaitBody<String>()
            throw AuthorizationClientException.create(grant, response.statusCode(), text)
        }

        return response.awaitBody()
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
