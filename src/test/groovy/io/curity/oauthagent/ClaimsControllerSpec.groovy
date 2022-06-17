package io.curity.oauthagent

import org.springframework.http.HttpHeaders
import org.springframework.web.client.HttpClientErrorException

import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.HttpStatus.*

class ClaimsControllerSpec extends TokenHandlerSpecification {

    static def claimsURL
    static def claimsPath = "/claims"

    def "Requesting claims from an untrusted origin should return a 401 response"() {
        given:
        def request = getRequestWithMaliciousOrigin(GET, claimsURI)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
    }

    def "Requesting claims without session cookies should return a 401 response"() {
        given:
        def request = getRequestWithValidOrigin(GET, claimsURI)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
        def responseBody = json.parseText(response.responseBodyAsString)
        responseBody["code"] == "unauthorized_request"
    }

    def "Requesting claims with valid cookies should return ID Token claims"() {
        given:
        def cookieHeader = new HttpHeaders()
        cookieHeader.addAll("Cookie", cookiesAndCSRFForAuthenticatedUser.cookies)
        def request = getRequestWithValidOrigin(GET, claimsURI, null, cookieHeader)

        when:
        def response = client.exchange(request, String.class)

        then:
        response.statusCode == OK
        def responseBody = json.parseText(response.body)
        responseBody["sub"] == "user@example.com"
        responseBody["auth_time"] == 1626259937
    }

    private def getClaimsURI() {
        if (claimsURL == null) {
            claimsURL = new URI("$baseUrl:$port/${configuration.endpointsPrefix}$claimsPath")
        }
        claimsURL
    }
}
