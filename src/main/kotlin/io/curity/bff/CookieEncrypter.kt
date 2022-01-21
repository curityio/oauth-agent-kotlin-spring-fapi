package io.curity.bff

import io.curity.bff.exception.CookieDecryptionException
import io.curity.bff.exception.InvalidBFFCookieException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Arrays;
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class CookieEncrypter(private val config: BFFConfiguration, private val cookieName: CookieName)
{
    private val encryptionKey = SecretKeySpec(config.encKey.decodeHex(), "AES")

    suspend fun getEncryptedCookie(cookieName: String, cookieValue: String, cookieOptions: CookieSerializeOptions) =
        encryptValue(cookieValue).serializeToCookie(cookieName, cookieOptions)

    suspend fun getEncryptedCookie(cookieName: String, cookieValue: String): String =
        encryptValue(cookieValue).serializeToCookie(cookieName, config.cookieSerializeOptions)

    suspend fun encryptValue(plaintext: String): String
    {
        return withContext(Dispatchers.Default) {
            kotlin.run {

                val ivBytes = ByteArray(GCM_IV_SIZE)
                SecureRandom().nextBytes(ivBytes)
                val parameterSpec = GCMParameterSpec(GCM_TAG_SIZE * 8, ivBytes)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, parameterSpec)

                val cipherTextBytes = cipher.doFinal(plaintext.toByteArray())
                val allBytes = byteArrayOf(CURRENT_VERSION.toByte()) + ivBytes + cipherTextBytes

                return@withContext Base64.getUrlEncoder().withoutPadding().encodeToString(allBytes)
            }
        }
    }

    suspend fun decryptValueFromCookie(cookieValue: String): String
    {
        return withContext(Dispatchers.Default) {
            kotlin.run {

                val allBytes = Base64.getUrlDecoder().decode(cookieValue);

                val minSize = VERSION_SIZE + GCM_IV_SIZE + 1 + GCM_TAG_SIZE
                if (allBytes.size < minSize) {
                    throw InvalidBFFCookieException("The received cookie has an invalid length")
                }

                val version = allBytes[0].toInt()
                if (version != CURRENT_VERSION) {
                    throw InvalidBFFCookieException("The received cookie has invalid format")
                }

                var offset = VERSION_SIZE
                val ivBytes = allBytes.copyOfRange(offset, offset + GCM_IV_SIZE)

                offset += GCM_IV_SIZE
                val ciphertextBytes = Arrays.copyOfRange(allBytes, offset, allBytes.size)

                val parameterSpec = GCMParameterSpec(GCM_TAG_SIZE * 8, ivBytes)

                try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, encryptionKey, parameterSpec)
                    val decryptedBytes = cipher.doFinal(ciphertextBytes)
                    return@withContext String(decryptedBytes)

                } catch (e: RuntimeException) {
                    throw CookieDecryptionException(e)
                }
            }
        }
    }

    fun String.decodeHex(): ByteArray
    {
        return ByteArray(length / 2) { current ->
            Integer.parseInt(this, current * 2, (current + 1) * 2, 16).toByte()
        }
    }

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
        const val VERSION_SIZE = 1
        const val GCM_IV_SIZE = 12
        const val GCM_TAG_SIZE = 16
        const val CURRENT_VERSION = 1

        private val minusDayInSeconds = -Duration.ofDays(1).toSeconds().toInt()
    }
}
