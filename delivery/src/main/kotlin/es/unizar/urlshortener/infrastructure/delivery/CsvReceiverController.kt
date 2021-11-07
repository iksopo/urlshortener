package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.FileStorage
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest

@Controller
class CsvReceiverController(
    val createShortUrlUseCase: CreateShortUrlUseCase
) {
    @Autowired
    lateinit var fileStorage: FileStorage

    @GetMapping("/csv")
    fun csv(model: MutableMap<String, Any>): String {
        return "uploadform"
    }

    @PostMapping("/csv")
    fun uploadMultipartFile(@RequestParam("uploadfile") file: MultipartFile, model: Model, request: HttpServletRequest): String {
        var valid = (file.originalFilename != null && file.originalFilename!!.isNotEmpty())
        var previousNameSplit: List<String>
        var extension = ""
        if (valid) {
            previousNameSplit = file.originalFilename!!.split(".")
            extension = previousNameSplit[previousNameSplit.size - 1]
        }
        valid = (valid && extension == "csv")
        if (valid) {
            val newName = "${fileStorage.generateName()}.$extension"
            fileStorage.store(file, newName)
            val lines = fileStorage.readLines(newName)
            var newLines = ArrayList<String>()
            for (line in lines) {
                createShortUrlUseCase.create(
                    url = line,
                    data = ShortUrlProperties(
                        ip = request.remoteAddr,
                        sponsor = null
                    )
                ).let {
                    //val h = HttpHeaders()
                    val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
                    //h.location = url
                    val response = ShortUrlDataOut(
                        url = url,
                        properties = mapOf(
                            "safe" to it.properties.safe
                        )
                    )
                    newLines.add("$line,$url,")
                    //ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
                }
            }
            fileStorage.overwriteFile(newName, newLines)
            model.addAttribute("message", "File uploaded successfully! -> filename = " + file.originalFilename)
            model.addAttribute("file", newName)
        } else {
            model.addAttribute("message", "The name or type of file is invalid")
        }
        return "uploadform"
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