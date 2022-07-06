package io.curity.oauthagent

import java.security.SecureRandom

fun ByteArray.toHexString(): String {
    return joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
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
