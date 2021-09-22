package io.curity.bff

import java.nio.charset.StandardCharsets

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

private val HEX_ARRAY = "0123456789ABCDEF".toByteArray(StandardCharsets.US_ASCII)
