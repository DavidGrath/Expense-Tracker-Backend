package com.davidgrath.expensetrackerbackend.utils

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest


private val LOGGER = LoggerFactory.getLogger("com.davidgrath.expensetrackerbackend.Utils")


val CORE_CATEGORIES = setOf(
    "food",
    "rent",
    "utilities",
    "transportation",
    "healthcare",
    "debt",
    "childcare",
    "household",
    "entertainment",
    "self_care",
    "clothing",
    "education",
    "gifts_and_donations",
    "emergency",
    "savings",
    "fitness",
    "miscellaneous",
)

fun getSha256(inputStream: InputStream): String {

        val bufSize = DEFAULT_BUFFER_SIZE
        val bufferedStream = inputStream.buffered()
        val md = MessageDigest.getInstance("SHA256")
        var array = ByteArray(bufSize)
        var len = 0
        var total = 0L //TODO UOM/UCOM
        while (len >= 0) {
            len = bufferedStream.read(array)
            if (len >= 0) {
                total += len
                md.update(array, 0, len)
            }
        }
        val hash = md.digest()
        val hashText = String.format("%064x", BigInteger(1, hash))
        LOGGER.info("getSha256: read {} bytes", total)
        LOGGER.info("getSha256: hash: {}", hashText)
        return hashText

}

fun getSha256(byteArray: ByteArray): String {

    val bufSize = DEFAULT_BUFFER_SIZE

    val md = MessageDigest.getInstance("SHA256")

    var len = byteArray.size

    md.update(byteArray, 0, len)
    val hash = md.digest()
    val hashText = String.format("%064x", BigInteger(1, hash))
    LOGGER.info("getSha256: read {} bytes", len)
    LOGGER.info("getSha256: hash: {}", hashText)
    return hashText

}


fun file(file: File, vararg segments: String): File {
    val sep = File.separator
    val fullPath = segments.joinToString(sep)
    return File(file, fullPath)
}