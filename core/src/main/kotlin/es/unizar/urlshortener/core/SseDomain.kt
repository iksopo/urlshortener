package es.unizar.urlshortener.core

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException


interface ProgressListener {
    fun onProgress(value: Int)
    fun onCompletion()
}

/**
 * Sends progress information to all WebSocket connections in sseEmitters.
 */
class SseEmitterProgressListener(private val sseEmitters: Collection<SseEmitter>) : ProgressListener {
    override fun onProgress(value: Int) {
        val html = """
            <div id="progress-container" class="progress-container">
                <div class="progress-bar" style="width:$value%"></div>
            </div>
            """.trimIndent()
        sendToAllClients(html)
    }

    override fun onCompletion() {
        val html = """
            <p>Your file was processed, download will start in short...</p>
        """.trimIndent()
        sendToAllClients(html)
    }

    private fun sendToAllClients(html: String) {
        for (sseEmitter in sseEmitters) {
            try {
                // multiline strings are sent as multiple "data:" SSE
                // this confuses HTMX in our example so, we remove all newlines
                // so only one "data:" is sent per html
                sseEmitter.send(html.replace("\n", ""))
            } catch (ex: IOException) {
                println(ex.message)
            }
        }
    }
}
