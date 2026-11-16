package com.davidgrath.expensetrackerbackend.entities.dtos

import java.math.BigDecimal

data class TopItemSumByCategory(
    val itemId: Long,
    val itemDescription: String,
    val itemAmount: BigDecimal,
    val denseRank: Int,
    val categoryId: Long,
    val stringId: String?,
    val isCustom: Boolean,
    val name: String?,
    val sum: BigDecimal,
    val categoryIcon: String
)