package io.curity.bff

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.curity.bff.exception.InvalidBFFCookieException
import io.curity.bff.exception.InvalidIDTokenException
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.util.Base64

@Service
class UserInfo(private val cookieEncrypter: CookieEncrypter, private val objectMapper: ObjectMapper)
{
    fun getUserInfo(idTokenCookie: String): Map<String, Any>
    {
        val idToken: String

        try
        {
            idToken = cookieEncrypter.decryptValueFromCookie(idTokenCookie)
        } catch (exception: RuntimeException) {
            throw InvalidBFFCookieException("Unable to decrypt the ID cookie to get user info", exception)
        }

        // TODO this should properly verify id token
        // TODO - what to do when id token is expired or missing? Call userinfo endpoint? It may have different data than the id token
        val tokenParts = idToken.split(".")

        if (tokenParts.size != 3) {
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

class IDTokenType: TypeReference<Map<String, Any>>()
