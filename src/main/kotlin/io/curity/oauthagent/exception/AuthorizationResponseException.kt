package io.curity.oauthagent.exception

class AuthorizationResponseException(error: String, description: String) : OAuthAgentException(
    description,
    null,
    400,
    error,
    ""
) {

    init {
        if (code == "login_required") {
            statusCode = 401
        }
    }
}