package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

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
        val lines = fileStorage.readLines(newName).filter { l -> l != "" }
        val numUrls = lines.size
        val wellFormatted = AtomicBoolean(true)
        val allWrongFormatted = AtomicBoolean(numUrls != 0)
            // If there are no urls to shorten, the format is always valid
            // Otherwise it is invalid until a valid line is found
        runBlocking {
            for (line in lines) {
                launch {
                    val lineSplitByComma = line.split(",")
                    if (lineSplitByComma.size != 3) {
                        wellFormatted.set(false)
                        mutex.withLock {
                            shortUrls.add(
                                Pair(
                                    ShortUrl(
                                        hash = "", redirection = Redirection(lineSplitByComma[0]),
                                        properties = ShortUrlProperties(safe = false, ip = remoteAddr)
                                    ), "Invalid structure of line, each line must consist of three comma-separated fields, even if they are empty"
                                )
                            )
                        }
                    } else {
                        allWrongFormatted.set(false)
                        val url = lineSplitByComma[0]
                        val leftUses = lineSplitByComma[1].toIntOrNull() // TODO: throw if it's not ""
                        try {
                            val expiration = lineSplitByComma[2].let { date ->
                                if (date == "") {
                                    null
                                } else {
                                    java.sql.Date.valueOf(
                                        LocalDate.parse(
                                            lineSplitByComma[2],
                                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                        )
                                    )
                                }
                            }
                            println(expiration)
                            //LocalDate.parse(lineSplitByComma[2], DateTimeFormatter.ISO_OFFSET_DATE_TIME) //OffsetDateTime.parse(lineSplitByComma[2])
                            createShortUrlUseCase.create(
                                url = url,
                                data = ShortUrlProperties(
                                    ip = remoteAddr,
                                    leftUses = leftUses,
                                    expiration = expiration
                                )
                            ).let {
                                mutex.withLock { shortUrls.add(Pair(it, null)) }
                            }
                        } catch (ex: InvalidUrlException) {
                            mutex.withLock {
                                shortUrls.add(
                                    Pair(
                                        ShortUrl(
                                            hash = "", redirection = Redirection(url),
                                            properties = ShortUrlProperties(safe = false, ip = remoteAddr)
                                        ), ex.message
                                    )
                                )
                            }
                        }
                    }
                    val progress = counter.incrementAndGet() * 100 / numUrls
                    listener.onProgress(progress)
                }
            }
        }
        listener.onCompletion()
        if (!wellFormatted.get() && allWrongFormatted.get()) {
            println(nameSplitByPoint.joinToString("."))
            fileStorage.deleteFile(newName)
            throw WrongStructuredFile(nameSplitByPoint.joinToString("."))
        }
        return FileAndShortUrl(filename = newName, urls = shortUrls)
    }
}