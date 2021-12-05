package es.unizar.urlshortener

import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class PeriodicDelete : RouteBuilder() {

    // @todo
    override fun configure() {
        from("scheduler://foo?delay=60000")
            .to("bean:shortUrlRepositoryService?method=deleteExpireds")
    }
}