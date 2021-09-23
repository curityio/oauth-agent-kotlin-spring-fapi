package io.curity.bff

import java.nio.charset.StandardCharsets
import java.security.SecureRandom

fun ByteArray.toHex(): ByteArray
{
    val hexChars = ByteArray(this.size * 2)
    for (j in this.indices)
    {
        val v: Int = this[j].toInt()
        hexChars[j * 2] = HEX_ARRAY[v ushr 4 and 0x0F]
        hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
    }
    return hexChars
}

fun generateRandomString(): String
{
    val leftLimit = 48 // numeral '0'
    val rightLimit = 122 // letter 'z'

    val targetStringLength = 64L
    val random = SecureRandom()

    return random.ints(leftLimit, rightLimit + 1)
        .filter { i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97) }
        .limit(targetStringLength)
        .collect({ StringBuilder() }, java.lang.StringBuilder::appendCodePoint, java.lang.StringBuilder::append)
        .toString()
}

private val HEX_ARRAY = "0123456789ABCDEF".toByteArray(StandardCharsets.US_ASCII)
