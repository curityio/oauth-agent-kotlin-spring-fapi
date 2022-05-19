package io.curity.oauthagent

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static groovy.json.JsonOutput.toJson
import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.HttpStatus.*

class ExtensibilitySpec extends TokenHandlerSpecification {

    def "Starting a login request with a simple OpenID Connect parameter should succeed"() {
        given:

        def options = [
                "extraParams": [
                        ["key": "prompt", "value": "login"]
                ]
        ]

        def request = getRequestWithValidOrigin(POST, loginStartURI, toJson(options))
        stubFor(post(stubs.getPAREndpointPath())
            .willReturn(aResponse()
                .withBody(toJson([request_uri: "parRequestURI", expires_in: 100]))
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
        ))

        when:
        def response = client.exchange(request, String.class)

        then:
        response.statusCode == OK
        def responseBody = json.parseText(response.body)
        def authorizationRequestUrl = responseBody["authorizationRequestUrl"]?.toString()
        !authorizationRequestUrl.empty
    }

    def "Starting a login request with multiple OpenID Connect parameters should succeed"() {

        def claims = [
                "id_token": [
                        "acr": [
                                "essential": true,
                                "values": [
                                        "urn:se:curity:authentication:html-form:htmlform1"
                                ]
                        ]
                ]
        ]

        def options = [
                "extraParams": [
                        ["key": "ui_locates", "value": "fr"],
                        ["key": "claims", "value": toJson(claims)]
                ]
        ]

        def request = getRequestWithValidOrigin(POST, loginStartURI, toJson(options))
        stubFor(post(stubs.getPAREndpointPath())
                .willReturn(aResponse()
                        .withBody(toJson([request_uri: "parRequestURI", expires_in: 100]))
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                ))

        when:
        def response = client.exchange(request, String.class)

        then:
        response.statusCode == OK
        def responseBody = json.parseText(response.body)
        def authorizationRequestUrl = responseBody["authorizationRequestUrl"]?.toString()
        !authorizationRequestUrl.empty
    }
}
