package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.InvalidDateException
import es.unizar.urlshortener.core.NullUrl
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * The specification of the controller.
 */
@Api(value = "example", description = "Shortener API", tags = ["API to shorten URIs"])
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    @ApiOperation(value = "Redirect to a shortened uri")
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "OK"),
            ApiResponse(code = 301, message = "Redirect successful"),
            ApiResponse(code = 400, message = "Uri is being validated"),
            ApiResponse(code = 404, message = "Uri doesn't exist")]
    )
    fun redirectTo(
        @ApiParam(value = "Id", example = "a3828e31",type = "String",required = true )
        id: String?,
        request: HttpServletRequest): ResponseEntity<Void>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    @ApiOperation(value = "Shorten url")
    @ApiResponses(
        value = [
            ApiResponse(code = 201, message = "Uri created"),
            ApiResponse(code = 400, message = "Bad request, missing parameter or wrong format")]
    )
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

}

/**
 * Data required to create a short url.
 * LeftUses and expiration are strings to handle parsing errors properly,
 * if they were as Int and Date, Spring throws exceptions and catches them internally,
 * without a proper error message.
 */
@ApiModel(value="Input ShortUrl info entity")
data class ShortUrlDataIn(
    @ApiModelProperty(name = "url", dataType = "String", required = true, example = "http//www.example.com")
    val url: String? = null,
    @ApiModelProperty(name = "leftUses", dataType = "Integer", required = false, example = "4")
    val leftUses: String? = null,
    @ApiModelProperty(name = "expiration", dataType = "Date ISO_OFFSET_DATE_TIME format", required = false, example = "123")
    val expiration: String? = null,
    @ApiModelProperty(name = "sponsor", dataType = "String", required = false, hidden = true)
    val sponsor: String? = null
)

/**
 * Data returned after the creation of a short url.
 */
@ApiModel(value="Output ShortUrl info entity")
data class ShortUrlDataOut(
    @ApiModelProperty(name = "url", dataType = "String", example = "tiny-6f12359f")
    val url: URI? = null,
    @ApiModelProperty(name = "properties", dataType = "Map with some properties and metadata")
    val properties: Map<String, Any> = emptyMap()
)

fun parseDate(date: String): Date {
    try {
        val dateTime = OffsetDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return Date.from(dateTime.toInstant())
    } catch (ex: DateTimeParseException){
        throw InvalidDateException(date)
    }
}
/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase
) : UrlShortenerController {

    @Autowired
    lateinit var validateURIUseCase: ValidateURIUseCase

    @Timed(value="redirection.time")
    @GetMapping("/tiny-{id:.*}")
    override fun redirectTo(@ApiParam(name = "id", required = true,  value = "Shortened url", example="tiny-6f12359f")
                                @PathVariable id: String?, request: HttpServletRequest): ResponseEntity<Void> =
     redirectUseCase.redirectTo(id?: throw NullUrl()).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))

            val h = HttpHeaders()

            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }

    @PostMapping("/api/link", consumes = [ MediaType.APPLICATION_FORM_URLENCODED_VALUE ])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url ?: throw NullUrl(),
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                leftUses = data.leftUses?.toInt(),
                expiration = data.expiration?.let{parseDate(it)}
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "validating" to it.properties.safe
                )
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
}

@Configuration
class RestTemplateConfig {

    /**
     * Build a RestTemplate Bean with the default configuration
     */
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplateBuilder().build()
    }
}
