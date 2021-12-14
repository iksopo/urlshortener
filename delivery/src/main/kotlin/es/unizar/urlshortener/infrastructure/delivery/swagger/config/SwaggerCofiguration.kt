package es.unizar.urlshortener.infrastructure.delivery.swagger.config


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

/**
 * Swagger creates a web interface to document and use exposed application methods.
 * Several annotations can be used to customize the messages and inputs.
 * To access the page, go to use "RouteToWebApp/swagger-ui.html, where RouteToWebApp is where the
 * the applications is deployed. In a local case, it would be localhost:8080.
 */


@Configuration
@EnableSwagger2
class SwaggerCofiguration {

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .apiInfo(getApiInfo())
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.any())
            .build()
    }

    private fun getApiInfo(): ApiInfo {
        val contact = Contact("Project Overlord", "https://github.com/iksopo/urlshortener", "example@mail.com")
        return ApiInfoBuilder()
            .title("URI Shortener API")
            .description("URI Shortener API developed by group C ")
            .version("1.0.1")
            .license("Apache 2.0")
            .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
            .contact(contact)
            .build()
    }
}