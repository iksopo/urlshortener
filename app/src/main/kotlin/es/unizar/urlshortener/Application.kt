package es.unizar.urlshortener

import es.unizar.urlshortener.core.FileStorage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

/**
 * The marker that makes this project a Spring Boot application.
 */
@SpringBootApplication
class UrlShortenerApplication{
    @Autowired
    lateinit var fileStorage: FileStorage

    @Bean
    fun run() = CommandLineRunner {
        fileStorage.deleteAll()
        fileStorage.init()
    }
}

/**
 * The main entry point.
 */
fun main(args: Array<String>) {
    runApplication<UrlShortenerApplication>(*args)
}
