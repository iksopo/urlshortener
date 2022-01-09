package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.InvalidDateException
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

}

@Value("\${google.API.value}")
const val APIKEY : String = "AIzaSyBS26eLBuGZEmscRx9AmUVG8O_YaiwgDu0"
const val FINDURL : String = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=$APIKEY"
const val CLIENTNAME : String = "URIShortenerTestApp"
const val CLIENTVERSION : String = "0.1"


/**
 * Data required to create a short url.
 * LeftUses and expiration are strings to handle parsing errors properly,
 * if they were as Int and Date, Spring throws exceptions and catches them internally,
 * without a proper error message.
 */
data class ShortUrlDataIn(
    val url: String,
    val leftUses: String? = null,
    val expiration: String? = null,
    val sponsor: String? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
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
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
     redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))

            val h = HttpHeaders()

            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }

    @PostMapping("/api/link", consumes = [ MediaType.APPLICATION_FORM_URLENCODED_VALUE ])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
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

            //Check URL is valid
            //https://testsafebrowsing.appspot.com/s/unwanted.html URI maliciosa de ejemplo para probar.

            if(it.validation==ValidateURISTATUS.VALIDATION_FAIL_UNSAFE){
                val response = ShortUrlDataOut(
                    url = url,
                    properties = mapOf(
                        "Unsafe" to it.properties.safe,
                    )
                )
                return ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.FORBIDDEN)
            }
            if(it.validation==ValidateURISTATUS.VALIDATION_FAIL_UNREACHABLE){
                val response = ShortUrlDataOut(
                    url = url,
                    properties = mapOf(
                        "Not Reachable" to it.properties.safe,
                    )
                )
                return ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.NOT_FOUND)
            }


            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
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
