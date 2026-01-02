package com.davidgrath.expensetrackerbackend.entities.views

import java.math.BigDecimal

data class ItemSumByCategory(
    val categoryId: Long,
    val stringId: String?,
    val isCustom: Boolean,
    val name: String?,
    val sum: BigDecimal,
    val categoryIcon: String
)