package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
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
        listener.onProgress(counter.get()) // Send empty progress bar to web client
        val shortUrls = ArrayList<Pair<ShortUrl, String?>>()
        val mutex = Mutex()
        // Name of temporary file to avoid collisions
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
                                        hash = "", redirection = Redirection(lineSplitByComma[0]), validation = ValidateURISTATUS.YET_TO_VALIDATE,
                                        properties = ShortUrlProperties(safe = false, ip = remoteAddr)
                                    ), "Invalid structure of line: each line must consist of three comma-separated fields even if they are empty"
                                )
                            )
                        }
                    } else {
                        allWrongFormatted.set(false)
                        val url = lineSplitByComma[0]
                        val leftUses = lineSplitByComma[1].toIntOrNull()
                        try {
                            if ((leftUses == null && lineSplitByComma[1] != "")) {
                                throw InvalidLeftUses(lineSplitByComma[1])
                            }

                            val expiration = lineSplitByComma[2].let { date ->
                                if (date == "") {
                                    null
                                } else {
                                    val dateTime = OffsetDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                    val day = dateTime.toLocalDate()
                                    val time = dateTime.toLocalTime()
                                    Date(day.toEpochSecond(time, dateTime.offset) * 1000)
                                }
                            }

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
                        } catch (ex: Exception) {
                            when(ex) {
                                is InvalidUrlException,
                                is InvalidLeftUses ->
                                    mutex.withLock {
                                        shortUrls.add(
                                            Pair(
                                                ShortUrl(
                                                    hash = "", redirection = Redirection(url), validation = ValidateURISTATUS.YET_TO_VALIDATE,
                                                    properties = ShortUrlProperties(safe = false, ip = remoteAddr)
                                                ), ex.message
                                            )
                                        )
                                    }
                                is DateTimeParseException ->
                                    mutex.withLock {
                                        shortUrls.add(
                                            Pair(
                                                ShortUrl(
                                                    hash = "", redirection = Redirection(url), validation = ValidateURISTATUS.YET_TO_VALIDATE,
                                                    properties = ShortUrlProperties(safe = false, ip = remoteAddr)
                                                ), "[${lineSplitByComma[2]}] cannot be parsed to a valid date"
                                            )
                                        )
                                    }
                            }
                        }
                    }
                    val progress = counter.incrementAndGet() * 100 / numUrls
                    listener.onProgress(progress) // Update progress bar in client
                }
            }
        }
        listener.onCompletion()
        // If all the lines in the file had an invalid structure (not three comma-separated fields),
        // delete temporary file instead of updating it, and propagate error
        if (!wellFormatted.get() && allWrongFormatted.get()) {
            fileStorage.deleteFile(newName)
            throw WrongStructuredFile(nameSplitByPoint.joinToString("."))
        }
        return FileAndShortUrl(filename = newName, urls = shortUrls)
    }
}