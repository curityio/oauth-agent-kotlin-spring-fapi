package io.curity.oauthagent.controller

import io.curity.oauthagent.exception.OAuthAgentException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody

@ControllerAdvice
class ExceptionController
{
    @ExceptionHandler(OAuthAgentException::class)
    @ResponseBody
    fun handleException(exception: OAuthAgentException, response: ServerHttpResponse): ErrorMessage
    {
        logger.info("Exception occurred during request: {}", exception.logMessage, exception)

        response.statusCode = HttpStatus.valueOf(exception.statusCode)

        return ErrorMessage(
            exception.code,
            exception.message
        )
    }

    companion object
    {
        private val logger = LoggerFactory.getLogger(ExceptionController::class.java)
    }
}

data class ErrorMessage(
    val code: String,
    val message: String?
)
