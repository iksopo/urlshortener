package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpRequestTest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun setup() {
        /*
        val httpClient = HttpClientBuilder.create().disableRedirectHandling().build()
        (restTemplate.restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory).httpClient = httpClient
        */

        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @AfterEach
    fun tearDowns() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @Test
    fun `main page works`() {
        val response = restTemplate.getForEntity("http://localhost:$port/", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("A front-end example page for the project")
    }

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        val target = shortUrl("http://example.com/").headers.location
        require(target != null)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(1)
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        val response = restTemplate.getForEntity("http://localhost:$port/f684a3c4", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        val response = shortUrl("http://example.com/")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/tiny-f684a3c4"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/tiny-f684a3c4"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns bad request if it can't compute a hash`() {
        val response = shortUrl("http://example.com/")
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/tiny-f684a3c4"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/tiny-f684a3c4"))
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates an url with 1 uses left and fetchs it twice`() {
        val sUrl = shortUrl("http://example.com/", 1)
        val target = sUrl.headers.location
        require(target != null)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        val response2 = restTemplate.getForEntity(target, String::class.java)
        assertThat(response2.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `creates an url with 2 left uses and fetchs it three times`() {
        val sUrl = shortUrl("http://example.com/", 2)
        val target = sUrl.headers.location
        require(target != null)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        val response2 = restTemplate.getForEntity(target, String::class.java)
        assertThat(response2.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response2.headers.location).isEqualTo(URI.create("http://example.com/"))

        val response3 = restTemplate.getForEntity(target, String::class.java)
        assertThat(response3.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `csv page works`() {
        val response = restTemplate.getForEntity("http://localhost:$port/csv", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("URL Shortener: CSV")
    }

    @Test
    fun `asks with bad number format`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "http://example.com/"
        data["leftUses"] = "Definitely not a number"
        val response = restTemplate.postForEntity("http://localhost:$port/api/link", HttpEntity(data, headers), ShortUrlDataOut::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `asks with bad date format`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "http://example.com/"
        data["expiration"] = "Definitely not a number"
        val response = restTemplate.postForEntity("http://localhost:$port/api/link", HttpEntity(data, headers), ShortUrlDataOut::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `asks with 0 uses left`() {
        val response = shortUrl("http://example.com/", leftUses = 0)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }


    @Test
    fun `creates an url that expires in 20 seconds`() {
        val sUrl = shortUrl("https://www.google.com/", null, OffsetDateTime.now().plusSeconds(20))
        println(sUrl)
        val target = sUrl.headers.location
        require(target != null)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("https://www.google.com/"))

        val response2 = restTemplate.getForEntity(target, String::class.java)
        assertThat(response2.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response2.headers.location).isEqualTo(URI.create("https://www.google.com/"))

        Thread.sleep(21_000)

        val response3 = restTemplate.getForEntity(target, String::class.java)
        assertThat(response3.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `creates an url with a lot of uses and tries to generate concurrency issues`() {
        val numberUses = 500
        val sUrl = shortUrl("http://example.com/", numberUses, null)
        println(sUrl)
        val target = sUrl.headers.location
        require(target != null)
        var redirections = 0
        runBlocking {
            repeat(numberUses + 1){
                launch {
                    val response = restTemplate.getForEntity(target, String::class.java)
                    if (response.statusCode == HttpStatus.TEMPORARY_REDIRECT){
                        synchronized(redirections){
                            redirections++
                        }
                    }
                }
            }
        }
        assertThat(redirections).isEqualTo(numberUses)
    }

    @Test
    fun `metrics health works`() {
        val response = restTemplate.getForEntity("http://localhost:$port/actuator/health", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("UP")
    }

    @Test
    fun `metrics process uptime works`() {
        val response = restTemplate.getForEntity("http://localhost:$port/actuator/metrics/process.uptime", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `expiration uses URIs gets deleted async`() {
        val sUrl = shortUrl("http://example.com/", 1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        val target = sUrl.headers.location
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))
        var rows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")
        var i = 0
        while (i < 5 && rows == 1){
            Thread.sleep(5_000)
            rows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")
            i++
        }
        assertThat(rows).isEqualTo(0)
    }

    @Test
    fun `expiration time URIs gets deleted async`() {
        val sUrl = shortUrl("http://example.com/", expiration=OffsetDateTime.now().plusSeconds(10))
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        val target = sUrl.headers.location
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))
        var rows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")
        var i = 0
        while (i < 10 && rows == 1){
            Thread.sleep(5_000)
            rows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")
            i++
        }
        assertThat(rows).isEqualTo(0)
    }

    private fun shortUrl(url: String, leftUses: Int? = null, expiration: OffsetDateTime? = null): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url
        leftUses?.let {
            data["leftUses"] = it.toString()
        }
        expiration?.let {
            data["expiration"] = expiration.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }
        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java
        )
    }

}
