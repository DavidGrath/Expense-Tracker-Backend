package com.davidgrath.expensetrackerbackend.entities.views

import jakarta.persistence.Column
import java.math.BigDecimal

data class AccountWithStats(
    val id: Long,
    val profileId: Long,
    val currencyCode: String,
    val financialInstitutionId: Long?,
    val referenceNumber: String,
    val name: String,
    val createdAt: String,
    val expenses: BigDecimal,
    val income: BigDecimal,
    val transactionCount: Int,
    val itemCount: Int //TODO Account for reductions
)
