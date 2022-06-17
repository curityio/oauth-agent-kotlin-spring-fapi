package io.curity.oauthagent.exception

import io.curity.oauthagent.utilities.Grants
import org.springframework.http.HttpStatus

class AuthorizationClientException(logMessage: String) : OAuthAgentException(
    "A request sent to the Authorization Server was rejected",
    null,
    400,
    "authorization_error",
    logMessage
) {

    // Return a response to the SPA based on the response form the Authorization Server
    fun classify(grant: String, status: HttpStatus, text: String) {

       // When a refresh token expires, the 401 informs the SPA to trigger re-authentication
        if (grant == Grants.RefreshToken && text.contains("invalid_grant")) {
            this.code = "session_expired"
            this.statusCode = 401
        }

        // When an access token expires during a user info request, inform the SPA to do a token refresh
        if (grant == Grants.UserInfo && status == HttpStatus.UNAUTHORIZED) {
            this.statusCode = 401
        }
    }
}
