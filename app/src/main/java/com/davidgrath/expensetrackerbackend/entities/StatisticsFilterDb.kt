package com.davidgrath.expensetrackerbackend.entities

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Currency

/**
 * TODO I'll need to add sort criteria eventually
 */
data class StatisticsFilterDb(
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    /**
     * If present, will override [accountIds]
     * TODO Test this across all statistics methods
     */
    val currencyCode: Currency? = null,
    /**
     * It is assumed that at least 1 account ID has been sent from the frontend. Otherwise, nondistinct currency codes could be permitted by this logic.
     * I could potentially throw an exception, but at the repository level it feels wrong
     * TODO Hmmm. Or maybe throwing exceptions isn't so bad. Also, that means [accountIds] would become a compulsory parameter for this data class
     * TODO strengthen the assumption by moving the at least 1 check from the controller layer to the service layer and update this KDoc after
     *
     */
    val accountIds: List<Long> = emptyList(),
    val categoryIds: List<Long> = emptyList(),
    val transactionModes: List<TransactionMode> = emptyList(),
    val weekDays: List<DayOfWeek> = emptyList(),
    /**
     * Any seller present here will cause the corresponding locations in [sellerLocationIds] to be ignored and include all locations
     * TODO Test this across all statistics methods
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
