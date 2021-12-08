package es.unizar.urlshortener.core

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException


interface ProgressListener {
    fun onProgress(value: Int)
    fun onCompletion()
    fun setFilename(newFilename: String)
}

class SseEmitterProgressListener(private val sseEmitters: Collection<SseEmitter>, private var filename: String) : ProgressListener {
    override fun onProgress(value: Int) {
        println("progress: $value")
        val html = """
            <div id="progress-container" class="progress-container">
                <div class="progress-bar" style="width:$value%"></div>
            </div>
            """.trimIndent()
        sendToAllClients(html)
    }

    override fun onCompletion() {
       val html = """
            <form method="post" action="/csv-$filename" style="display:inline">
              <button type="submit">
                Download new file
              </button>
            </form>
        """.trimIndent()
        sendToAllClients(html)
    }

    override fun setFilename(newFilename: String) {
        filename = newFilename
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
