package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrl
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.persistence.PersistenceContext

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    fun findByHash(hash: String): ShortUrlEntity?
    @Transactional
    fun findUpdatingByHash(hash: String) : ShortUrlEntity? {
        val url = findByHash(hash)
        println("Before: " + url)
        url?.let {
            it.leftUses?.let {
                if (it == 1) {
                    deleteByHash(hash)
                } else {
                    url.leftUses = it -1
                    println("Updated" +  url.leftUses)
                    save(url)
                }
            }
            it.expiration?.let {
                val now = OffsetDateTime.now();
                if (now.compareTo(it.toInstant().atOffset(ZoneOffset.UTC)) > 0) {
                    deleteByHash(hash)
                    return null
                }
            }
            println("After: " + url)
        }
        return url
    }

    @Transactional
    fun deleteByHash(hash: String)
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long>