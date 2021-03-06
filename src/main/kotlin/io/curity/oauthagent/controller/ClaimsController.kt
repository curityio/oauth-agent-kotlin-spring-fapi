package io.curity.oauthagent.controller

import io.curity.oauthagent.*
import io.curity.oauthagent.exception.InvalidCookieException
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/\${oauthagent.endpointsPrefix}/claims")
class ClaimsController(
    private val requestValidator: RequestValidator,
    private val idTokenClaims: IDTokenClaims,
    private val cookieName: CookieName
)
{
    @GetMapping("", produces = ["application/json"])
    suspend fun getClaims(request: ServerHttpRequest): Map<String, Any>
    {
        requestValidator.validateServletRequest(
            request,
            ValidateRequestOptions(requireCsrfHeader = false)
        )

        val idTokenCookie = request.cookies[cookieName.idToken]?.first()?.value
            ?: throw InvalidCookieException("No ID cookie was supplied in a call to get ID token claims")

        return idTokenClaims.getClaims(idTokenCookie)
    }
}
