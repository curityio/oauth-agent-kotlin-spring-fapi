package io.curity.oauthagent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.curity.oauthagent.exception.InvalidCookieException
import io.curity.oauthagent.exception.InvalidIDTokenException
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class IDTokenClaims(private val cookieEncrypter: CookieEncrypter, private val objectMapper: ObjectMapper)
{
    suspend fun getClaims(idTokenCookie: String): Map<String, Any>
    {
        val idToken: String

        try
        {
            idToken = cookieEncrypter.decryptValueFromCookie(idTokenCookie)
        } catch (exception: RuntimeException)
        {
            throw InvalidCookieException("Unable to decrypt the ID cookie to get claims", exception)
        }

        val tokenParts = idToken.split(".")
        if (tokenParts.size != 3)
        {
            throw InvalidIDTokenException()
        }

        try
        {
            return objectMapper.readValue(Base64.getDecoder().decode(tokenParts[1]), IDTokenType())
        } catch (exception: RuntimeException)
        {
            throw InvalidIDTokenException(exception)
        }
    }
}

class IDTokenType : TypeReference<Map<String, Any>>()
