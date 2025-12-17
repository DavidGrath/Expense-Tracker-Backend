package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.DateAmountSummary
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.toByte
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
open class TransactionRepository {

    @Autowired
    open lateinit var dslContext: DSLContext
    open var TRANSACTION: Transaction = Transaction.TRANSACTION
    open var ACCOUNT: Account = Account.ACCOUNT
    open var TRANSACTION_ITEM: TransactionItem = TransactionItem.TRANSACTION_ITEM

    fun insertTransaction(transaction: TransactionPojo): Long {
        val transactionDb = dslContext.newRecord(TRANSACTION, transaction)
        transactionDb.store()
        return transactionDb.id!!
    }

    fun getTransactionSumByDate(
        profileId: Long, debitOrCredit: Boolean,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<DateAmountSummary> {
        val aggregateDate = TRANSACTION.DATED_AT.`as`("aggregateDate")
        var selectStep = dslContext.select(aggregateDate, DSL.sum(TRANSACTION.AMOUNT).`as`("sum")).from(TRANSACTION)
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
        if(fromDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.greaterOrEqual(fromDate))
        }
        if(toDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.lessOrEqual(toDate))
        }
        if(accountIds.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.ACCOUNT_ID.`in`(accountIds))
        }
        if(dates.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.`in`(dates))
        }
        if(categories.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.ID.`in`(
                dslContext.select(TRANSACTION.ID).from(TRANSACTION).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.TRANSACTION_ID.eq(TRANSACTION.ID)).where(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))
            ))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }
        val seekStep = selectStep.groupBy(aggregateDate).orderBy(aggregateDate)
        return seekStep.fetch().into(DateAmountSummary::class.java)
    }


    fun getMaxOrdinalInDayForAccount(accountId: Long, date: LocalDate): Int? {
        val selectStep = dslContext.select(DSL.max(TRANSACTION.ORDINAL)).from(TRANSACTION)
            .where(TRANSACTION.ACCOUNT_ID.eq(accountId))
            .and(TRANSACTION.DATED_AT.eq(date))

        return selectStep.fetchOne()?.value1()
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(TRANSACTION).where("true")
        return delete.execute()
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionRepository::class.java)
    }
}