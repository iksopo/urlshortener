package es.unizar.urlshortener.core

class InvalidUrlException(val url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(val key: String) : Exception("[$key] is not known")

class InvalidTypeOfFile(filename: String?) : Exception("The format of [$filename] is not supported, only .csv files are")

class WrongStructuredFile(filename: String) : Exception("The structure of [$filename] is invalid: each line must consist of three comma-separated fields, even if they are empty")

class InvalidLeftUses(val leftUses: String) : Exception("[$leftUses] is not a valid number of uses: it must be a number greater than 0")

class FileDoesNotExist(filename: String) : Exception(
    "The file [$filename] doesn't exist. If you couldn't download the new CSV, try regenerating it")

class InvalidDateException(val date: String) : Exception("[$date] is not a valid date. They must follow [ISO_OFFSET_DATE_TIME](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_OFFSET_DATE_TIME)")