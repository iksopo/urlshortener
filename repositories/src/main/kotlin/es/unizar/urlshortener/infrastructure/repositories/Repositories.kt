package es.unizar.urlshortener.infrastructure.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.LockModeType

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    fun findByHash(hash: String): ShortUrlEntity?

    @Transactional
    fun deleteByHash(hash: String)

    @Transactional
    fun deleteByExpirationBefore(date: Date) : Int

    @Transactional
    fun deleteByLeftUsesEquals(leftUses: Int) : Int

    @Transactional
    @Modifying
    @Query("UPDATE ShortUrlEntity surl SET surl.leftUses = surl.leftUses - 1 where surl.hash = ?1 AND surl.leftUses > 0")
    fun updateLeftUsesByHash(hash: String): Int
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long>