package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.FileStorage
import es.unizar.urlshortener.core.usecases.CreateShortUrlsFromCsvUseCase
import io.swagger.annotations.*
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Api(value = "example", description = "Massive shortener API", tags = ["API to shorten URIs in a CSV file"])
@Controller
class CsvReceiverController(
    private val sseRepository: SseRepository,
    val createShortUrlsFromCsvUseCase: CreateShortUrlsFromCsvUseCase
) {
    @Autowired
    lateinit var fileStorage: FileStorage

    @ApiOperation(value = "Shows a page to upload the CSV file and gives each user a unique id")
    @GetMapping("/csv")
    fun csv(model: MutableMap<String, Any>): String {
        model["uuid"] = UUID.randomUUID().toString()
        return "uploadform"
    }

    @ApiOperation(value = "Shortens URIs in a given CSV file and returns them in a new CSV file")
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "OK"),
            ApiResponse(code = 400, message = "The file couldn't be read or had invalid format")]
    )
    @PostMapping("/csv", consumes = [ MediaType.MULTIPART_FORM_DATA_VALUE ], produces = [ "text/csv" ])
    fun uploadCsv(@ApiParam(value = "File with the URIs and their expiration data.", example = "https://google.es,,",
        type = "multipart/form-data", required = true ) @RequestParam("file") file: MultipartFile,
        @ApiParam(value = "Id assigned to the user with GET /csv", example = "f97e7508-490b-49bc-a2a0-721d2a63324f",
        type = "String", required = true) @RequestParam("uuid") uuid: String, request: HttpServletRequest,
        response: HttpServletResponse) {
        val listener = sseRepository.createProgressListener(uuid)
        response.status = HttpStatus.CREATED.value()
        createShortUrlsFromCsvUseCase.create(file, request.remoteAddr, listener).let {
            val newLines = ArrayList<String>()
            for (i in 0 until it.urls.size) {
                val pair = it.urls[i]

                val shortUrl = pair.first
                val errorMsg = pair.second
                val target = shortUrl.redirection.target
                val url = linkTo<UrlShortenerControllerImpl> { redirectTo(shortUrl.hash, request) }.toUri()
                if (errorMsg != null) {
                    newLines.add("$target,,$errorMsg")
                } else {
                    newLines.add("$target,$url,")
                }
                if (i == 0) {
                    response.addHeader("Location", "$url")
                }
            }

            fileStorage.overwriteFile(it.filename, newLines)
            val generatedFile = fileStorage.loadFile(it.filename)
            response.contentType = "text/csv"
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shortUrls.csv\"")
            IOUtils.copy(generatedFile.inputStream, response.outputStream)
            response.outputStream.close()
            generatedFile.inputStream.close()
            fileStorage.deleteFile(it.filename)
        }
    }
}