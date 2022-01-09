package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.util.Date

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
    private val hashService: HashService
) : CreateShortUrlUseCase {
    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
        if (validatorService.isValid(url)) {
            var id: String = hashService.hasUrl(url)
            if (data.leftUses != null || data.expiration != null) {
                id += hashService.hasUrl((System.currentTimeMillis()).toString())
            }
            val su = ShortUrl(
                hash = id,
                redirection = Redirection(target = url),
                properties = ShortUrlProperties(
                    safe = data.safe,
                    ip = data.ip,
                    sponsor = data.sponsor,
                    leftUses = data.leftUses,
                    expiration = data.expiration
                )
            )
            shortUrlRepository.save(su)
        } else {
            throw InvalidUrlException(url)
        }
}
