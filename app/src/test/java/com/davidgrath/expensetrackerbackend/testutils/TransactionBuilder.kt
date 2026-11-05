package com.davidgrath.expensetrackerbackend.testutils

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

interface TransactionBuilder: Commit {
    fun withItem(description: String): TransactionBuilder
    fun withItem(description: String, category: String, amount: BigDecimal): TransactionBuilder
    fun withDefaultItemPrice(defaultPrice: BigDecimal): TransactionBuilder
    fun withNewAccount(currencyCode: String, accountName: String): TransactionBuilder
    fun withMode(mode: TransactionMode): TransactionBuilder
    fun getAccount(): AccountPojo
    fun withAccountId(accountId: Long): TransactionBuilder
    fun debitOrCredit(debitOrCredit: Boolean): TransactionBuilder
    fun withDetailedItem(): TransactionItemBuilder
    fun withNewSeller(name: String): TransactionBuilder
    fun withSeller(sellerId: Long): TransactionBuilder
    fun withNewSellerLocation(name: String): TransactionBuilder
    fun getSeller(): SellerPojo?
    fun repeatIntoDates(dates: List<LocalDate>): TransactionBuilder

    /**
     * Inclusive, Inclusive
     */
    fun repeatIntoDateRange(startDate: LocalDate, endDate: LocalDate): TransactionBuilder
    fun atDate(date: LocalDate): TransactionBuilder
    fun atTime(time: LocalTime): TransactionBuilder
    fun useDatesAsCreationDates(b: Boolean): TransactionBuilder
    fun withDocument(resource: TestData.Resource): TransactionBuilder
}