package io.curity.oauthagent.behaviors.authorizationrequest

import io.curity.oauthagent.OAuthAgentConfiguration
import io.curity.oauthagent.OAuthParametersProvider
import io.curity.oauthagent.controller.StartAuthorizationParameters
import java.lang.RuntimeException

class DefaultAuthorizationRequestHandler(
        private val config: OAuthAgentConfiguration,
        private val oAuthParametersProvider: OAuthParametersProvider) : AuthorizationRequestHandler {

    override suspend fun createRequest(parameters: StartAuthorizationParameters?): AuthorizationRequestData {
        throw RuntimeException("NOT IMPLEMENTED")
    }
}
