package es.unizar.urlshortener.infrastructure.delivery

import com.fasterxml.jackson.annotation.JsonFormat
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.springframework.beans.BeanInstantiationException
import org.springframework.beans.TypeMismatchException
import org.springframework.core.convert.ConversionFailedException
import org.springframework.http.*
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.validation.ConstraintViolationException

// https://betterprogramming.pub/environment-based-error-handling-with-spring-boot-and-kotlin-b36b901135ad
data class ErrorResponse(
    val status: HttpStatus,
    val message: String,
    val localizedMessage: String? = null
)

@ControllerAdvice
class ControllerExceptionsHandler {

    @ExceptionHandler(
        BeanInstantiationException::class,
    )
    fun illegalArgumentException(e: Exception): ResponseEntity<ErrorResponse> {
        val nested = e.cause as Exception
        return generateErrorResponse(HttpStatus.BAD_REQUEST, nested.localizedMessage, nested)
    }

    @ExceptionHandler(
        Exception::class
    ) // No quedaba otra que hacerlo así porque lanza algo anónimo (extraño)
    fun internalServerErrorException(e: Exception): ResponseEntity<ErrorResponse> {
        if (Regex("Invalid Date").containsMatchIn(e.localizedMessage)) {
            return generateErrorResponse(HttpStatus.BAD_REQUEST, "Date must have a correct format.", e)
        }
        return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Generic internal error", e)
    }

    private fun generateErrorResponse(
        status: HttpStatus,
        message: String,
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        println("---*---")
        println(e.javaClass.simpleName)
        println(status)
        println(message)
        println(e.localizedMessage)
        println("---*---")
        return ResponseEntity(ErrorResponse(status, message, e.message), status)
    }
}