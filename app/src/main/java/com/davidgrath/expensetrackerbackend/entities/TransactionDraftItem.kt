package com.davidgrath.expensetrackerbackend.entities

import java.math.BigDecimal
import java.util.UUID

data class TransactionDraftItem(
    val uuid: UUID,
    val dbId: Long?,
    val category: Long,
    val amount: BigDecimal? = null,
    val description: String? = null,
    val brand: String? = null,
    val variation: String? = null,
    val referenceNumber: String? = null,
    val quantity: Int = 1,
    val isReduction: Boolean = false,
    val ordinal: Int = 0,
    val images: List<TransactionDraftFile> = emptyList(),
    val deletedDbImages: List<TransactionDraftFile> = emptyList()
)
