package io.curity.oauthagent

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "oauthagent")
@ConstructorBinding
data class OAuthAgentConfigurationProperties(

    // Host settings
    var endpointsPrefix: String,

    // Client settings
    var clientID: String = "",
    var redirectUri: String,
    var postLogoutRedirectURI: String?,
    var scope: String?,

    // Behavior settings
    var financialGrade: Boolean,

    // Authorization Server settings
    var issuer: String,
    var jwksUri: String,
    var logoutEndpoint: String,
    var authorizeEndpoint: String,
    var authorizeExternalEndpoint: String,
    var tokenEndpoint: String,
    var userInfoEndpoint: String,

    // Secure cookie and CORS settings
    var cookieNamePrefix: String,
    var encKey: String,
    var trustedWebOrigins: List<String>,
    var corsEnabled: Boolean,
    var cookieSerializeOptions: CookieSerializeOptions
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
    // Host settings
    val endpointsPrefix = configurationProperties.endpointsPrefix

    // Behavior settings
    val financialGrade = configurationProperties.financialGrade

    // Client settings
    val clientID = configurationProperties.clientID
    val redirectUri = configurationProperties.redirectUri
    val postLogoutRedirectURI = configurationProperties.postLogoutRedirectURI
    val scope = configurationProperties.scope

    // Authorization Server settings
    val issuer = configurationProperties.issuer
    val jwksUri = configurationProperties.jwksUri
    val logoutEndpoint = configurationProperties.logoutEndpoint
    val authorizeEndpoint = configurationProperties.authorizeEndpoint
    val authorizeExternalEndpoint = configurationProperties.authorizeExternalEndpoint
    val tokenEndpoint = configurationProperties.tokenEndpoint
    val userInfoEndpoint = configurationProperties.userInfoEndpoint

    // Secure cookie and CORS configuration
    val cookieNamePrefix = configurationProperties.cookieNamePrefix
    val encKey = configurationProperties.encKey
    val trustedWebOrigins = configurationProperties.trustedWebOrigins
    val corsEnabled = configurationProperties.corsEnabled
    val cookieSerializeOptions = configurationProperties.cookieSerializeOptions
}
