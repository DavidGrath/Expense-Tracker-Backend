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
     * TODO Test this
     */
    val currencyCodes: List<Currency> = emptyList(),
    val accountIds: List<Long> = emptyList(),
    val categoryIds: List<Long> = emptyList(),
    val transactionModes: List<TransactionMode> = emptyList(),
    /**
     * Any dates that fall outside the date range of [fromDate]-[toDate], if they are specified, will be ignored.
     * TODO test this.
     * TODO Actually. Get rid of this. It's meant to be a weekday filter and there are better ways to handle it
     */
    val includeDates: List<LocalDate> = emptyList(),
    val weekDays: List<DayOfWeek> = emptyList(),
    /**
     * Any seller present here will cause the corresponding locations in [sellerLocationIds] to be ignored and include all locations
     * TODO Test this
     */
    val sellerIds: List<Long> = emptyList(),
    val sellerLocationIds: List<Long> = emptyList()
)
