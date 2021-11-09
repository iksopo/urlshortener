package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.FileStorage
import es.unizar.urlshortener.core.InvalidTypeOfFile
import es.unizar.urlshortener.core.usecases.CreateShortUrlsFromCsvUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
class CsvReceiverController(
    val createShortUrlsFromCsvUseCase: CreateShortUrlsFromCsvUseCase
) {
    @Autowired
    lateinit var fileStorage: FileStorage

    @GetMapping("/csv")
    fun csv(model: MutableMap<String, Any>): String {
        return "uploadform"
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/csv")
    fun uploadMultipartFile(@RequestParam("uploadfile") file: MultipartFile, model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        val h = HttpHeaders()
        createShortUrlsFromCsvUseCase.create(file, request.remoteAddr).let {
            val newLines = ArrayList<String>()
            var isFirst = true
            for (pair in it.urls) {
                val shortUrl = pair.first
                val errorMsg = pair.second
                val target = shortUrl.redirection.target
                val url = linkTo<UrlShortenerControllerImpl> { redirectTo(shortUrl.hash, request) }.toUri()
                if (errorMsg != null) {
                    newLines.add("$target,,$errorMsg")
                    if (isFirst) {
                        model.addAttribute("firstUrlError", "$errorMsg")
                    }
                } else {
                    newLines.add("$target,$url,")
                    if (isFirst) {
                        model.addAttribute("firstUrl", "$url")
                    }
                }

                if (isFirst) {
                    h.location = url
                    response.addHeader("location", "$url")
                    isFirst = false
                }
            }

            fileStorage.overwriteFile(it.filename, newLines)
            model.addAttribute("message", "Your file ${file.originalFilename} has been successfully processed")
            model.addAttribute("file", it.filename)

            return "uploadform"
        }
    }

    @GetMapping("/csv-{filename:.*}")
    fun downloadFile(@PathVariable filename: String): ResponseEntity<Resource> {
        println(filename)
        val file = fileStorage.loadFile(filename)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"")
            .body(file)
    }
}