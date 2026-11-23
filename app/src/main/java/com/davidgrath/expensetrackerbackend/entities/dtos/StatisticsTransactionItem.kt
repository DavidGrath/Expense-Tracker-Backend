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
    val brand: String?,
    val variation: String?,
    val weight: BigDecimal?,
    val weightPrefix: String?,
    val weightUnit: String?,
    val volume: BigDecimal?,
    val volumePrefix: String?,
    val volumeUnit: String?,
    val isReduction: Boolean,
    val isCharge: Boolean,
    val firstImageId: Long?,
)