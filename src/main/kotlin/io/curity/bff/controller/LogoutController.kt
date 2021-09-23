package io.curity.bff.controller

import io.curity.bff.BFFConfiguration
import io.curity.bff.CookieEncrypter
import io.curity.bff.CookieName
import io.curity.bff.RequestValidator
import io.curity.bff.ValidateRequestOptions
import io.curity.bff.encodeURI
import io.curity.bff.exception.InvalidBFFCookieException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.WebUtils
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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
    fun logoutUser(request: HttpServletRequest, response: HttpServletResponse): LogoutUserResponse
    {
        requestValidator.validateServletRequest(request, ValidateRequestOptions())

        if (WebUtils.getCookie(request, cookieName.accessToken) != null)
        {
            cookieEncrypter.getCookiesForUnset().forEach {
                response.addHeader("Set-Cookie", it)
            }

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
