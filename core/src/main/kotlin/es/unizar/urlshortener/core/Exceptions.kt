package es.unizar.urlshortener.core

class InvalidUrlException(val url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(val key: String) : Exception("[$key] is not known")

class InvalidTypeOfFile(val filename: String?) : Exception("The format of [$filename] is not supported, only .csv files are")

class FileDoesNotExist(val filename: String) : Exception(
    "The file [$filename] doesn't exist. If you couldn't download the new CSV, try regenerating it")