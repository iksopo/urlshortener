package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import es.unizar.urlshortener.core.SseEmitterProgressListener
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

@Component
class SseRepository {
    val sseEmitters: Multimap<String, SseEmitter> = MultimapBuilder.hashKeys().arrayListValues().build()
    fun put(id: String, sseEmitter: SseEmitter) {
        sseEmitters.put(id, sseEmitter)
    }

    fun createProgressListener(id: String): SseEmitterProgressListener {
        return SseEmitterProgressListener(sseEmitters[id])
    }
}

@Api(value = "example", description = "WebSocket API", tags = ["API to open a WebSocket to receive file processing progress"])
@Controller
class SseController(
    private val repository: SseRepository
) {
    @ApiOperation(value = "Opens an maintains a WebSocket connection that will send HTML code to display the progress of a POST /csv request.")
    @GetMapping("/progress")
    fun progressEvents(@ApiParam(value = "User's id", example = "f97e7508-490b-49bc-a2a0-721d2a63324f",
        type = "String", required = true) @RequestParam("uuid") id: String): SseEmitter {
        val sseEmitter = SseEmitter(Long.MAX_VALUE)
        repository.put(id, sseEmitter)
        println("Adding SseEmitter for user: $id")
        with(sseEmitter) {
            onCompletion { println("SseEmitter for user $id is completed") }
            onTimeout { println("SseEmitter for user $id is timed out") }
            onError { ex -> println("SseEmitter for user $id got error: $ex") }
        }
        return sseEmitter
    }
}