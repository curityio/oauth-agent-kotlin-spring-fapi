package io.curity.oauthagent.behaviors.authorizationrequest

import io.curity.oauthagent.controller.StartAuthorizationParameters

interface AuthorizationRequestHandler {
    suspend fun createRequest(parameters: StartAuthorizationParameters?): AuthorizationRequestData;
}
