package io.curity.bff.controller

import io.curity.bff.AuthorizationRequestData
import io.curity.bff.AuthorizationServerClient
import io.curity.bff.BFFConfiguration
import io.curity.bff.CookieEncrypter
import io.curity.bff.CookieName
import io.curity.bff.RequestValidator
import io.curity.bff.ValidateRequestOptions
import io.curity.bff.exception.InvalidResponseJwtException
import io.curity.bff.generateRandomString
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.jwt.consumer.JwtConsumer
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.WebUtils
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
@CrossOrigin
@RequestMapping("/\${bff.bffEndpointsPrefix}/login")
class LoginController(
    private val cookieName: CookieName,
    private val cookieEncrypter: CookieEncrypter,
    private val authorizationServerClient: AuthorizationServerClient,
    private val requestValidator: RequestValidator,
    private val jwtConsumer: JwtConsumer
)
{
    @PostMapping("/start")
    fun startLogin(request: HttpServletRequest, response: HttpServletResponse): StartAuthorizationResponse
    {

        requestValidator.validateServletRequest(
            request,
            ValidateRequestOptions(requireCsrfHeader = false)
        )

        val authorizationRequestData = getAuthorizationURL()

        val encryptedCookieValue =
            cookieEncrypter.getEncryptedCookie(cookieName.tempLoginData, authorizationRequestData.toJSONString())

        response.addHeader("Set-Cookie", encryptedCookieValue)

        return StartAuthorizationResponse(authorizationRequestData.authorizationRequestUrl!!)
    }

    @PostMapping("/end", consumes = ["application/json"])
    fun handlePageLoad(
        request: HttpServletRequest,
        response: HttpServletResponse,
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

            // Avoid setting a new value if the user opens two browser tabs and signs in on both
            val csrfCookie = request.getCookie(cookieName.csrf)
            csrfToken = if (csrfCookie == null)
            {
                generateRandomString()
            } else
            {
                cookieEncrypter.decryptValueFromCookie(csrfCookie)
            }

            // Write the SameSite cookies
            val cookiesToSet = authorizationServerClient.getCookiesForTokenResponse(tokenResponse, true, csrfToken)
            cookiesToSet.forEach {
                response.addHeader("Set-Cookie", it)
            }

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

    private fun getAuthorizationURL(): AuthorizationRequestData
    {
        val codeVerifier = generateRandomString()
        val state = generateRandomString()

        val authorizationRequestUrl = authorizationServerClient.getAuthorizationRequestObjectUri(state, codeVerifier)

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
            return OAuthQueryParams(
                responseClaims.getStringClaimValue("code"),
                responseClaims.getStringClaimValue("state")
            )
        } catch (exception: InvalidJwtException)
        {
            throw InvalidResponseJwtException(exception)
        }
    }

    fun HttpServletRequest.getCookie(cookieName: String): String? =
        WebUtils.getCookie(this, cookieName)?.value
}

data class OAuthQueryParams(val code: String?, val state: String?)

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
