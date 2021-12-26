package es.unizar.urlshortener.core

import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.util.FileSystemUtils
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.copyTo
import kotlin.io.path.readLines

interface FileStorage {
    fun store(file: MultipartFile, filename: String)
    fun loadFile(filename: String): Resource
    fun deleteFile(filename: String)
    fun deleteAll()
    fun init()
    fun generateName(): String
    fun readLines(filename: String): List<String>
    fun overwriteFile(filename: String, lines: List<String>)
}

@Service
class FileStorageImpl: FileStorage {
    private var rootLocation = Paths.get("filestorage")

    private var numFiles = AtomicInteger(0)

    override fun store(file: MultipartFile, filename: String) {
        Files.copy(file.inputStream, this.rootLocation.resolve(filename))
    }

    override fun loadFile(filename: String): Resource{
        val file = rootLocation.resolve(filename)
        val resource = UrlResource(file.toUri())

        if (resource.exists() || resource.isReadable) {
            return resource
        } else {
            throw FileDoesNotExist(filename)
        }
    }

    override fun deleteFile(filename: String) {
        Files.deleteIfExists(rootLocation.resolve(filename))
    }

    override fun deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile())
    }

    override fun init() {
        Files.createDirectory(rootLocation)
    }

    override fun generateName(): String {
        return "tmp${numFiles.incrementAndGet()}"
    }

    override fun readLines(filename: String): List<String> = rootLocation.resolve(filename).readLines()

    override fun overwriteFile(filename: String, lines: List<String>) {
        PrintWriter(rootLocation.resolve(filename).toString()).use {
            for (line in lines) {
                it.println(line)
            }
        }
    }
}