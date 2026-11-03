package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.DateAmountSummary
import com.davidgrath.expensetrackerbackend.entities.views.StringFieldWithTimestamp
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
open class TransactionItemRepository {

    @Autowired
    open lateinit var mainContext: DSLContext
    open var TRANSACTION_ITEM: TransactionItem = TransactionItem.TRANSACTION_ITEM

    fun insertTransactionItem(item: TransactionItemPojo): Long {
        val itemDb = mainContext.newRecord(TRANSACTION_ITEM, item)
        itemDb.store()
        return itemDb.id!!
    }

    fun insertTransactionItemTrx(trxContext: DSLContext, item: TransactionItemPojo): Long {
        val itemDb = trxContext.newRecord(TRANSACTION_ITEM, item)
        itemDb.store()
        return itemDb.id!!
    }

    fun getAllByTransactionId(profileId: Long, transactionId: Long): List<TransactionItemPojo> {
        val selectStep = mainContext.select(TRANSACTION_ITEM.asterisk())
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(TRANSACTION_ITEM.TRANSACTION_ID.eq(transactionId))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep
                .fetch().into(TransactionItemPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun getDescriptionsAndDates(profileId: Long, prefixString: String): List<StringFieldWithTimestamp> {
        val selectStep = mainContext
            .select(
                TRANSACTION_ITEM.DESCRIPTION.`as`("stringField"),
                TRANSACTION_ITEM.CREATED_AT
            )
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(TRANSACTION_ITEM.DESCRIPTION.like("$prefixString%"))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(StringFieldWithTimestamp::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getBrandsAndDates(profileId: Long, prefixString: String): List<StringFieldWithTimestamp> {
        val selectStep = mainContext
            .select(
                TRANSACTION_ITEM.BRAND.`as`("stringField"),
                TRANSACTION_ITEM.CREATED_AT
            )
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(TRANSACTION_ITEM.BRAND.like("$prefixString%"))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)

        val result = selectStep.fetch().into(StringFieldWithTimestamp::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getVariationsAndDates(profileId: Long, prefixString: String): List<StringFieldWithTimestamp> {
        val selectStep = mainContext
            .select(
                TRANSACTION_ITEM.VARIATION.`as`("stringField"),
                TRANSACTION_ITEM.CREATED_AT
            )
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(TRANSACTION_ITEM.VARIATION.like("$prefixString%"))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)

        val result = selectStep.fetch().into(StringFieldWithTimestamp::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun getTransactionItemSumByDateRecursive(
        profileId: Long, debitOrCredit: Boolean,
        fromDate: LocalDate, toDate: LocalDate,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<DateAmountSummary> {
        val dateSequence = DSL.name("date_sequence").fields("d")
            .`as`(
                DSL
                    .select(DSL.localDate(fromDate))
                    .unionAll(
                        DSL.select(DSL.localDateAdd(DSL.field("d", SQLDataType.LOCALDATE), 1, DatePart.DAY) )
                            .from(DSL.table(DSL.name("date_sequence")))
                            .where(DSL.localDateAdd(DSL.field("d", SQLDataType.LOCALDATE), 1, DatePart.DAY).lessOrEqual(toDate))
                    )
            )

        val aggregateDate = TRANSACTION.DATED_AT.`as`("aggregateDate")
        var selectStep = mainContext.select(aggregateDate, DSL.sum(TRANSACTION_ITEM.AMOUNT).`as`("sum"))
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
            .and(TRANSACTION.DATED_AT.greaterOrEqual(fromDate))
            .and(TRANSACTION.DATED_AT.lessOrEqual(toDate))

        if(accountIds.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.ACCOUNT_ID.`in`(accountIds))
        }
        if(dates.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.`in`(dates))
        }
        if(categories.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }
        val mainQuerySeekStep = selectStep.groupBy(aggregateDate).orderBy(aggregateDate)
        val mainQuery = DSL.name("main_query")
            .`as`(
                mainQuerySeekStep
            )
        val recursive = mainContext.withRecursive(dateSequence)
            .with(mainQuery)
            .select(dateSequence.field("d")!!.`as`("aggregateDate"), DSL.ifnull(mainQuery.field("sum"), BigDecimal.ZERO))
            .from(dateSequence)
            .leftJoin(mainQuery).on(mainQuery.field("aggregateDate", SQLDataType.LOCALDATE)!!.eq(dateSequence.field("d", SQLDataType.LOCALDATE)))


        val seekStep = recursive.orderBy(aggregateDate)
            .limit(DEFAULT_FETCH_LIMIT)
        val result = seekStep.fetch().into(DateAmountSummary::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getEarliestItemTransactionDate(
        profileId: Long, debitOrCredit: Boolean,
        accountIds: List<Long>,
        dates: List<LocalDate>,
        categories: List<Long>,
        modes: List<TransactionMode>,
        sellers: List<Long>,
        currentDate: LocalDate): LocalDate? {
        var selectStep = mainContext.select(DSL.min(TRANSACTION.DATED_AT))
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
            .and(TRANSACTION.DATED_AT.lessOrEqual(currentDate))

        if(accountIds.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.ACCOUNT_ID.`in`(accountIds))
        }
        if(dates.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.`in`(dates))
        }
        if(categories.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }
        return selectStep.fetchOne()?.value1()
    }


    fun getLatestItemTransactionDate(
        profileId: Long, debitOrCredit: Boolean,
        accountIds: List<Long>,
        dates: List<LocalDate>,
        categories: List<Long>,
        modes: List<TransactionMode>,
        sellers: List<Long>,
        currentDate: LocalDate): LocalDate? {
        var selectStep = mainContext.select(DSL.max(TRANSACTION.DATED_AT))
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
            .and(TRANSACTION.DATED_AT.lessOrEqual(currentDate))

        if(accountIds.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.ACCOUNT_ID.`in`(accountIds))
        }
        if(dates.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.`in`(dates))
        }
        if(categories.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }
        return selectStep.fetchOne()?.value1()
    }

    fun getTransactionItemDebitSum(
        profileId: Long,
        fromDate: LocalDate?, toDate: LocalDate?,
        accountIds: List<Long>,
        dates: List<LocalDate>,
        categories: List<Long>,
        modes: List<TransactionMode>,
        sellers: List<Long>
    ): BigDecimal {
        var selectStep = mainContext.select(DSL.sum(TRANSACTION_ITEM.AMOUNT)).from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and( TRANSACTION.DEBIT_OR_CREDIT.eq(TRUE_BYTE))
            .and( TRANSACTION_ITEM.IS_REDUCTION.eq(FALSE_BYTE))
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
            selectStep = selectStep.and(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }

        return selectStep.fetchOne()?.value1()?: BigDecimal.ZERO
    }

    fun getTransactionItemCreditSum(
        profileId: Long,
        fromDate: LocalDate?, toDate: LocalDate?,
        accountIds: List<Long>,
        dates: List<LocalDate>,
        categories: List<Long>,
        modes: List<TransactionMode>,
        sellers: List<Long>
    ): BigDecimal {
        var selectStep = mainContext.select(DSL.sum(TRANSACTION_ITEM.AMOUNT)).from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and( TRANSACTION.DEBIT_OR_CREDIT.eq(FALSE_BYTE))
            .and( TRANSACTION_ITEM.IS_REDUCTION.eq(FALSE_BYTE))
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
            selectStep = selectStep.and(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }

        return selectStep.fetchOne()?.value1()?: BigDecimal.ZERO
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionItemRepository::class.java)
    }
}