package com.davidgrath.expensetrackerbackend.entities.dtos

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class TransactionWithItemAndAccount(
    val transactionId: Long,
    val transactionAmount: BigDecimal,
    val transactionDebitOrCredit: Byte,
    val transactionDatedAt: LocalDate,
    val transactionTime: LocalTime?,
    val transactionMode: TransactionMode,
    val transactionNote: String?,
    val documentCount: Int,
    val accountId: Long,
    val accountProfileId: Long,
    val accountCurrencyCode: String,
    val accountName: String,
    val accountIsDefault: Boolean,
    val itemId: Long,
    val itemImageCount: Int,
    val itemDescription: String,
    val itemAmount: BigDecimal,
    val itemQuantity: Int,
    val itemBrand: String?,
    val itemVariation: String?,
    val itemIsReduction: Byte,
    val categoryId: Long,
    val categoryName: String?,
    val categoryIsCustom: Byte,
    val categoryStringId: String?,
    val categoryIcon: String,
)
