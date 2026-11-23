package com.davidgrath.expensetrackerbackend.entities.dtos

import java.math.BigDecimal

data class StatisticsTransactionFilterData(
    val profileId: Long,
    val id: Long,
    val excludedItemCount: Int,
    val excludedItemAmount: BigDecimal,
    val excludedChargesAmount: BigDecimal
)