package es.unizar.urlshortener.infrastructure.delivery

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

    @ResponseBody
    @ExceptionHandler(value = [InvalidLeftUses::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidLeftUses(ex: InvalidLeftUses): ErrorMessage {
        return ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)
    }

    @ResponseBody
    @ExceptionHandler(value = [InvalidDateException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidDate(ex: InvalidDateException): ErrorMessage {
        return ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)
    }

    @ResponseBody
    @ExceptionHandler(value = [NumberFormatException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidNumberFormat(ex: NumberFormatException): ErrorMessage {
        return ErrorMessage(HttpStatus.BAD_REQUEST.value(), "Invalid number format used.")
    }

    @ResponseBody
    @ExceptionHandler(value = [ValidationInProcess::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun validationInProcess(ex: ValidationInProcess) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [UriUnreachable::class])
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected fun uriUnreachable(ex: UriUnreachable) = ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [UriUnsafe::class])
    @ResponseStatus(HttpStatus.FORBIDDEN)
    protected fun uriUnsafe(ex: UriUnsafe) = ErrorMessage(HttpStatus.FORBIDDEN.value(), ex.message)


    @ResponseBody
    @ExceptionHandler(value = [NullUrl::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun nullUrls(ex: InvalidUrlException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)
}

data class ErrorMessage(
    val statusCode: Int,
    val message: String?,
    val timestamp: String = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now())
)