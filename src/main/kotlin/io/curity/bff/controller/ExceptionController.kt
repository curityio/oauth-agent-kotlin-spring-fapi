package io.curity.bff.controller

import io.curity.bff.exception.BFFException
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import javax.servlet.http.HttpServletResponse

@ControllerAdvice
class ExceptionController
{
    @ExceptionHandler(BFFException::class)
    @ResponseBody
    fun handleException(exception: BFFException, response: HttpServletResponse): ErrorMessage
    {
        logger.info("Exception occured during request: {}", exception.logMessage, exception)

        response.status = exception.statusCode

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
