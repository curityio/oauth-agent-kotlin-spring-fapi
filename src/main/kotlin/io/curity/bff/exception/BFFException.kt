package io.curity.bff.exception

import java.lang.RuntimeException

sealed class BFFException(message: String, cause: Throwable?, val statusCode: Int, val code: String, val logMessage: String?): RuntimeException(message, cause)
