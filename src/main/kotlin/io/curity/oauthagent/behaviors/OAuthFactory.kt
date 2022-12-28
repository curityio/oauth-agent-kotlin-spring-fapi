package io.curity.oauthagent.behaviors

import io.curity.oauthagent.AuthorizationServerClient
import io.curity.oauthagent.OAuthAgentConfiguration
import io.curity.oauthagent.OAuthParametersProvider
import io.curity.oauthagent.behaviors.authorizationrequest.AuthorizationRequestHandler
import io.curity.oauthagent.behaviors.authorizationrequest.DefaultAuthorizationRequestHandler
import io.curity.oauthagent.behaviors.authorizationrequest.ParAuthorizationRequestHandler
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class OAuthFactory(
        private val config: OAuthAgentConfiguration,
        private val context: ConfigurableApplicationContext) {

    @Bean
    fun createAuthorizationRequestHandler(): AuthorizationRequestHandler {

        val parametersProvider = this.context.beanFactory.getBean(OAuthParametersProvider::class.java)

        return if (config.financialGrade) {

            val authorizationServerClient = this.context.beanFactory.getBean(AuthorizationServerClient::class.java)
            ParAuthorizationRequestHandler(authorizationServerClient, parametersProvider)

        } else {

            DefaultAuthorizationRequestHandler(config, parametersProvider)
        }
    }
}
