package com.davidgrath.expensetrackerbackend.testutils

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

interface TransactionBuilder: Commit {
    fun withItem(description: String): TransactionBuilder
    fun withItem(description: String, category: String, amount: BigDecimal): TransactionBuilder
    fun withDefaultItemPrice(defaultPrice: BigDecimal): TransactionBuilder
    fun withNewAccount(currencyCode: String, accountName: String): TransactionBuilder
    fun debitOrCredit(debitOrCredit: Boolean): TransactionBuilder
    fun withDetailedItem(): TransactionItemBuilder
    fun repeatIntoDates(dates: List<LocalDate>): TransactionBuilder

    /**
     * Inclusive, Inclusive
     */
    fun repeatIntoDateRange(startDate: LocalDate, endDate: LocalDate): TransactionBuilder
    fun atDate(date: LocalDate): TransactionBuilder
    fun atTime(time: LocalTime): TransactionBuilder
    fun useDatesAsCreationDates(b: Boolean): TransactionBuilder
}