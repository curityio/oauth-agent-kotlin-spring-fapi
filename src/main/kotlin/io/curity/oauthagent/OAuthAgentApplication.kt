package io.curity.oauthagent

import io.curity.oauthagent.utilities.CustomCorsProcessor
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwk.HttpsJwks
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jwt.consumer.JwtConsumer
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter

@SpringBootApplication
class OAuthAgentApplication {

    @Bean
    fun corsWebFilter(configuration: OAuthAgentConfiguration): CorsWebFilter {

        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowedOrigins = configuration.trustedWebOrigins
        config.allowCredentials = true
        config.allowedMethods = listOf("POST", "GET", "OPTIONS")
        config.allowedHeaders = listOf("*")
        source.registerCorsConfiguration("/**", config)

        val corsProcessor = CustomCorsProcessor()
        return CorsWebFilter(source, corsProcessor)
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
