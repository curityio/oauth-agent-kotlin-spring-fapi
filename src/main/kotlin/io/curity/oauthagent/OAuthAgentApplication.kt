package io.curity.oauthagent

import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwk.HttpsJwks
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jwt.consumer.JwtConsumer
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer


@SpringBootApplication
class OAuthAgentApplication
{

    @Bean
    fun corsConfigurer(configuration: OAuthAgentConfiguration): WebFluxConfigurer
    {
        return object : WebFluxConfigurer
        {
            override fun addCorsMappings(registry: CorsRegistry)
            {
                registry
                    .addMapping("/**")
                    .allowedOrigins(*configuration.trustedWebOrigins.toTypedArray())
                    .allowCredentials(true)
                    .allowedMethods("POST", "GET", "OPTIONS")
            }
        }
    }

    @Bean
    fun jwtConsumer(config: OAuthAgentConfiguration): JwtConsumer
    {
        val httpsJkws = HttpsJwks(config.jwksUri)
        val httpsJwksKeyResolver = HttpsJwksVerificationKeyResolver(httpsJkws)

        return JwtConsumerBuilder()
            .setRequireExpirationTime()
            .setAllowedClockSkewInSeconds(30)
            .setExpectedIssuer(config.issuer)
            .setExpectedAudience(config.clientID)
            .setVerificationKeyResolver(httpsJwksKeyResolver)
            .setJwsAlgorithmConstraints(
                AlgorithmConstraints.ConstraintType.PERMIT,
                AlgorithmIdentifiers.RSA_USING_SHA256
            )
            .build()
    }

    @Bean
    fun oauthParametersProvider(): OAuthParametersProvider = OAuthParametersProviderImpl()
}

fun main(args: Array<String>)
{
    runApplication<OAuthAgentApplication>(*args)
}
