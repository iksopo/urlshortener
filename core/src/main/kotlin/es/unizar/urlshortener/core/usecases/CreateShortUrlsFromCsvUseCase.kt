package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.web.multipart.MultipartFile

/**
 * Given a file creates a short URL for each url in the file
 * and creates a new file with the short URL or the error occurred.
 * Returns the key used to create the first URL.
 * When the url is created optional data may be added.
 */
interface CreateShortUrlsFromCsvUseCase {
    fun create(file: MultipartFile, remoteAddr: String): FileAndShortUrl
}

/**
 * Implementation of [CreateShortUrlsFromCsvUseCase].
 */
class CreateShortUrlsFromCsvUseCaseImpl(
    private var fileStorage: FileStorage,
    private val createShortUrlUseCase: CreateShortUrlUseCase
) : CreateShortUrlsFromCsvUseCase {
    override fun create(file: MultipartFile, remoteAddr: String): FileAndShortUrl {
        val nameSplitByPoint = file.originalFilename?.split(".")
        if (nameSplitByPoint == null || !nameSplitByPoint[nameSplitByPoint.size-1].equals("csv", ignoreCase = true)) {
            throw InvalidTypeOfFile(file.originalFilename)
        }
        val shortUrls = ArrayList<Pair<ShortUrl, String?>>()
        val newName = "${fileStorage.generateName()}.csv"
        fileStorage.store(file, newName)
        val lines = fileStorage.readLines(newName)
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
                            shortUrls.add(Pair(it, null))
                        }
                    } catch (ex: InvalidUrlException) {
                        shortUrls.add(Pair(
                            ShortUrl(hash = "", redirection = Redirection(line),
                                properties = ShortUrlProperties(safe = false, ip = remoteAddr)), ex.message))
                    }
                }
            }
        }
        return FileAndShortUrl(filename = newName, urls = shortUrls)
    }
}
