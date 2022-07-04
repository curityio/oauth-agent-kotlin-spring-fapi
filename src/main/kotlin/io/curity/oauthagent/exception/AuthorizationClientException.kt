package io.curity.oauthagent.exception

import io.curity.oauthagent.utilities.Grant
import org.springframework.http.HttpStatus

class AuthorizationClientException(status: Int, code: String, responseText: String) : OAuthAgentException(
    "A request sent to the Authorization Server was rejected",
    null,
    status,
    code,
    responseText
) {

    companion object Factory {

        fun create(grant: Grant, status: HttpStatus, responseText: String): AuthorizationClientException {

            var statusNumber = 400
            var errorCode = "authorization_error"

            if (grant == Grant.UserInfo && status == HttpStatus.UNAUTHORIZED) {
                errorCode = "token_expired"
                statusNumber = 401
            }

            if (grant == Grant.RefreshToken && responseText.contains("invalid_grant")) {
                errorCode = "session_expired"
                statusNumber = 401
            }

            val logMessage = "$grant request failed with response: $responseText"
            return AuthorizationClientException(
                statusNumber,
                errorCode,
                logMessage
            )
        }
    }
}
