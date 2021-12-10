package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.transaction.annotation.Transactional
import javax.persistence.LockModeType

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
open class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : RedirectUseCase {
    override fun redirectTo(key: String): Redirection =
        shortUrlRepository.findByKey(key)?.let {
            val usable = shortUrlRepository.updateLeftUses(it) && shortUrlRepository.checkNotExpired(it)
            if (usable){
                return it.redirection
            } else {
                GlobalScope.launch {
                    shortUrlRepository.deleteExpireds(key)
                }
                throw RedirectionNotFound(key)
            }
        } ?: throw RedirectionNotFound(key)
}

