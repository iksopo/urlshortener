package es.unizar.urlshortener.core

import es.unizar.urlshortener.core.usecases.ValidateURISTATUS

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
    fun updateLeftUses(su : ShortUrl): Boolean
    fun checkNotExpired(su : ShortUrl) : Boolean
    fun deleteExpireds() : Pair<Int, Int>
    fun deleteByKey(key: String)
    fun updateValidation(key: String, status: ValidateURISTATUS): Boolean
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}