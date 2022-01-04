package es.unizar.urlshortener

import es.unizar.urlshortener.core.FileStorage
import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository,
    @Autowired var fileStorage: FileStorage
) {
    @Bean
    fun cleanFileStorage() {
        fileStorage.deleteAll()
        fileStorage.init()
    }

    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun createShortUrlUseCase() = CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())

    @Bean
    fun createShortUrlsFromCsvUseCase() = CreateShortUrlsFromCsvUseCaseImpl(fileStorage, createShortUrlUseCase())

    @Bean
    fun ValidateURIUseCase() = ValidateURIUseCaseImpl()

    @Bean
    fun timedAspect(registry: MeterRegistry): TimedAspect = TimedAspect(registry)
}


