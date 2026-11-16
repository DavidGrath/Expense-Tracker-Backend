package com.davidgrath.expensetrackerbackend.entities.dtos

import java.math.BigDecimal

data class ItemSumByCategory(
    val categoryId: Long,
    val stringId: String?,
    val isCustom: Boolean,
    val name: String?,
    val sum: BigDecimal,
    val categoryIcon: String
)