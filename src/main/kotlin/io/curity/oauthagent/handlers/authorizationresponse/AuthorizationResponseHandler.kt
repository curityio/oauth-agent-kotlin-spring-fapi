package io.curity.oauthagent.handlers.authorizationresponse

import io.curity.oauthagent.controller.OAuthQueryParams

/*
 * Handles an authorization response to receive the code and state
 */
interface AuthorizationResponseHandler {
    suspend fun handleResponse(pageUrl: String?): OAuthQueryParams
}