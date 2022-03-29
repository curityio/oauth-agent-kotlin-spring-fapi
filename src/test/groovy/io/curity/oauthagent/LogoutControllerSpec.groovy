package io.curity.oauthagent

import org.springframework.http.HttpHeaders
import org.springframework.web.client.HttpClientErrorException

import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.HttpStatus.FORBIDDEN
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNAUTHORIZED

class LogoutControllerSpec extends TokenHandlerSpecification {

    static def logoutEndpointPath = "/logout"
    static def logoutEndpointURI

    def "Posting to logout from a malicious origin should return a 403 response"() {
        given:
        def request = getRequestWithMaliciousOrigin(POST, logoutEndpointURL)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == FORBIDDEN
    }

    def "Posting to logout without cookies should return a 401 response"() {
        given:
        def request = getRequestWithValidOrigin(POST, logoutEndpointURL)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
        def responseBody = json.parseText(response.responseBodyAsString)
        responseBody["code"] == "unauthorized_request"
    }

    def "Posting incorrect CSRF token to logout should return a 401 response"() {
        given:
        def csrfHeader = new HttpHeaders()
        csrfHeader.add("x-${configuration.cookieNamePrefix}-csrf", "abc123")

        def request = getRequestWithValidOrigin(POST, logoutEndpointURL, null ,csrfHeader)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
        def responseBody = json.parseText(response.responseBodyAsString)
        responseBody["code"] == "unauthorized_request"
    }

    def "Posting to logout with correct session cookies should return a 200 response and clear cookies"() {
        given:
        def additionalHeaders = new HttpHeaders()
        def cookiesAndCSRF = getCookiesAndCSRFForAuthenticatedUser()
        additionalHeaders.addAll("Cookie", cookiesAndCSRF.cookies)
        additionalHeaders.add("x-${configuration.cookieNamePrefix}-csrf", cookiesAndCSRF.csrf.toString())
        def request = getRequestWithValidOrigin(POST, logoutEndpointURL, null, additionalHeaders)

        when:
        def response = client.exchange(request, String.class)

        then: "Response status is 200 OK"
        response.statusCode == OK

        and: "Logout url is returned"
        def responseBody = json.parseText(response.body)
        def logoutUrl = responseBody["url"].toString()
        logoutUrl.startsWith(configuration.logoutEndpoint)
        logoutUrl.contains(configuration.clientID)
        def encodedPostLogoutRedirectUri = StringHelperKt.encodeURI(configuration.postLogoutRedirectURI)
        logoutUrl.contains(encodedPostLogoutRedirectUri)

        and: "Cookie values should be set to empty cookies"
        def currentCookies = response.headers["Set-Cookie"]
        !currentCookies.any {it.matches("${configuration.cookieNamePrefix}-(at|auth|csrf|id)=[^;]")}
    }

    private def getLogoutEndpointURL() {
        if (logoutEndpointURI == null) {
            logoutEndpointURI = new URI("$baseUrl:$port/${configuration.endpointsPrefix}$logoutEndpointPath")
        }
        logoutEndpointURI
    }
}
