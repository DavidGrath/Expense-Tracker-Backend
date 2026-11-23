package com.davidgrath.expensetrackerbackend.entities.service

import com.davidgrath.expensetrackerbackend.entities.dtos.StatisticsTransactionItem
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class StatisticsTransaction(
    val profileId: Long,
    val id: Long,
    val currencyCode: String,
    val total: BigDecimal,
    val date: LocalDate,
    val time: LocalTime?,
    val debitOrCredit: Boolean,
    val accountId: Long,
    val excludedItemCount: Int,
    val excludedItemAmount: BigDecimal,
    val excludedChargesAmount: BigDecimal,
    val items: List<StatisticsTransactionItem>
)

