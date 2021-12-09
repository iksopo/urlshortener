package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.atomic.AtomicInteger

/**
 * Given a file creates a short URL for each url in the file
 * and creates a new file with the short URL or the error occurred.
 * Returns the key used to create the first URL.
 * When the url is created optional data may be added.
 */
interface CreateShortUrlsFromCsvUseCase {
    fun create(file: MultipartFile, remoteAddr: String, listener: ProgressListener): FileAndShortUrl
}

/**
 * Implementation of [CreateShortUrlsFromCsvUseCase].
 */
class CreateShortUrlsFromCsvUseCaseImpl(
    private var fileStorage: FileStorage,
    private val createShortUrlUseCase: CreateShortUrlUseCase
) : CreateShortUrlsFromCsvUseCase {
    override fun create(file: MultipartFile, remoteAddr: String, listener: ProgressListener): FileAndShortUrl {
        val nameSplitByPoint = file.originalFilename?.split(".")
        if (nameSplitByPoint == null || !nameSplitByPoint[nameSplitByPoint.size-1].equals("csv", ignoreCase = true)) {
            throw InvalidTypeOfFile(file.originalFilename)
        }
        val counter = AtomicInteger()
        listener.onProgress(counter.get())
        val shortUrls = ArrayList<Pair<ShortUrl, String?>>()
        val mutex = Mutex()
        val newName = "${fileStorage.generateName()}.csv"
        fileStorage.store(file, newName)
        val lines = fileStorage.readLines(newName)
        val numUrls = lines.size
        runBlocking {
            for (line in lines) {
                launch {
                    try {
                        createShortUrlUseCase.create(
                            url = line,
                            data = ShortUrlProperties(
                                ip = remoteAddr
                            )
                        ).let {
                            mutex.withLock { shortUrls.add(Pair(it, null)) }
                        }
                    } catch (ex: InvalidUrlException) {
                        mutex.withLock { shortUrls.add(Pair(ShortUrl(hash = "", redirection = Redirection(line),
                                properties = ShortUrlProperties(safe = false, ip = remoteAddr)), ex.message)) }

                    }
                    val progress = counter.incrementAndGet() * 100 / numUrls
                    listener.onProgress(progress)
                }
            }
        }
        listener.onCompletion()
        return FileAndShortUrl(filename = newName, urls = shortUrls)
    }
}