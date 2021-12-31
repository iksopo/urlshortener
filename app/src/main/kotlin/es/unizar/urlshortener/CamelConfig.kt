package es.unizar.urlshortener

import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class CamelConfig : RouteBuilder() {

    /**
     * Configures a route to delete periodically expired urls
     */
    override fun configure() {
        from("quartz://periodicDeleterGroup/periodicDelete"
                + "?cron=0+*/1+*+?+*+*?"
                + "")
            .to("bean:shortUrlRepositoryService?method=deleteExpireds")
    }
}