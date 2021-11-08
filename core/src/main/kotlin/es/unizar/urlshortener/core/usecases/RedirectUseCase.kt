package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import java.time.OffsetDateTime

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {
    fun redirectTo(key: String): Redirection
}

/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : RedirectUseCase {
    override fun redirectTo(key: String): Redirection =
        shortUrlRepository.findByKey(key)?.let {
            var shortUrlToUpdate = it
            shortUrlToUpdate.properties.leftUses?.let {
                    shortUrlToUpdate.properties.leftUses = it - 1;
                    if (it == 1) {
                        shortUrlRepository.deleteByHash(shortUrlToUpdate.hash)
                    }
            }
            shortUrlToUpdate.properties.expiration?.let {
                val now = OffsetDateTime.now();
                if (now.compareTo(shortUrlToUpdate.properties.expiration) >= 0) {
                    shortUrlRepository.deleteByHash(shortUrlToUpdate.hash)
                    throw RedirectionNotFound(key)
                }
            }
            // Actualizar de la bbdd
            return it.redirection
        } ?: throw RedirectionNotFound(key)
}

