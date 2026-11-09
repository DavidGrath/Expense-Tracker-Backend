package com.davidgrath.expensetrackerbackend.entities

data class TransactionDraftFile(
    val dbId: Long?,
    val dbOrdinal: Int?,
    val path: String,
    val mimeType: String,
    val sha256: String,
    val sizeBytes: Long,
    val type: DocumentType?,
    /**
     * Should always be false if the item is new
     */
    val dbIsLinked: Boolean = false
)
