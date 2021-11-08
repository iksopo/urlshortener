package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

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
            if (now.compareTo(it) > 0) {
                shortUrlEntityRepository.deleteByHash(su.hash)
                return false
            } else {
                return true
            }
        } ?: return true
    }
    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
}
