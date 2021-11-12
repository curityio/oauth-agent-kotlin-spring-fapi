package io.curity.bff.controller

import io.curity.bff.BFFConfiguration
import io.curity.bff.CookieEncrypter
import io.curity.bff.CookieName
import io.curity.bff.RequestValidator
import io.curity.bff.ValidateRequestOptions
import io.curity.bff.encodeURI
import io.curity.bff.exception.InvalidBFFCookieException
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/\${bff.bffEndpointsPrefix}/logout")
class LogoutController(
    private val requestValidator: RequestValidator,
    private val cookieName: CookieName,
    private val cookieEncrypter: CookieEncrypter,
    private val config: BFFConfiguration
)
{
    @PostMapping("", produces = ["application/json"])
    suspend fun logoutUser(request: ServerHttpRequest, response: ServerHttpResponse): LogoutUserResponse
    {
        requestValidator.validateServletRequest(request, ValidateRequestOptions())

        if (request.cookies[cookieName.accessToken]?.isNotEmpty() == true)
        {
            response.headers[SET_COOKIE] = cookieEncrypter.getCookiesForUnset()

            return LogoutUserResponse(getLogoutUrl())
        } else
        {
            throw InvalidBFFCookieException("No auth cookie was supplied in a logout call")
        }
    }

    fun getLogoutUrl(): String =
        "${config.logoutEndpoint}?client_id=${config.clientID.encodeURI()}" +
                if (config.postLogoutRedirectURI != null) "&post_logout_redirect_uri=${config.postLogoutRedirectURI!!.encodeURI()}" else ""
}

class LogoutUserResponse(
    val url: String
)
