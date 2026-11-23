package com.davidgrath.expensetrackerbackend.entities.dtos

import java.math.BigDecimal

data class StatisticsTransactionItem(
    val profileId: Long,
    val id: Long,
    val transactionId: Long,
    val amount: BigDecimal,
    val description: String,
    val categoryId: Long,
    val quantity: Int,
    val isReduction: Boolean,
    val isCharge: Boolean,
    val firstImageId: Long?,
)