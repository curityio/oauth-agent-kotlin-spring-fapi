package io.curity.oauthagent.controller

import io.curity.oauthagent.exception.OAuthAgentException
import io.curity.oauthagent.exception.UnhandledException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import java.util.ArrayList

@ControllerAdvice
class ExceptionController
{
    @ExceptionHandler(Throwable::class)
    @ResponseBody
    fun handleException(caught: Throwable, request: ServerHttpRequest, response: ServerHttpResponse): ErrorMessage
    {
        val exception = if (caught is OAuthAgentException) caught else UnhandledException(caught)

        logError(exception, request)
        response.statusCode = HttpStatus.valueOf(exception.statusCode)

        return ErrorMessage(
            exception.code,
            exception.message
        )
    }

    /*
     * Only output logs when there is an error, and only output stack traces for 5xx errors
     */
    private fun logError(exception: OAuthAgentException, request: ServerHttpRequest) {

        val fields = ArrayList<String>()
        fields.add(request.method.toString())
        fields.add(request.path.toString())
        fields.addAll(exception.getLogFields())

        if (exception.statusCode >= 500) {
            if (logger.isErrorEnabled) {
                logger.error(fields.joinToString())
            }

        } else {
            if (logger.isInfoEnabled) {
                logger.info(fields.joinToString())
            }
        }
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
