package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.ShortUrl
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import javax.persistence.PersistenceContext
import java.util.Date
import javax.persistence.LockModeType

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByHash(hash: String): ShortUrlEntity?

    @Transactional
    fun deleteByHash(hash: String)

    @Transactional
    fun deleteByExpirationBefore(date: Date)
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long>