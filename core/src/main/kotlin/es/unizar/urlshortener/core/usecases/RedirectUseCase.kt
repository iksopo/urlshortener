package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional
    override fun redirectTo(key: String): Redirection =
        shortUrlRepository.findByKey(key)?.let {
            GlobalScope.launch {
                shortUrlRepository.updateLeftUses(it)
                shortUrlRepository.checkNotExpired(it)
            }
            return it.redirection
        } ?: throw RedirectionNotFound(key)
}

