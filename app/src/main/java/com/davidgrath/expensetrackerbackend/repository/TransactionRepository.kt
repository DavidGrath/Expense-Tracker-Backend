package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.*
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.*
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
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

    fun getTransactionSumByDate( //TODO Improve with recursive CTE, as I did on Android
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
            .limit(DEFAULT_FETCH_LIMIT)
        val result = seekStep.fetch().into(DateAmountSummary::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun getMaxSortKeyInDayForAccount(profileId: Long, accountId: Long, date: LocalDate): Int? {
        val selectStep = dslContext.select(DSL.max(TRANSACTION.SORT_KEY))
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(TRANSACTION.ACCOUNT_ID.eq(accountId))
            .and(TRANSACTION.DATED_AT.eq(date))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.value1()
    }

    /*fun getByUserIdAndId(userId: Long, id: Long): TransactionPojo? {
        val selectStep = dslContext.select(TRANSACTION.asterisk())
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .innerJoin(PROFILE).on(PROFILE.ID.eq(ACCOUNT.PROFILE_ID))
            .where(PROFILE.USER_ID.eq(userId))
            .and(TRANSACTION.ID.eq(id))

        return selectStep.fetchOne()?.into(TransactionPojo::class.java)
    }*/

    fun getByProfileIdAndId(profileId: Long, id: Long): TransactionPojo? {
        val selectStep = dslContext.select(TRANSACTION.asterisk())
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
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
        if(accountIds.isNotEmpty()) { //TODO AccountIds should contain at least 1 account. Verify from the frontend
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
            .limit(DEFAULT_FETCH_LIMIT)
        val result = seekStep.fetch().into(TransactionPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getTransactionsWithItems(
        profileId: Long,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<TransactionWithItemAndAccount> {
        val transactionItemId = TRANSACTION_ITEM.ID.`as`("itemId")
        var selectStep = dslContext.select(
            TRANSACTION.ID.`as`("transactionId"),
            TRANSACTION.AMOUNT.`as`("transactionAmount"),
            TRANSACTION.DEBIT_OR_CREDIT.`as`("transactionDebitOrCredit"),
            TRANSACTION.DATED_AT.`as`("transactionDatedAt"),
            TRANSACTION.TIME.`as`("transactionTime"),
            TRANSACTION.MODE.`as`("transactionMode"),
            TRANSACTION.NOTE.`as`("transactionNote"),
                (dslContext.select(DSL.count(DOCUMENT.ID)).from(DOCUMENT).where(DOCUMENT.TRANSACTION_ID.eq(TRANSACTION.ID))).asField<Int>("documentCount"),
            TRANSACTION.ACCOUNT_ID.`as`("accountId"),
            ACCOUNT.PROFILE_ID.`as`("accountProfileId"),
            ACCOUNT.CURRENCY_CODE.`as`("accountCurrencyCode"),
            ACCOUNT.NAME.`as`("accountName"),
            transactionItemId,
            (dslContext.select(DSL.count(IMAGE.ID)).from(IMAGE).innerJoin(TRANSACTION_ITEM_IMAGES).on(TRANSACTION_ITEM_IMAGES.IMAGE_ID.eq(IMAGE.ID)).where(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ID.eq(TRANSACTION_ITEM.ID)).asField<Int>("itemImageCount")),
            TRANSACTION_ITEM.DESCRIPTION.`as`("itemDescription"),
            TRANSACTION_ITEM.AMOUNT.`as`("itemAmount"),
            TRANSACTION_ITEM.QUANTITY.`as`("itemQuantity"),
            TRANSACTION_ITEM.BRAND.`as`("itemBrand"),
            TRANSACTION_ITEM.VARIATION.`as`("itemVariation"),
            TRANSACTION_ITEM.IS_REDUCTION.`as`("itemIsReduction"),
            (dslContext.select(CATEGORY.ID).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.eq(CATEGORY.ID)).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<Long>("categoryId")),
            (dslContext.select(CATEGORY.NAME).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.eq(CATEGORY.ID)).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<String>("categoryName")),
            (dslContext.select(CATEGORY.IS_CUSTOM).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.eq(CATEGORY.ID)).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<Byte>("categoryIsCustom")),
            (dslContext.select(CATEGORY.STRING_ID).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.eq(CATEGORY.ID)).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<String>("categoryStringId")),
            (dslContext.select(CATEGORY.ICON).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.eq(CATEGORY.ID)).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<String>("categoryIcon")),
        )
            .from(TRANSACTION)
            .innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.TRANSACTION_ID.eq(TRANSACTION.ID))
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
        val seekStep = selectStep.orderBy(TRANSACTION.DATED_AT, TRANSACTION.ID)
            .limit(DEFAULT_FETCH_LIMIT)
        val result = seekStep.fetch().into(TransactionWithItemAndAccount::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
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

    fun getSumByCategory(
        profileId: Long, debitOrCredit: Boolean,
        fromDate: LocalDate?, toDate: LocalDate?, accountIds: List<Long>, dates: List<LocalDate>, categories: List<Long>,
        modes: List<TransactionMode>, sellers: List<Long>): List<ItemSumByCategory> {

        var selectStep = dslContext.select(
            CATEGORY.ID.`as`("categoryId"),
            CATEGORY.STRING_ID, CATEGORY.IS_CUSTOM, CATEGORY.NAME, DSL.sum(TRANSACTION_ITEM.AMOUNT).`as`("sum"), CATEGORY.ICON.`as`("categoryIcon")
        )
            .from(TRANSACTION_ITEM)
            .innerJoin(CATEGORY).on(CATEGORY.ID.eq(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID))
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
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
            selectStep = selectStep.and(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))

        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }
        val seekStep = selectStep.groupBy(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID).orderBy(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID)
            .limit(DEFAULT_FETCH_LIMIT)

        val result = seekStep.fetch().into(ItemSumByCategory::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun getTransactionAndItemCount(
        profileId: Long,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>,
        categories: List<Long>,
        modes: List<TransactionMode>,
        sellers: List<Long>
    ): TransactionAndItemCount {
        var selectStep = dslContext.select(
            DSL.countDistinct(TRANSACTION.ID).`as`("transactionCount"),
            DSL.count(TRANSACTION_ITEM.ID).`as`("itemCount")
        )
            .from(TRANSACTION)
            .innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.TRANSACTION_ID.eq(TRANSACTION.ID))
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
            selectStep = selectStep.and(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }

        return selectStep.fetchOne()?.into(TransactionAndItemCount::class.java)!!
    }


    fun getEarliestTransactionDate(
        profileId: Long,
        accountIds: List<Long>): LocalDate? {
        var selectStep = dslContext.select(
            DSL.min(TRANSACTION.DATED_AT)
        )
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))

        if(accountIds.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.ACCOUNT_ID.`in`(accountIds))
        }
        return selectStep.fetchOne()?.value1()
    }

    fun getTransactionCumSumByDate(
        profileId: Long, debitOrCredit: Boolean,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<DateAmountSummary> {

        val window = DSL.orderBy(TRANSACTION.DATED_AT).rangeBetweenUnboundedPreceding().andCurrentRow()
        var selectStep = dslContext.select(
            TRANSACTION.DATED_AT.`as`("aggregateDate"),
            DSL.sum(TRANSACTION.AMOUNT).over(window)
                .`as`("sum"))
            .from(TRANSACTION)
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
        val alias = selectStep.asTable("x")
            val selectFrom = dslContext.select(
                alias.field("aggregateDate"),
                DSL.max(alias.field("sum"))).from(alias)
        val seekStep =  selectFrom
            .groupBy(alias.field("aggregateDate"))
            .orderBy(alias.field("aggregateDate"))
            .limit(DEFAULT_FETCH_LIMIT)

        val result = seekStep.fetch().into(DateAmountSummary::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun getTopNExpensesByCategory(
        profileId: Long, debitOrCredit: Boolean, n: Int,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<TopItemSumByCategory> {

        val window = DSL.partitionBy(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID).orderBy(TRANSACTION_ITEM.AMOUNT.desc())
            .rowsBetweenUnboundedPreceding().andUnboundedFollowing()
        val sum = DSL.sum(TRANSACTION_ITEM.AMOUNT).over(window).`as`("sum")

        val rank = DSL.denseRank().over(window).`as`("denseRank")
        var selectStep = dslContext.select(
            TRANSACTION_ITEM.ID.`as`("itemId"),TRANSACTION_ITEM.DESCRIPTION.`as`("itemDescription"),
            TRANSACTION_ITEM.AMOUNT.`as`("itemAmount"), rank,
            CATEGORY.ID.`as`("categoryId"),
            CATEGORY.STRING_ID, CATEGORY.IS_CUSTOM, CATEGORY.NAME,
            sum,
            CATEGORY.ICON.`as`("categoryIcon")
        )
            .from(TRANSACTION_ITEM)
            .innerJoin(CATEGORY).on(CATEGORY.ID.eq(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID))
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
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
            selectStep = selectStep.and(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))

        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }

        val seekStep = selectStep
            .orderBy(sum.desc())
        val alias = seekStep.asTable()
        val finalSelect = dslContext.selectFrom(alias).where(rank.lessOrEqual(n))
        return finalSelect.fetch().into(TopItemSumByCategory::class.java)
    }
    //endregion

    //region Delete


    //endregion


    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionRepository::class.java)
    }
}