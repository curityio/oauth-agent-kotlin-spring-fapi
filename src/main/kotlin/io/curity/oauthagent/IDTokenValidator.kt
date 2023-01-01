package io.curity.oauthagent

import io.curity.oauthagent.exception.InvalidIDTokenException
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.springframework.stereotype.Service

@Service
class IDTokenValidator(private val config: OAuthAgentConfiguration) {

    /*
     * Make some sanity checks to ensure that the issuer and audience are configured correctly
     * The ID token is received over a trusted back channel connection so its signature does not need verifying
     * https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
     */
    fun validate(idToken: String) {

        try {
            val jwtConsumer = JwtConsumerBuilder()
                    .setSkipSignatureVerification()
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(30)
                    .setExpectedIssuer(config.issuer)
                    .setExpectedAudience(config.clientID)
                    .build()

            val claims = jwtConsumer.processToClaims(idToken)
            val azp = claims.getClaimValue("azp", String::class.java)
            if (!azp.isNullOrBlank() || claims.audience.size > 0) {
                if (azp != config.clientID) {
                    throw InvalidIDTokenException()
                }
            }

        } catch (exception: InvalidJwtException) {
            throw InvalidIDTokenException(exception)
        }
    }
}
