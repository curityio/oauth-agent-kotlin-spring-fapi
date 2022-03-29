package io.curity.oauthagent

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "oauthagent")
@ConstructorBinding
data class OAuthAgentConfigurationProperties(
    var clientID: String = "",
    var redirectUri: String,
    var postLogoutRedirectURI: String?,
    var scope: String?,

    // Authorization Server Configuration
    var issuer: String,
    var jwksUri: String,
    var logoutEndpoint: String,
    var authorizeEndpoint: String,
    var authorizeExternalEndpoint: String,
    var tokenEndpoint: String,

    // Secure cookie and CORS configuration
    var cookieSerializeOptions: CookieSerializeOptions,
    var endpointsPrefix: String,
    var encKey: String,
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
@EnableConfigurationProperties(OAuthAgentConfigurationProperties::class)
class OAuthAgentConfiguration(configurationProperties: OAuthAgentConfigurationProperties)
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
    val authorizeExternalEndpoint = configurationProperties.authorizeExternalEndpoint
    val tokenEndpoint = configurationProperties.tokenEndpoint

    // Secure cookie and CORS configuration
    val cookieSerializeOptions = configurationProperties.cookieSerializeOptions
    val endpointsPrefix = configurationProperties.endpointsPrefix
    val encKey = configurationProperties.encKey
    val cookieNamePrefix = configurationProperties.cookieNamePrefix
    val trustedWebOrigins = configurationProperties.trustedWebOrigins
}
