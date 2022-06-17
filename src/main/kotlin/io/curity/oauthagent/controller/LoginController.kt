package io.curity.oauthagent.controller

import io.curity.oauthagent.*
import io.curity.oauthagent.exception.AuthorizationResponseException
import io.curity.oauthagent.exception.CookieDecryptionException
import io.curity.oauthagent.exception.InvalidResponseJwtException
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.jwt.consumer.JwtConsumer
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder

@RestController
@CrossOrigin
@RequestMapping("/\${oauthagent.endpointsPrefix}/login")
class LoginController(
    private val cookieName: CookieName,
    private val cookieEncrypter: CookieEncrypter,
    private val authorizationServerClient: AuthorizationServerClient,
    private val requestValidator: RequestValidator,
    private val jwtConsumer: JwtConsumer,
    private val oAuthParametersProvider: OAuthParametersProvider
)
{
    @PostMapping("/start", consumes = ["application/json"])
    suspend fun startLogin(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        @RequestBody(required = false) body: StartAuthorizationParameters?
    ): StartAuthorizationResponse
    {
        requestValidator.validateServletRequest(
            request,
            ValidateRequestOptions(requireCsrfHeader = false)
        )

        val authorizationRequestData = getAuthorizationURL(body)

        val encryptedCookieValue =
            cookieEncrypter.getEncryptedCookie(cookieName.tempLoginData, authorizationRequestData.toJSONString())

        response.headers[SET_COOKIE] = encryptedCookieValue

        return StartAuthorizationResponse(authorizationRequestData.authorizationRequestUrl!!)
    }

    @PostMapping("/end", consumes = ["application/json"])
    suspend fun handlePageLoad(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        @RequestBody body: EndAuthorizationRequest
    ): EndAuthorizationResponse
    {
        requestValidator.validateServletRequest(
            request,
            ValidateRequestOptions(requireCsrfHeader = false)
        )

        val queryParams = getOAuthQueryParams(body.pageUrl)
        val isOAuthResponse = queryParams.state != null && queryParams.code != null

        val isLoggedIn: Boolean
        var csrfToken: String? = null

        if (isOAuthResponse)
        {
            val tempLoginData = request.getCookie(cookieName.tempLoginData)
            val tokenResponse =
                authorizationServerClient.getTokens(tempLoginData, queryParams.code!!, queryParams.state!!)

            val csrfCookie = request.getCookie(cookieName.csrf)
            csrfToken = if (csrfCookie == null)
            {
                generateRandomString()
            } else
            {
                try {
                    // Avoid setting a new value if the user opens two browser tabs and signs in on both
                    cookieEncrypter.decryptValueFromCookie(csrfCookie)

                } catch (exception: CookieDecryptionException) {

                    // If the system has been redeployed with a new cookie encryption key, decrypting old cookies from the browser will fail
                    // In this case generate a new CSRF token so that the SPA can complete its login without errors
                    generateRandomString()
                }
            }

            // Write the SameSite cookies
            val cookiesToSet = authorizationServerClient.getCookiesForTokenResponse(tokenResponse, true, csrfToken)
            response.headers[SET_COOKIE] = cookiesToSet

            isLoggedIn = true
        } else
        {
            // See if we have a session cookie
            isLoggedIn = request.getCookie(cookieName.accessToken) != null

            if (isLoggedIn)
            {
                // During an authenticated page refresh or opening a new browser tab, we must return the anti forgery token
                // This enables an XSS attack to get the value, but this is standard for CSRF tokens
                csrfToken = cookieEncrypter.decryptValueFromCookie(request.getCookie(cookieName.csrf)!!)
            }
        }

        return EndAuthorizationResponse(
            isOAuthResponse,
            isLoggedIn,
            csrfToken
        )
    }

    private suspend fun getAuthorizationURL(parameters: StartAuthorizationParameters?): AuthorizationRequestData
    {
        val codeVerifier = oAuthParametersProvider.getCodeVerifier()
        val state = oAuthParametersProvider.getState()

        val authorizationRequestUrl = authorizationServerClient.getAuthorizationRequestObjectUri(state, codeVerifier, parameters)

        return AuthorizationRequestData(
            authorizationRequestUrl,
            codeVerifier,
            state
        )
    }

    private fun getOAuthQueryParams(pageUrl: String?): OAuthQueryParams
    {
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

    private fun ServerHttpRequest.getCookie(cookieName: String): String? =
        this.cookies[cookieName]?.first()?.value

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

data class OAuthQueryParams(val code: String?, val state: String?)

class StartAuthorizationParameters(
    val extraParams: List<ExtraParams>?
)

class StartAuthorizationResponse(
    val authorizationRequestUrl: String
)

class EndAuthorizationResponse(
    val handled: Boolean,
    val isLoggedIn: Boolean,
    val csrf: String?
)

class EndAuthorizationRequest(
    val pageUrl: String?
)
