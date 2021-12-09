package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
 */
data class ShortUrlDataIn(
    val url: String,
    val leftUses: Int? = null,
    val expiration: Date? = null,
    val sponsor: String? = null
)

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
            var status = HttpStatus.valueOf(it.mode)


            //Check URL is valid
            runBlocking {
                val validationResponse = async { validateURIUseCase.ValidateURI(it.target) }
                if (validationResponse.await() == ValidateURIUseCaseResponse.UNSAFE) {
                    status = HttpStatus.FORBIDDEN
                }
                if (validationResponse.await() == ValidateURIUseCaseResponse.NOT_REACHABLE) {
                    status = HttpStatus.NOT_FOUND
                }
            }

            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, status)
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
            var response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                )
            )

            var status = HttpStatus.CREATED

            //Check URL is valid
            //https://testsafebrowsing.appspot.com/s/unwanted.html URI maliciosa de ejemplo para probar.
            runBlocking {
                val validationResponse = async { validateURIUseCase.ValidateURI(data.url) }

                if (validationResponse.await() == ValidateURIUseCaseResponse.UNSAFE) {
                    response = ShortUrlDataOut(
                        url = url,
                        properties = mapOf(
                            "Unsafe" to it.properties.safe,
                        )
                    )
                    status = HttpStatus.FORBIDDEN
                }
                if (validationResponse.await() == ValidateURIUseCaseResponse.NOT_REACHABLE) {
                     response = ShortUrlDataOut(
                        url = url,
                        properties = mapOf(
                            "Not Reachable" to it.properties.safe,
                        )
                    )
                    status = HttpStatus.NOT_FOUND
                }
            }

            println("after asyncs")
            return ResponseEntity<ShortUrlDataOut>(response, h, status)
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