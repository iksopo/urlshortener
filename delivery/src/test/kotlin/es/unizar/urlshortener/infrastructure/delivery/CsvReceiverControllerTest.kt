package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlsFromCsvUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


@WebMvcTest
@ContextConfiguration(classes = [
    CsvReceiverController::class,
    SseController::class,
    RestResponseEntityExceptionHandler::class])
class CsvReceiverControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var fileStorage: FileStorage

    @MockBean
    private lateinit var createShortUrlsFromCsvUseCase: CreateShortUrlsFromCsvUseCase

    @MockBean
    private lateinit var sseRepository: SseRepository

    @BeforeEach
    fun setup() {
        fileStorage.deleteAll()
        fileStorage.init()
    }

    @Test
    fun `create returns a list of redirects when the format of the file is valid`() {
        val uuid = UUID.randomUUID().toString()
        sseRepository.put(uuid, SseEmitter(Long.MAX_VALUE))
        val listener = sseRepository.createProgressListener(uuid)
        val filename = "firstUrlIsValid.csv"
        val path = Paths.get("src/test/resources/$filename")
        val fileBytes = assertDoesNotThrow("The file could not be read", fun (): ByteArray {
            return Files.readAllBytes(path)
        })
        val sentFile = MockMultipartFile("file", filename, "text/plain", fileBytes)
        val localhost = "127.0.0.1"
        val expectedReturn = FileAndShortUrl("tmp0.csv", arrayListOf(
            Pair(ShortUrl("52531b66", Redirection("http://unizar.es"), properties = ShortUrlProperties(localhost, safe = true)), null),
            Pair(ShortUrl("383f1c34", Redirection("https://moodle.unizar.es"), properties = ShortUrlProperties(localhost, safe = true)), null),
            Pair(ShortUrl("", Redirection("invalid line"), properties = ShortUrlProperties(localhost, safe = false)), "[invalid name] does not follow a supported schema"),
            Pair(ShortUrl("16eea967", Redirection("http://www.google.es"), properties = ShortUrlProperties(localhost, safe = true)), null)
        ))
        val address = "127.0.0.1"
        given(createShortUrlsFromCsvUseCase.create(sentFile, address, listener)).willReturn(expectedReturn)
    }

    @Test
    fun `create throws exception when the format of the file is invalid`() {
        val uuid = UUID.randomUUID().toString()
        sseRepository.put(uuid, SseEmitter(Long.MAX_VALUE))
        val listener = sseRepository.createProgressListener(uuid)
        val filename = "notACsv.txt"
        val path = Paths.get("src/test/resources/$filename")
        val fileBytes = assertDoesNotThrow("The file could not be read", fun (): ByteArray {
            return Files.readAllBytes(path)
        })
        val sentFile = MockMultipartFile("file", filename, "text/plain", fileBytes)
        val address = "127.0.0.1"
        given(createShortUrlsFromCsvUseCase.create(sentFile, address, listener)).willAnswer { throw InvalidTypeOfFile(filename) }
    }
}
