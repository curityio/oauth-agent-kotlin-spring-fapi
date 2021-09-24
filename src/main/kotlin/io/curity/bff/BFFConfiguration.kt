package io.curity.bff

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "bff")
@ConstructorBinding
data class BFFConfigurationProperties(
    var clientID: String = "",
    var redirectUri: String,
    var postLogoutRedirectURI: String?,
    var scope: String?,

    // Authorization Server Configuration
    var issuer: String,
    var jwksUri: String,
    var logoutEndpoint: String,
    var authorizeEndpoint: String,
    var tokenEndpoint: String,

    // BFF session cookie and CORS configuration
    var cookieSerializeOptions: CookieSerializeOptions,
    var bffEndpointsPrefix: String,
    var encKey: String,
    var salt: String,
    var cookieNamePrefix: String,
    var trustedWebOrigins: List<String>
)

data class CookieSerializeOptions(
    var httpOnly: Boolean = true,
    var secure: Boolean = true,
    var sameSite: Boolean = true,
    var domain: String,
    var path: String = "/",
    var expiresInSeconds: Int? = null
)

@Configuration
@EnableConfigurationProperties(BFFConfigurationProperties::class)
class BFFConfiguration(configurationProperties: BFFConfigurationProperties)
{
    val clientID = configurationProperties.clientID
    val redirectUri = configurationProperties.redirectUri
    val postLogoutRedirectURI = configurationProperties.postLogoutRedirectURI
    val scope = configurationProperties.scope

    // Authorization Server Configuration
    val issuer = configurationProperties.issuer
    val jwksUri = configurationProperties.jwksUri
    val logoutEndpoint = configurationProperties.logoutEndpoint
    val authorizeEndpoint = configurationProperties.authorizeEndpoint
    val tokenEndpoint = configurationProperties.tokenEndpoint

    // BFF session cookie and CORS configuration
    val cookieSerializeOptions = configurationProperties.cookieSerializeOptions
    val bffEndpointsPrefix = configurationProperties.bffEndpointsPrefix
    val encKey = configurationProperties.encKey
    val salt = configurationProperties.salt
    val cookieNamePrefix = configurationProperties.cookieNamePrefix
    val trustedWebOrigins = configurationProperties.trustedWebOrigins
}
