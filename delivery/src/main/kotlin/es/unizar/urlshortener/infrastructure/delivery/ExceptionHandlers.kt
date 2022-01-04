package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.FileDoesNotExist
import es.unizar.urlshortener.core.InvalidTypeOfFile
import es.unizar.urlshortener.core.InvalidUrlException
import es.unizar.urlshortener.core.RedirectionNotFound
import org.springframework.beans.BeanInstantiationException
import org.springframework.http.HttpHeaders
import es.unizar.urlshortener.core.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ResponseBody
    @ExceptionHandler(value = [InvalidUrlException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun invalidUrls(ex: InvalidUrlException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [RedirectionNotFound::class])
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected fun redirectionNotFound(ex: RedirectionNotFound) = ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [InvalidTypeOfFile::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun invalidTypeOfFile(ex: InvalidTypeOfFile) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [WrongStructuredFile::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun wrongStructuredFile(ex: WrongStructuredFile) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [FileDoesNotExist::class])
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected fun fileDoesNotExist(ex: FileDoesNotExist) = ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.message)

    @ExceptionHandler(BeanInstantiationException::class)
    fun illegalArgumentException(e: Exception): ErrorMessage {
        println("?")
        val nested = e.cause as Exception
        return ErrorMessage(HttpStatus.BAD_REQUEST.value(), nested.localizedMessage)
    }

    @ExceptionHandler(Exception::class)
    fun internalServerErrorException(e: Exception): ErrorMessage {
        println("?")
        if (Regex("Invalid Date").containsMatchIn(e.localizedMessage)) {
            return ErrorMessage(HttpStatus.BAD_REQUEST.value(), "Date must have a correct format.")
        }

        return ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Generic internal error")
    }
}

data class ErrorMessage(
    val statusCode: Int,
    val message: String?,
    val timestamp: String = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now())
)