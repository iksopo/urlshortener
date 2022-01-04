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
    override fun updateLeftUses(su : ShortUrl) : Boolean {
        su.properties.leftUses?.let {
            val updatedRows = shortUrlEntityRepository.updateLeftUsesByHash(su.hash)
            return updatedRows == 1
        } ?: run {
            return true
        }
    }
    override fun checkNotExpired(su : ShortUrl) : Boolean {
        su.properties.expiration?.let {
            val suDate = it.toInstant().atOffset(ZoneOffset.UTC)
            val now = OffsetDateTime.now();
            println("Su: " + suDate + ", now: " + now + " = " + (now < suDate))
            return now < suDate
        } ?: return true
    }

    override fun deleteExpireds() {
        val now = OffsetDateTime.now()
        shortUrlEntityRepository.deleteByExpirationBefore(Date.from(now.toInstant()))
    }

    override fun deleteByKey(key: String) {
        shortUrlEntityRepository.deleteByHash(key)
    }
    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
}
