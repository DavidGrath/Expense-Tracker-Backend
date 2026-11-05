package com.davidgrath.expensetrackerbackend.entities

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Currency

data class StatisticsFilterDb(
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    /**
     * If present, will override [accountIds]
     * TODO Test this
     */
    val currencyCode: Currency? = null,
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
    val sellerLocationIds: List<Long> = emptyList(),
    /**
     * Unlike most of the other filter criteria that work on the transaction level, [categoryIds] works at the line item level
     * I also plan on adding people/groups to line items in the not too distant future, so here's the deal:
     * When any of the filter criteria affect items, then I won't bother subtracting reductions, and I'll need to indicate to the user that I ignored
     * them because of that. In general, reductions are an interesting topic. This is essentially a mini-rant for now before I decide on the
     * proper way to handle things
     */
    val subtractReductions: Boolean = false
)
