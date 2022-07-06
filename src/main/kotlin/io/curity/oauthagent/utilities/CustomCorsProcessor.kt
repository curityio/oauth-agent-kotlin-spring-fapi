package io.curity.oauthagent.utilities

import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.cors.reactive.DefaultCorsProcessor

class CustomCorsProcessor: DefaultCorsProcessor() {

    // Return 401 responses to the SPA when there are CORS errors
    override fun rejectRequest(response: ServerHttpResponse) {
        response.statusCode = HttpStatus.UNAUTHORIZED
    }
}