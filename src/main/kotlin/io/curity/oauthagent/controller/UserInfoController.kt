package io.curity.oauthagent.controller

import io.curity.oauthagent.*
import io.curity.oauthagent.exception.InvalidCookieException
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/\${oauthagent.endpointsPrefix}/userInfo")
class UserInfoController(
    private val requestValidator: RequestValidator,
    private val cookieEncrypter: CookieEncrypter,
    private val cookieName: CookieName,
    private val authorizationServerClient: AuthorizationServerClient
)
{
    @GetMapping("", produces = ["application/json"])
    suspend fun getUserInfo(request: ServerHttpRequest): Map<String, Any>
    {
        requestValidator.validateServletRequest(
            request,
            ValidateRequestOptions(requireCsrfHeader = false)
        )

        val accessTokenCookie = request.cookies[cookieName.accessToken]?.first()?.value
            ?: throw InvalidCookieException("No access token cookie was supplied in a call to get user info")
        val decryptedToken = cookieEncrypter.decryptValueFromCookie(accessTokenCookie)
        return authorizationServerClient.getUserInfo(decryptedToken)
    }
}
