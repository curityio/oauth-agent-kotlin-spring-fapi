package io.curity.oauthagent

import org.springframework.http.HttpHeaders
import org.springframework.web.client.HttpClientErrorException

import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.HttpStatus.FORBIDDEN
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNAUTHORIZED

class UserinfoControllerSpec extends TokenHandlerSpecification {

    static def userinfoURL
    static def userinfoPath = "/userInfo"

    def "Requesting user info from an untrusted origin should return a 403 response"() {
        given:
        def request = getRequestWithMaliciousOrigin(GET, userinfoURI)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == FORBIDDEN
    }

    def "Requesting user info without session cookies should return a 401 response"() {
        given:
        def request = getRequestWithValidOrigin(GET, userinfoURI)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
        def responseBody = json.parseText(response.responseBodyAsString)
        responseBody["code"] == "unauthorized_request"
    }

    def "Requesting user info with valid cookies should return user data"() {
        given:
        def cookieHeader = new HttpHeaders()
        cookieHeader.addAll("Cookie", cookiesAndCSRFForAuthenticatedUser.cookies)
        def request = getRequestWithValidOrigin(GET, userinfoURI, null, cookieHeader)

        when:
        def response = client.exchange(request, String.class)

        then:
        response.statusCode == OK
        def responseBody = json.parseText(response.body)
        responseBody["sub"] == "user@example.com"
        responseBody["username"] == "user"
    }

    private def getUserinfoURI() {
        if (userinfoURL == null) {
            userinfoURL = new URI("$baseUrl:$port/${configuration.endpointsPrefix}$userinfoPath")
        }
        userinfoURL
    }
}
