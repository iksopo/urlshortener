package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.FileStorage
import es.unizar.urlshortener.core.usecases.CreateShortUrlsFromCsvUseCase
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.collections.ArrayList

@Controller
class CsvReceiverController(
    private val sseRepository: SseRepository,
    val createShortUrlsFromCsvUseCase: CreateShortUrlsFromCsvUseCase
) {
    @Autowired
    lateinit var fileStorage: FileStorage

    @GetMapping("/csv")
    fun csv(model: MutableMap<String, Any>): String {
        model["uuid"] = UUID.randomUUID().toString()
        return "uploadform"
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/csv2", consumes = [ MediaType.MULTIPART_FORM_DATA_VALUE ])
    fun upload2(@RequestParam("file") file: MultipartFile, @RequestParam("uuid") uuid:String,
                model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        println("csv2")
        println(uuid)
        val listener = sseRepository.createProgressListener(uuid)
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
                    if (i == 0) {
                        model.addAttribute("firstUrlError", "$errorMsg")
                    }
                } else {
                    newLines.add("$target,$url,")
                    if (i == 0) {
                        model.addAttribute("firstUrl", "$url")
                    }
                }
                if (i == 0) {
                    response.addHeader("Location", "$url")
                }
            }

            fileStorage.overwriteFile(it.filename, newLines)
            model.addAttribute("message", "Your file ${file.originalFilename} has been successfully processed")
            model.addAttribute("file", it.filename)
        }
        model.addAttribute("uuid", uuid)
        return "uploadform"
    }
/*
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/csv", consumes = [ MediaType.MULTIPART_FORM_DATA_VALUE ])
    fun uploadCsv(@RequestParam("file") file: MultipartFile, model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
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
                    response.addHeader("Location", "$url")
                    isFirst = false
                }
            }

            fileStorage.overwriteFile(it.filename, newLines)
            model.addAttribute("message", "Your file ${file.originalFilename} has been successfully processed")
            model.addAttribute("file", it.filename)

            return "uploadform"
        }
    }*/

    @PostMapping("/csv-{filename:.*}")
    fun downloadFile(@PathVariable filename: String, response: HttpServletResponse)/*: ResponseEntity<Resource>*/ {
        val file = fileStorage.loadFile(filename)
        response.contentType = "text/csv"
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shortUrls.csv\"")
        IOUtils.copy(file.inputStream, response.outputStream)
        response.outputStream.close()
        file.inputStream.close()
        fileStorage.deleteFile(filename)
    }

    @PostMapping("/csvshort")
    fun downloadFile2(@RequestParam("name") filename: String, response: HttpServletResponse)/*: ResponseEntity<Resource>*/ {
        println(filename)
        val file = fileStorage.loadFile(filename)
        response.contentType = "text/csv"
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shortUrls.csv\"")
        IOUtils.copy(file.inputStream, response.outputStream)
        response.outputStream.close()
        file.inputStream.close()
        fileStorage.deleteFile(filename)
    }
}