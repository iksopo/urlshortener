package es.unizar.urlshortener

import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class PeriodicDelete : RouteBuilder() {

    /**
     * Configures a route to delete periodically expired urls
     */
    override fun configure() {
        from("scheduler://foo?delay=60000")
            .to("bean:shortUrlRepositoryService?method=deleteExpireds")
    }
}