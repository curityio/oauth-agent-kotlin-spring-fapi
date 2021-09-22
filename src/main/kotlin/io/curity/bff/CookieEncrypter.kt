package io.curity.bff

import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Service
class CookieEncrypter(private val config: BFFConfiguration, private val cookieName: CookieName)
{

    private val key = getKeyFromPassword()

    //@Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun getKeyFromPassword(): SecretKey
    {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(config.encKey.toCharArray(), config.salt.toByteArray(), 65536, 256)
        return SecretKeySpec(
            factory.generateSecret(spec)
                .encoded, "AES"
        )
    }

    fun getEncryptedCookie(cookieName: String, cookieValue: String, cookieOptions: CookieSerializeOptions) =
        encryptValue(cookieValue).serializeToCookie(cookieName, cookieOptions)

    fun getEncryptedCookie(cookieName: String, cookieValue: String): String =
        encryptValue(cookieValue).serializeToCookie(cookieName, config.cookieSerializeOptions)

    fun encryptValue(value: String): String {
        val iv = generateIv()
        return "${String(Base64.getEncoder().encode(iv.iv.toHex()), StandardCharsets.UTF_8)}:${encrypt("AES/CBC/PKCS5Padding", value, iv)}"

    }

    fun generateIv(): IvParameterSpec
    {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        return IvParameterSpec(iv)
    }

    //@Throws(
    //    NoSuchPaddingException::class,
    //    NoSuchAlgorithmException::class,
    //    InvalidAlgorithmParameterException::class,
    //    InvalidKeyException::class,
    //    BadPaddingException::class,
    //    IllegalBlockSizeException::class
    //)
    fun encrypt(
        algorithm: String, input: String, iv: IvParameterSpec
    ): String
    {
        val cipher: Cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val cipherText: ByteArray = cipher.doFinal(input.toByteArray())
        return Base64.getEncoder()
            .encodeToString(cipherText)
    }

    private fun decrypt(
        algorithm: String, cipherText: String, key: SecretKey, iv: IvParameterSpec
    ): String
    {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val plainText = cipher.doFinal(
            Base64.getDecoder()
                .decode(cipherText)
        )
        return String(plainText)
    }

    fun decryptValueFromCookie(cookieValue: String): String {
        val valueArray = cookieValue.split(":")

        val iv = String(Base64.getDecoder().decode(valueArray[0]), StandardCharsets.UTF_8)
        val cipherText = valueArray[1]

        return decrypt("AES/CBC/PKCS5Padding", cipherText, key, IvParameterSpec(iv.decodeHex()))
    }

    fun String.decodeHex(): ByteArray {
        return ByteArray(length / 2) { current ->
            Integer.parseInt(this, current * 2, (current + 1) * 2, 16).toByte()
        }
    }

    fun String.serializeToCookie(name: String, options: CookieSerializeOptions): String {
        val builder = StringBuilder()
        builder.append(name).append('=')
        builder.append(this)

        builder.append("; Domain=").append(options.domain)
        builder.append("; Path=").append(options.path)
        if (options.secure) {
            builder.append("; Secure")
        }

        builder.append("; HttpOnly")

        if (options.sameSite) {
            builder.append("; SameSite=true")
        }

        val expiresInSeconds = options.expiresInSeconds
        if (expiresInSeconds != null)
        {
            if (expiresInSeconds > -1) {
                builder.append("; Max-Age=").append(options.expiresInSeconds)
            }

            val expires =
                if (expiresInSeconds != 0) ZonedDateTime.now().plusSeconds(expiresInSeconds.toLong()) else Instant.EPOCH.atZone(ZoneOffset.UTC)
            builder.append("; Expires=").append(expires.format(DateTimeFormatter.RFC_1123_DATE_TIME))
        }

        return builder.toString()
    }

    fun getCookieForUnset(cookieName: String): String {
        val options = config.cookieSerializeOptions.copy(expiresInSeconds = minusDayInSeconds)
        return "".serializeToCookie(cookieName, options)
    }

    fun getCookiesForUnset(): List<String> {
        val options = config.cookieSerializeOptions.copy(expiresInSeconds = minusDayInSeconds)
        return cookieName.cookiesForUnset.map { "".serializeToCookie(it, options) }
    }

    companion object {
        private val minusDayInSeconds = -Duration.ofDays(1).toSeconds().toInt()
    }
}
