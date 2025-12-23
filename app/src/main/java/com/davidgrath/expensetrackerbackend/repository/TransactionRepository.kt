package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.DateAmountSummary
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.jooq.DSLContext
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

    //region Create

    fun insertTransaction(transaction: TransactionPojo): Long {
        val transactionDb = dslContext.newRecord(TRANSACTION, transaction)
        transactionDb.store()
        return transactionDb.id!!
    }

    //endregion

    //region Read

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

    fun getByUserIdAndId(userId: Long, id: Long): TransactionPojo? {
        val selectStep = dslContext.select(TRANSACTION.asterisk())
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .innerJoin(PROFILE).on(PROFILE.ID.eq(ACCOUNT.PROFILE_ID))
            .where(PROFILE.USER_ID.eq(userId))
            .and(TRANSACTION.ID.eq(id))

        return selectStep.fetchOne()?.into(TransactionPojo::class.java)
    }

    fun getTransactions(
        profileId: Long,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<TransactionPojo> {

        var selectStep = dslContext.select(TRANSACTION.asterisk())
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
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
        val seekStep = selectStep.orderBy(TRANSACTION.DATED_AT)
        return seekStep.fetch().into(TransactionPojo::class.java)
    }


    fun getTransactionSum(
        profileId: Long,
        debitOrCredit: Boolean,
        fromDate: LocalDate?, toDate: LocalDate?,
        accountIds: List<Long>,
        dates: List<LocalDate>,
        categories: List<Long>,
        modes: List<TransactionMode>,
        sellers: List<Long>
    ): BigDecimal {
        var selectStep = dslContext.select(DSL.sum(TRANSACTION.AMOUNT)).from(TRANSACTION)
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

        return selectStep.fetchOne()?.value1()?: BigDecimal.ZERO
    }

    //endregion

    //region Delete

    fun deleteAllForTesting(): Int { //TODO Try and make these delete methods only be available in the test folder
        val delete = dslContext.deleteFrom(TRANSACTION).where("true")
        return delete.execute()
    }

    //endregion


    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionRepository::class.java)
    }
}