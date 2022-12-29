package io.curity.oauthagent

import org.springframework.http.HttpHeaders
import org.springframework.web.client.HttpClientErrorException

import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.FORBIDDEN
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.UNAUTHORIZED

class RefreshControllerSpec extends TokenHandlerSpecification {

    static def refreshEndpointPath = "/refresh"
    static def refreshEndpointURI

    /*def "Sending POST request to refresh endpoint from untrusted origin should return a 401 response"() {
        given:
        def request = getRequestWithMaliciousOrigin(POST, refreshEndpointURL)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
    }*/

    def "Sending POST request to refresh endpoint without session cookies should return a 401 response"() {
        given:
        def request = getRequestWithValidOrigin(POST, refreshEndpointURL)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
        print("*** DEBUG START")
        print(response.responseBodyAsString)
        print("*** DEBUG END")
        def responseBody = json.parseText(response.responseBodyAsString)
        responseBody["code"] == "unauthorized_request"
    }

    /*def "Posting to refresh endpoint with incorrect CSRF token should return a 401 response"() {
        given:
        def csrfHeader = new HttpHeaders()
        csrfHeader.add("x-${configuration.cookieNamePrefix}-csrf", "abc123")
        def request = getRequestWithValidOrigin(POST, refreshEndpointURL, null, csrfHeader)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
        def responseBody = json.parseText(response.responseBodyAsString)
        responseBody["code"] == "unauthorized_request"
    }

    def "Posting correct cookies to refresh endpoint should return a new set of cookies"() {
        given:
        stubs.idsvrRespondsWithRefreshedTokens()

        and: "There is a proper request to the refresh endpoint"
        def cookiesAndCSRF = cookiesAndCSRFForAuthenticatedUser
        def additionalHeaders = new HttpHeaders()
        additionalHeaders.addAll("Cookie", cookiesAndCSRF.cookies)
        additionalHeaders.add("x-${configuration.cookieNamePrefix}-csrf", cookiesAndCSRF.csrf.toString())
        def request = getRequestWithValidOrigin(POST, refreshEndpointURL, null, additionalHeaders)


        when: "The request is sent"
        def response = client.exchange(request, String.class)

        then: "The response is a 204 NO_CONTENT"
        response.statusCode == NO_CONTENT

        and: "Value of the access token and refresh token cookies should change"
        def currentCookies = response.headers["Set-Cookie"]
        def accessTokenCookieName = "${configuration.cookieNamePrefix}-at"
        def refreshTokenCookieName = "${configuration.cookieNamePrefix}-auth"

        cookiesAreDifferent(accessTokenCookieName, cookiesAndCSRF.cookies, currentCookies)
        cookiesAreDifferent(refreshTokenCookieName, cookiesAndCSRF.cookies, currentCookies)
    }

    def "A configuration error when refreshing tokens should result the SPA displaying a 400 error"() {
        given:
        stubs.idsvrRespondsToRefreshWithInvalidClientWhenClientSecretIsMisconfigured()

        and: "There is a proper request to the refresh endpoint"
        def cookiesAndCSRF = cookiesAndCSRFForAuthenticatedUser
        def additionalHeaders = new HttpHeaders()
        additionalHeaders.addAll("Cookie", cookiesAndCSRF.cookies)
        additionalHeaders.add("x-${configuration.cookieNamePrefix}-csrf", cookiesAndCSRF.csrf.toString())
        def request = getRequestWithValidOrigin(POST, refreshEndpointURL, null, additionalHeaders)

        when: "The request is sent"
        client.exchange(request, String.class)

        then: "The response is a 400"
        def response = thrown HttpClientErrorException
        response.statusCode == BAD_REQUEST
    }

    def "An expired refresh token should result in a 401 response and cleared cookies"() {
        given:
        stubs.idsvrRespondsToRefreshWithInvalidGrantWhenRefreshTokenIsExpired()

        and: "There is a proper request to the refresh endpoint"
        def cookiesAndCSRF = cookiesAndCSRFForAuthenticatedUser
        def additionalHeaders = new HttpHeaders()
        additionalHeaders.addAll("Cookie", cookiesAndCSRF.cookies)
        additionalHeaders.add("x-${configuration.cookieNamePrefix}-csrf", cookiesAndCSRF.csrf.toString())
        def request = getRequestWithValidOrigin(POST, refreshEndpointURL, null, additionalHeaders)

        when: "The request is sent"
        client.exchange(request, String.class)

        then: "The response is a 401"
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED

        and: "Cookie values should be set to empty cookies"
        def currentCookies = response.responseHeaders["Set-Cookie"]
        !currentCookies.any {it.matches("${configuration.cookieNamePrefix}-(at|auth|csrf|id)=[^;]")}
    }*/

    private def getRefreshEndpointURL() {
        if (refreshEndpointURI == null) {
            refreshEndpointURI = new URI("$baseUrl:$port/${configuration.endpointsPrefix}$refreshEndpointPath")
        }

        refreshEndpointURI
    }

    private static def cookiesAreDifferent(String cookieName, List<String> previousCookies, List<String> currentCookies) {
        currentCookies.find {it.startsWith cookieName} != previousCookies.find {it.startsWith cookieName }
    }
}
