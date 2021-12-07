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
    override fun updateLeftUses(su : ShortUrl) {
        su.properties.leftUses?.let {
            if (it == 1) {
                shortUrlEntityRepository.deleteByHash(su.hash)
            } else {
                su.properties.leftUses = it -1
                save(su)
            }
        }
    }
    override fun checkNotExpired(su : ShortUrl) : Boolean {
        su.properties.expiration?.let {
            val now = OffsetDateTime.now();
            if (now.compareTo(it.toInstant().atOffset(ZoneOffset.UTC)) > 0) {
                shortUrlEntityRepository.deleteByHash(su.hash)
                return false
            } else {
                return true
            }
        } ?: return true
    }

    override fun deleteExpireds() {
        val now = OffsetDateTime.now()
        println("Deleting all expired dates... (before: " + now + ")")
        shortUrlEntityRepository.deleteByExpirationBefore(Date.from(now.toInstant()))
    }

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
}
