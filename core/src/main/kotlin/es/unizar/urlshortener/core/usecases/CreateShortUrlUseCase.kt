package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired



/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService,

) : CreateShortUrlUseCase {
    @Autowired
    lateinit var validateURIUseCase: ValidateURIUseCase

    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
        if (data.leftUses != null && data.leftUses <= 0) throw InvalidLeftUses(data.leftUses.toString())
        else if(validatorService.isValid(url)) {
            runBlocking {
                val validationResponse = async { validateURIUseCase.ValidateURI(url) }

                val id: String = hashService.hasUrl(url)
                var su = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    validation = ValidateURISTATUS.VALIDATION_PASS,
                    properties = ShortUrlProperties(
                        safe = data.safe,
                        ip = data.ip,
                        sponsor = data.sponsor,
                        leftUses = data.leftUses,
                        expiration = data.expiration
                    )
                )
                shortUrlRepository.save(su)
                var validationResult = ValidateURISTATUS.VALIDATION_PASS
                if (validationResponse.await() == ValidateURIUseCaseResponse.UNSAFE) {
                    validationResult = ValidateURISTATUS.VALIDATION_FAIL_UNSAFE
                    shortUrlRepository.deleteByKey(su.hash)
                    throw UriUnsafe(url)
                }
                if (validationResponse.await() == ValidateURIUseCaseResponse.NOT_REACHABLE) {
                    validationResult = ValidateURISTATUS.VALIDATION_FAIL_UNREACHABLE
                    shortUrlRepository.deleteByKey(su.hash)
                    throw UriUnreachable(url)
                }
                su.validation = validationResult
                su
            }
        } else throw InvalidUrlException(url)
}

