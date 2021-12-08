package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import es.unizar.urlshortener.core.SseEmitterProgressListener
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
        return SseEmitterProgressListener(sseEmitters[id], "")
    }
}

@Controller
class SseController(
    private val repository: SseRepository
) {
    @GetMapping("/progress")
    fun progressEvents(@RequestParam("uuid") id: String): SseEmitter {
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