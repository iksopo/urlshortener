package es.unizar.urlshortener.infrastructure.delivery

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.servlet.http.HttpServletRequest
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate

import javax.validation.constraints.Min;

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
 */
data class ShortUrlDataIn(
    val url: String,
    val leftUses: Int? = null,
    val expiration: Date? = null,
    val sponsor: String? = null
) {
    init {
        leftUses?.let{ require(it > 0) { "leftUses must be greater than 0." }}
    }
}

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)


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

    @GetMapping("/tiny-{id:.*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))

            val h = HttpHeaders()

            //Check URL is valid
            val validationResponse = validateURIUseCase.ValidateURI(it.target)
            if(validationResponse==ValidateURIUseCaseResponse.UNSAFE){
                return ResponseEntity(h, HttpStatus.FORBIDDEN)
            }
            if(validationResponse==ValidateURIUseCaseResponse.NOT_REACHABLE){
                return ResponseEntity(h, HttpStatus.NOT_FOUND)
            }

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
                leftUses = data.leftUses,
                expiration = data.expiration
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url

            //Check URL is valid
            //https://testsafebrowsing.appspot.com/s/unwanted.html URI maliciosa de ejemplo para probar.
            val validationResponse = validateURIUseCase.ValidateURI(data.url)

            if(validationResponse==ValidateURIUseCaseResponse.UNSAFE){
                val response = ShortUrlDataOut(
                    url = url,
                    properties = mapOf(
                        "Unsafe" to it.properties.safe,
                    )
                )
                return ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.FORBIDDEN)
            }
            if(validationResponse==ValidateURIUseCaseResponse.NOT_REACHABLE){
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
    fun restTemplate():RestTemplate {
        return RestTemplateBuilder().build()
    }
}