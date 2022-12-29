package io.curity.oauthagent.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.curity.oauthagent.*
import io.curity.oauthagent.exception.CookieDecryptionException
import io.curity.oauthagent.exception.InvalidRequestException
import io.curity.oauthagent.exception.InvalidStateException
import io.curity.oauthagent.exception.MissingTempLoginDataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
@RequestMapping("/\${oauthagent.endpointsPrefix}/login")
class LoginController(
        private val loginHandler: LoginHandler,
        private val cookieName: CookieName,
        private val cookieEncrypter: CookieEncrypter,
        private val cookieBuilder: CookieBuilder,
        private val objectMapper: ObjectMapper,
        private val authorizationServerClient: AuthorizationServerClient,
        private val requestValidator: RequestValidator
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

        val authorizationRequestData = loginHandler.createAuthorizationRequest(body)

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

        val queryParams = loginHandler.handleAuthorizationResponse(body.pageUrl)
        val isOAuthResponse = queryParams.state != null && queryParams.code != null

        val isLoggedIn: Boolean
        var csrfToken: String? = null

        if (isOAuthResponse)
        {
            val authorizationData = getStoredAuthorizationData(request)
            if (authorizationData.state != queryParams.state) {
                throw InvalidStateException()
            }

            val tokenResponse =
                authorizationServerClient.redeemCodeForTokens(queryParams.code!!, authorizationData.codeVerifier)

            val csrfCookie = request.getCookie(cookieName.csrf)
            csrfToken = if (csrfCookie == null)
            {
                generateRandomString()
            } else {

                try {
                    // Avoid setting a new value if the user opens two browser tabs and signs in on both
                    cookieEncrypter.decryptValueFromCookie(csrfCookie)

                } catch (exception: CookieDecryptionException) {

                    // If the system has been redeployed with a new cookie encryption key, decrypting old cookies from the browser will fail
                    // In this case generate a new CSRF token so that the SPA can complete its login without errors
                    generateRandomString()
                }
            }

            val cookiesToSet = cookieBuilder.createCookies(tokenResponse, csrfToken)
            response.headers[SET_COOKIE] = cookiesToSet
            isLoggedIn = true

        } else
        {
            isLoggedIn = request.getCookie(cookieName.accessToken) != null
            if (isLoggedIn)
            {
                // During a page reload, we must return the existing anti forgery token
                csrfToken = cookieEncrypter.decryptValueFromCookie(request.getCookie(cookieName.csrf)!!)
            }
        }

        return EndAuthorizationResponse(
            isOAuthResponse,
            isLoggedIn,
            csrfToken
        )
    }

    private suspend fun getStoredAuthorizationData(request: ServerHttpRequest): AuthorizationRequestData {

        val tempLoginData = request.getCookie(cookieName.tempLoginData) ?: throw MissingTempLoginDataException()

        return withContext(Dispatchers.Default) {
            val result = kotlin.runCatching {
                return@runCatching objectMapper.readValue(
                        cookieEncrypter.decryptValueFromCookie(tempLoginData),
                        AuthorizationRequestData::class.java
                )
            }

            result.getOrNull() ?: throw InvalidRequestException("Cookie value can't be decrypted or deserialized")
        }
    }

    private fun ServerHttpRequest.getCookie(cookieName: String): String? =
        this.cookies[cookieName]?.first()?.value
}

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
