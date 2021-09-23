package io.curity.bff

import io.curity.bff.exception.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.web.util.WebUtils
import javax.servlet.http.HttpServletRequest

@Service
class RequestValidator(
    private val cookieEncrypter: CookieEncrypter,
    private val config: BFFConfiguration,
    private val cookieName: CookieName
)
{
    fun validateServletRequest(request: HttpServletRequest, options: ValidateRequestOptions)
    {
        validateRequest(
            request.getHeader("X-${config.cookieNamePrefix}-csrf"),
            WebUtils.getCookie(request, cookieName.csrf)?.value,
            request.getHeader("Origin"),
            options
        )
    }

    private fun validateRequest(
        csrfHeader: String?,
        csrfCookie: String?,
        origin: String?,
        options: ValidateRequestOptions
    )
    {

        if (options.requireTrustedOrigin)
        {
            validateOrigin(origin)
        }

        if (options.requireCsrfHeader)
        {
            validateCSRFToken(csrfCookie, csrfHeader)
        }
    }

    private fun validateCSRFToken(csrfCookie: String?, csrfHeader: String?)
    {
        if (csrfCookie == null)
        {
            throw UnauthorizedException("No CSRF cookie was supplied in a POST request")
        }

        val decryptedCsrf = cookieEncrypter.decryptValueFromCookie(csrfCookie)
        if (decryptedCsrf != csrfHeader)
        {
            throw UnauthorizedException("The CSRF header did not match the CSRF cookie in a POST request")
        }
    }

    private fun validateOrigin(origin: String?)
    {
        if (origin == null || !config.trustedWebOrigins.contains(origin))
        {
            throw UnauthorizedException("The call is from an untrusted web origin: $origin")
        }
    }


}

class ValidateRequestOptions(
    val requireTrustedOrigin: Boolean = true,
    val requireCsrfHeader: Boolean = true
)
