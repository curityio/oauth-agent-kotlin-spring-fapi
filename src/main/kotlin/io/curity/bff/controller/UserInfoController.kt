package io.curity.bff.controller

import io.curity.bff.CookieName
import io.curity.bff.RequestValidator
import io.curity.bff.UserInfo
import io.curity.bff.ValidateRequestOptions
import io.curity.bff.exception.InvalidBFFCookieException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.WebUtils
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/\${bff.bffEndpointsPrefix}/userInfo")
class UserInfoController(private val requestValidator: RequestValidator, private val userInfo: UserInfo, private val cookieName: CookieName)
{
    @GetMapping("", produces = ["application/json"])
    fun getUserInfo(request: HttpServletRequest): Map<String, Any> {
        requestValidator.validateServletRequest(request,
            ValidateRequestOptions(requireCsrfHeader = false)
        )

        val idTokenCookie = WebUtils.getCookie(request, cookieName.idToken)?.value
            ?: throw InvalidBFFCookieException("No ID cookie was supplied in a call to get user info")

        return userInfo.getUserInfo(idTokenCookie)
    }
}
