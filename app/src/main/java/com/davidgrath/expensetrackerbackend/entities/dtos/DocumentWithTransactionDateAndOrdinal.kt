package com.davidgrath.expensetrackerbackend.entities.dtos

import com.davidgrath.expensetrackerbackend.entities.DocumentType
import java.time.LocalDate

data class DocumentWithTransactionDateAndOrdinal(
    val id: Long,
    val transactionId: Long,
    val type: DocumentType,
    val sizeBytes: Long,
    val sha256: ByteArray,
    val mimeType: String,
    val path: String,
    val transactionDatedAt: LocalDate,
    val transactionOrdinal: Int //TODO Why did I need this?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentWithTransactionDateAndOrdinal

        if (id != other.id) return false
        if (transactionId != other.transactionId) return false
        if (sizeBytes != other.sizeBytes) return false
        if (transactionOrdinal != other.transactionOrdinal) return false
        if (type != other.type) return false
        if (!sha256.contentEquals(other.sha256)) return false
        if (mimeType != other.mimeType) return false
        if (path != other.path) return false
        if (transactionDatedAt != other.transactionDatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + transactionId.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + transactionOrdinal
        result = 31 * result + type.hashCode()
        result = 31 * result + sha256.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + transactionDatedAt.hashCode()
        return result
    }
}
