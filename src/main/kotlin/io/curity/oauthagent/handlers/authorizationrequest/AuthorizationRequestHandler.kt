package io.curity.oauthagent.handlers.authorizationrequest

import io.curity.oauthagent.controller.StartAuthorizationParameters

/*
 * Creates the OpenID Connect authentication request details
 */
interface AuthorizationRequestHandler {
    suspend fun createRequest(parameters: StartAuthorizationParameters?): AuthorizationRequestData
}
