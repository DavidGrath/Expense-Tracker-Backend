package com.davidgrath.expensetrackerbackend.entities.views

import com.davidgrath.expensetrackerbackend.entities.DocumentType
import java.time.LocalDate

data class DocumentWithTransactionDateAndOrdinal(
    val id: Long,
    val transactionId: Long,
    val type: DocumentType,
    val sizeBytes: Long,
    val sha256: String,
    val mimeType: String,
    val path: String,
    val transactionDatedAt: LocalDate,
    val transactionOrdinal: Int
)
