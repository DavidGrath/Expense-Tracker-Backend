package com.davidgrath.expensetrackerbackend.entities.views

import java.math.BigDecimal

data class AccountWithStats(
    val id: Long,
    val profileId: Long,
    val currencyCode: String,
    val financialInstitutionId: Long?,
    val referenceNumber: String?,
    val name: String,
    val createdAt: String,
    val expenses: BigDecimal,
    val income: BigDecimal,
    val transactionCount: Long,
    val itemCount: Long //TODO Account for reductions
)
