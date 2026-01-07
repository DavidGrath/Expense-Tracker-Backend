package com.davidgrath.expensetrackerbackend.entities

import java.net.URI
import java.time.LocalDate
import java.time.LocalTime

data class TransactionDraft(
    val items: List<TransactionDraftItem>,
    val accountId: Long,
    val debitOrCredit: Boolean = true,
    val mode: TransactionMode = TransactionMode.Other,
    /**
     * To prevent the user from adding duplicate images
     */
    val imageHashes: Map<String, String> = emptyMap(),
    val documents: List<TransactionDraftFile> = emptyList(),
    val documentHashes: Map<String, String> = emptyMap(),
    val note: String? = null,
    val deletedDbItems: List<TransactionDraftItem> = emptyList(),
    val deletedDbDocuments: List<TransactionDraftFile> = emptyList(),
    /**
     * In edit mode, this means nothing
     */
    val dbOriginalDate: LocalDate? = null,
    val dbOriginalTime: LocalTime? = null,
    val customDate: LocalDate? = null,
    val customTime: LocalTime? = null,
    val sellerId: Long? = null,
    val sellerLocationId: Long? = null,
)
