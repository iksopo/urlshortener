package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    /**
     * Subtracts 1 to the number of uses left from [su]
     * @return true if only the url was not expired and couldn't update uses left
     */
    override fun updateLeftUses(su : ShortUrl) : Boolean {
        su.properties.leftUses?.let {
            val updatedRows = shortUrlEntityRepository.updateLeftUsesByHash(su.hash)
            return updatedRows == 1
        } ?: run {
            return true
        }
    }

    /**
     * Checks if [su] has expired without checking the database
     */
    override fun checkNotExpired(su : ShortUrl) : Boolean {
        su.properties.expiration?.let {
            val suDate = it.toInstant().atOffset(ZoneOffset.UTC)
            val now = OffsetDateTime.now()
            return now < suDate
        } ?: return true
    }

    /**
     * Deletes all expired URIs by non uses left and date
     * @return Pair containing the number of deleted rows: {expiredDate, expiredLeftUses}
     */
    override fun deleteExpireds() : Pair<Int, Int> {
        val now = OffsetDateTime.now()
        val expiredDate = shortUrlEntityRepository.deleteByExpirationBefore(Date.from(now.toInstant()))
        val expiredLeftUses = shortUrlEntityRepository.deleteByLeftUsesEquals(0)
        return Pair(expiredDate, expiredLeftUses)
    }

    override fun deleteByKey(key: String) {
        shortUrlEntityRepository.deleteByHash(key)
    }
    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
}
