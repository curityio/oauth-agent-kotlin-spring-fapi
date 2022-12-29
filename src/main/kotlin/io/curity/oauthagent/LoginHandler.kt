package io.curity.oauthagent

import io.curity.oauthagent.controller.StartAuthorizationParameters
import io.curity.oauthagent.exception.AuthorizationResponseException
import io.curity.oauthagent.exception.InvalidResponseJwtException
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.jwt.consumer.JwtConsumer
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class LoginHandler(
        private val authorizationServerClient: AuthorizationServerClient,
        private val oAuthParametersProvider: OAuthParametersProvider,
        private val jwtConsumer: JwtConsumer
) {

    suspend fun createAuthorizationRequest(parameters: StartAuthorizationParameters?): AuthorizationRequestData {

        val codeVerifier = oAuthParametersProvider.getCodeVerifier()
        val state = oAuthParametersProvider.getState()

        val authorizationRequestUrl = authorizationServerClient.getAuthorizationRequestObjectUri(state, codeVerifier, parameters)

        return AuthorizationRequestData(
                authorizationRequestUrl,
                codeVerifier,
                state
        )
    }

    suspend fun handleAuthorizationResponse(pageUrl: String?): OAuthQueryParams {

        if (pageUrl == null)
        {
            return OAuthQueryParams(null, null)
        }

        val queryParams = UriComponentsBuilder.fromUriString(pageUrl).build().queryParams

        if (queryParams["response"] == null)
        {
            return OAuthQueryParams(null, null)
        }

        try
        {
            val responseClaims = jwtConsumer.processToClaims(queryParams["response"]!!.first())
            val code = responseClaims.getStringClaimValue("code")
            val state = responseClaims.getStringClaimValue("state")
            if (!code.isNullOrBlank() && !state.isNullOrBlank()) {
                return OAuthQueryParams(code, state)
            }

            throw getAuthorizationResponseError(responseClaims)

        } catch (exception: InvalidJwtException)
        {
            throw InvalidResponseJwtException(exception)
        }
    }

    private fun getAuthorizationResponseError(responseClaims: JwtClaims): AuthorizationResponseException {

        var errorCode = responseClaims.getStringClaimValue("error")
        var errorDescription = responseClaims.getStringClaimValue("error_description")
        if (errorCode.isNullOrBlank()) {
            errorCode = "authorization_response_error"
        }
        if (errorDescription.isNullOrBlank()) {
            errorDescription = "Login failed at the Authorization Server"
        }

        return AuthorizationResponseException(errorCode, errorDescription)
    }
}
