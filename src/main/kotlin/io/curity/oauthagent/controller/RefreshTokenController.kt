package io.curity.oauthagent.controller

import io.curity.oauthagent.AuthorizationServerClient
import io.curity.oauthagent.CookieEncrypter
import io.curity.oauthagent.CookieName
import io.curity.oauthagent.RequestValidator
import io.curity.oauthagent.ValidateRequestOptions
import io.curity.oauthagent.exception.InvalidCookieException
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/\${oauthagent.endpointsPrefix}/refresh")
class RefreshTokenController(
    private val requestValidator: RequestValidator,
    private val cookieEncrypter: CookieEncrypter,
    private val cookieName: CookieName,
    private val authorizationServerClient: AuthorizationServerClient
)
{
    @PostMapping("", produces = ["application/json"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun refreshTokenFromCookie(request: ServerHttpRequest, response: ServerHttpResponse)
    {
        requestValidator.validateServletRequest(request, ValidateRequestOptions())

        val refreshTokenCookie = request.cookies[cookieName.auth]?.first()?.value
            ?: throw InvalidCookieException("No auth cookie was supplied in a token refresh call")

        val decryptedToken = cookieEncrypter.decryptValueFromCookie(refreshTokenCookie)
        val tokenResponse = authorizationServerClient.refreshAccessToken(decryptedToken)
        val cookiesToSet = authorizationServerClient.getCookiesForTokenResponse(tokenResponse, false, null)

        response.headers[SET_COOKIE] = cookiesToSet
    }
}
