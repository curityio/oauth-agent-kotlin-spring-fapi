package io.curity.oauthagent.exception

class AuthorizationClientException(logMessage: String) : OAuthAgentException(
    "A request sent to the Authorization Server was rejected",
    null,
    400,
    "authorization_error",
    logMessage
) {

    fun onTokenRefreshFailed(text: String) {

       if (text.contains("invalid_grant")) {

            this.code = "session_expired"
            this.statusCode = 401
        }
    }
}
