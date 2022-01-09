package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

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
            val validated = it.validation
            if (usable && ValidateURISTATUS.VALIDATION_PASS == validated){
                return it.redirection
            } else {
                if(!usable) {
                    throw RedirectionNotFound(key)
                }
                if (ValidateURISTATUS.VALIDATION_IN_PROGRESS == validated){
                    throw ValidationInProcess(key)
                }
                if (ValidateURISTATUS.VALIDATION_FAIL_UNSAFE == validated){
                    throw UriUnsafe(key)
                }
                if (ValidateURISTATUS.VALIDATION_FAIL_UNREACHABLE == validated){
                    throw UriUnreachable(key)
                }
                throw RedirectionNotFound(key)
            }
        } ?: throw RedirectionNotFound(key)
}

