package es.unizar.urlshortener.infrastructure.delivery

import com.fasterxml.jackson.annotation.JsonFormat
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
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
class ErrorResponse(
    status: HttpStatus,
    val message: String,
    var stackTrace: String? = null
) {
    var code: Int = 200
    var statusString: String = ""

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd-yyyy hh:mm:ss")
    val timestamp: Date = Date()
    init {
        code = status.value()
        statusString = status.name
    }
}

@ControllerAdvice
class ControllerExceptionsHandler {

    @ExceptionHandler(
        ConstraintViolationException::class,
        HttpClientErrorException.BadRequest::class,
        MethodArgumentNotValidException::class,
        MissingServletRequestParameterException::class,
        IllegalArgumentException::class
    )
    fun constraintViolationException(e: Exception): ResponseEntity<ErrorResponse> {
        return generateErrorResponse(HttpStatus.BAD_REQUEST, "Bad request", e)
    }

    @ExceptionHandler(
        //EntityNotFoundException::class,
        NoSuchElementException::class,
        //NoResultException::class,
        //EmptyResultDataAccessException::class,
        IndexOutOfBoundsException::class,
        KotlinNullPointerException::class
    )

    fun notFoundException(e: Exception): ResponseEntity<ErrorResponse> {
        return generateErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", e)
    }

    @ExceptionHandler(
        Exception::class
    )
    fun internalServerErrorException(e: Exception): ResponseEntity<ErrorResponse> {
        return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Generic internal error", e)
    }

    private fun generateErrorResponse(
        status: HttpStatus,
        message: String,
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        // converting the exception stack trace to a string
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        val stackTrace = sw.toString()

        // example: logging the stack trace
        println("---*---")
        println(message)
        println(status)
        println(e.localizedMessage)
        println("---*---")

        if (Regex("Invalid Date").containsMatchIn(e.localizedMessage)) {
            return ResponseEntity(ErrorResponse(HttpStatus.BAD_REQUEST, "Date must have a correct format.", e.localizedMessage), status)
        }

        return ResponseEntity(ErrorResponse(status, message, e.message), status)

    }

}