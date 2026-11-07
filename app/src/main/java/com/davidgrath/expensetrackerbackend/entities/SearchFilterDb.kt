package com.davidgrath.expensetrackerbackend.entities

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Currency

/**
 * TODO Contemplate using deny and allowlists where deny takes precedence over allow. May not actually do it
 * TODO I'll need to add sort criteria eventually
 */
data class SearchFilterDb(
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    /**
     * Any currency code present here will cause the corresponding accounts in [accountIds] to be ignored and include all accounts for that currency code
     */
    val currencyCodes: List<Currency> = emptyList(),
    val accountIds: List<Long> = emptyList(),
    val categoryIds: List<Long> = emptyList(),
    val transactionModes: List<TransactionMode> = emptyList(),
    val weekDays: List<DayOfWeek> = emptyList(),
    /**
     * Any seller present here will cause the corresponding locations in [sellerLocationIds] to be ignored and include all locations
     */
    val sellerIds: List<Long> = emptyList(),
    val sellerLocationIds: List<Long> = emptyList()
)
