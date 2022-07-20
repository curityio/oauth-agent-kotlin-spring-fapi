package io.curity.oauthagent

import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class CookieBuilder(private val config: OAuthAgentConfiguration, private val cookieName: CookieName, private val encrypter: CookieEncrypter)
{
    suspend fun getEncryptedCookie(cookieName: String, cookieValue: String, cookieOptions: CookieSerializeOptions) =
        encrypter.encryptValue(cookieValue).serializeToCookie(cookieName, cookieOptions)

    suspend fun getEncryptedCookie(cookieName: String, cookieValue: String): String =
        encrypter.encryptValue(cookieValue).serializeToCookie(cookieName, config.cookieSerializeOptions)

    fun String.serializeToCookie(name: String, options: CookieSerializeOptions): String
    {
        val builder = StringBuilder()
        builder.append(name).append('=')
        builder.append(this)

        builder.append("; Domain=").append(options.domain)
        builder.append("; Path=").append(options.path)
        if (options.secure)
        {
            builder.append("; Secure")
        }

        builder.append("; HttpOnly")

        if (options.sameSite)
        {
            builder.append("; SameSite=true")
        }

        val expiresInSeconds = options.expiresInSeconds
        if (expiresInSeconds != null)
        {
            if (expiresInSeconds > -1)
            {
                builder.append("; Max-Age=").append(options.expiresInSeconds)
            }

            val expires =
                if (expiresInSeconds != 0) ZonedDateTime.now()
                    .plusSeconds(expiresInSeconds.toLong()) else Instant.EPOCH.atZone(ZoneOffset.UTC)
            builder.append("; Expires=").append(expires.format(DateTimeFormatter.RFC_1123_DATE_TIME))
        }

        return builder.toString()
    }

    fun getCookieForUnset(cookieName: String): String
    {
        val options = config.cookieSerializeOptions.copy(expiresInSeconds = minusDayInSeconds)
        return "".serializeToCookie(cookieName, options)
    }

    fun getCookiesForUnset(): List<String>
    {
        val options = config.cookieSerializeOptions.copy(expiresInSeconds = minusDayInSeconds)
        return cookieName.cookiesForUnset.map { "".serializeToCookie(it, options) }
    }

    companion object
    {
        private val minusDayInSeconds = -Duration.ofDays(1).seconds.toInt()
    }
}
