package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.test.util.AssertionErrors.assertEquals
import org.springframework.test.util.AssertionErrors.assertNotEquals
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.context.WebApplicationContext
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
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

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

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
        Thread.sleep(21_000)
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
        Thread.sleep(21_000)
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
        Thread.sleep(10_000)
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
        Thread.sleep(10_000)
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
        Thread.sleep(10_000)
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
    fun `uploadCsv throws exception when the type of file is invalid`() {
        val uuid = getUuid()
        val sentFile = fileToSend("notACsv.txt")
        val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        mockMvc.perform(
            multipart("/csv")
            .file(sentFile)
            .param("uuid", uuid))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `uploadCsv throws exception when all lines in the file have invalid format`() {
        val uuid = getUuid()
        val sentFile = fileToSend("onlyInvalidLines.csv")
        val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        mockMvc.perform(
            multipart("/csv")
                .file(sentFile)
                .param("uuid", uuid))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `uploadCsv returns empty file when the sent one is empty`() {
        val uuid = getUuid()
        val sentFile = fileToSend("empty.csv")
        val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        mockMvc.perform(
            multipart("/csv")
                .file(sentFile)
                .param("uuid", uuid))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.content().contentType("text/csv"))
            .andExpect(MockMvcResultMatchers.content().bytes(fileBytes("empty.csv")))
    }

    @Test
    fun `uploadCsv returns a file when first url is valid`() {
        val uuid = getUuid()
        val sentFile = fileToSend("firstUrlIsValid.csv")
        val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        val result = mockMvc.perform(
            multipart("/csv")
                .file(sentFile)
                .param("uuid", uuid))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.content().contentType("text/csv")).andReturn()

        assertThat(result.response.getHeaderValue("Location").toString()).isNotEmpty

        Thread.sleep(10_000)
        val fileContent = result.response.contentAsString
        val requestedUrls = listOf("http://unizar.es", "https://drive.google.com", "https://moodle.unizar.es", "invalid line",
            "http://www.google.es", "https://www.google.com", "https://www.netflix.com", "https://www.youtube.com")
        val errorUrls = listOf("invalid line", "https://www.google.com", "https://www.netflix.com", "https://www.youtube.com")
        val createdButErrorUrls = listOf("https://moodle.unizar.es")
        checkFile(fileContent, requestedUrls, errorUrls, createdButErrorUrls)
    }

    @Test
    fun `uploadCsv returns a file when first url is invalid`() {
        val uuid = getUuid()
        val sentFile = fileToSend("firstUrlIsInvalid.csv")
        val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        val result = mockMvc.perform(
            multipart("/csv")
                .file(sentFile)
                .param("uuid", uuid))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.content().contentType("text/csv")).andReturn()

        assertThat(result.response.getHeaderValue("Location").toString()).isNotEmpty

        Thread.sleep(10_000)
        val fileContent = result.response.contentAsString
        val requestedUrls = listOf("http://unizar.es", "https://drive.google.com", "https://moodle.unizar.es", "invalid line",
            "http://www.google.es", "https://www.google.com", "https://www.netflix.com", "https://www.youtube.com")
        val errorUrls = listOf("invalid line", "https://www.google.com", "https://www.netflix.com", "https://www.youtube.com")
        val createdButErrorUrls = listOf("http://www.google.es")
        checkFile(fileContent, requestedUrls, errorUrls, createdButErrorUrls)
    }

    @Test
    fun `uploadCsv returns a file when format of first line is invalid`() {
        val uuid = getUuid()
        val sentFile = fileToSend("firstLineIsIncomplete.csv")
        val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        val result = mockMvc.perform(
            multipart("/csv")
                .file(sentFile)
                .param("uuid", uuid))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.content().contentType("text/csv")).andReturn()

        assertThat(result.response.getHeaderValue("Location").toString()).isNotEmpty

        Thread.sleep(10_000)
        val fileContent = result.response.contentAsString
        val requestedUrls = listOf("http://unizar.es", "https://google.es", "https://moodle.unizar.es")
        val errorUrls = listOf("http://unizar.es")
        val createdButErrorUrls = ArrayList<String>()
        checkFile(fileContent, requestedUrls, errorUrls, createdButErrorUrls)
    }
/*
    @Test
    fun `uploadCsv returns a file when sent file has more than a hundred lines`() {
        val uuid = getUuid()
        val sentFile = fileToSend("aLotOfUrls.csv")
        val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        val result = mockMvc.perform(
            multipart("/csv")
                .file(sentFile)
                .param("uuid", uuid))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.content().contentType("text/csv")).andReturn()

        assertThat(result.response.getHeaderValue("Location").toString()).isNotEmpty

        Thread.sleep(120_000)
        val fileContent = result.response.contentAsString
        val requestedUrls = listOf("http://www.google.es", "http://unizar.es", "https://moodle.unizar.es", "invalid line",
            "https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html#mutual-exclusion",
            "https://developer.android.com/reference/kotlin/java/util/concurrent/atomic/AtomicInteger",
            "https://www.youtube.com/", "https://github.com/", "https://duckduckgo.com/", "https://twitter.com/",
            "https://www.mecanografia-online.com/ES/Aspx/SelectExerciseWithCharacters.aspx", "https://www.netflix.com/")
        val errorUrls = listOf("invalid line")
        val createdButErrorUrls = ArrayList<String>()
        checkFile(fileContent, requestedUrls, errorUrls, createdButErrorUrls, 24)
    }*/

    @Test
    fun `expiration uses URIs gets deleted async`() {
        val sUrl = shortUrl("http://example.com/", 1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        val target = sUrl.headers.location
        Thread.sleep(10_000)
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
        Thread.sleep(7_000)
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

    private fun getUuid(): String {
        val responseWithUuid = restTemplate.getForEntity("http://localhost:$port/csv", String::class.java)
        val body = responseWithUuid.body.toString()
        val preRegex = ".*<input name=\"uuid\" type=\"hidden\" value=\"".toRegex()
        val postRegex = "\">.*".toRegex()
        val regex = "<input name=\"uuid\".+\">".toRegex()
        return regex.find(body)!!.value.replace(preRegex, "").replace(postRegex, "")
    }

    private fun fileToSend(filename: String): MockMultipartFile {
        val path = Paths.get("src/test/resources/$filename")
        val fileBytes = assertDoesNotThrow("The file could not be read", fun (): ByteArray {
            return Files.readAllBytes(path)
        })
        return MockMultipartFile("file", filename, "text/plain", fileBytes)
    }

    private fun fileBytes(filename: String): ByteArray {
        val path = Paths.get("src/test/resources/$filename")
        return Files.readAllBytes(path)
    }

    private fun checkFile(fileContent: String, requestedUrls: List<String>, errorUrls: List<String>, createdButErrorUrls: List<String>, nReps: Int = 1) {
        //runBlocking {
            for (url in requestedUrls) {
                //launch {
                    val regex = "$url,[^,]*,[^,]*\n".toRegex()
                    val matches = regex.findAll(fileContent)
                    var found = 0
                    for (match in matches) {
                        val lineSplit = match.value.split(",")
                        found++

                        if (found == 1) {
                            if (errorUrls.contains(url)) {
                                assertThat(lineSplit[1]).isEmpty()
                                assertThat(lineSplit[2]).isNotEqualTo("\n")
                            } else {
                                assertThat(lineSplit[1]).isNotEmpty
                                assertThat(lineSplit[2]).isEqualTo("\n")

                                val target = lineSplit[1].replaceFirst("localhost", "localhost:$port")
                                val response = restTemplate.getForEntity(target, String::class.java)
                                if (createdButErrorUrls.contains(url)) {
                                    assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
                                } else {
                                    assertEquals("Should have redirected to $url. Message: ${response.body}",
                                        HttpStatus.TEMPORARY_REDIRECT, response.statusCode)
                                    assertThat(response.headers.location).isEqualTo(URI.create(url))
                                }
                            }
                        } else {
                            assertThat(match.value).isEqualTo(matches.first().value)
                        }
                    }
                    assertNotEquals("$url not present in returned file", 0, found)
                    assertEquals("$url appears an incorrect number of times", nReps, found)
                //}
            }
        }
    //}

}
