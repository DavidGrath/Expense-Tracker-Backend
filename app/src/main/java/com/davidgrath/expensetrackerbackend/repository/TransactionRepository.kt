package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.dtos.*
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.*
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import com.davidgrath.expensetrackerbackend.utils.applyFiltersNoDateRange
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
class TransactionRepository {

    @Autowired
    lateinit var mainContext: DSLContext

    //region Create

    fun insertTransaction(transaction: TransactionPojo): Long {
        val transactionDb = mainContext.newRecord(TRANSACTION, transaction)
        transactionDb.store()
        return transactionDb.id!!
    }


    fun insertTransactionTrx(trxContext: DSLContext, transaction: TransactionPojo): Long {
        val transactionDb = trxContext.newRecord(TRANSACTION, transaction)
        transactionDb.store()
        return transactionDb.id!!
    }

    //endregion

    //region Read

    //TODO This method is basically useless
    fun getTransactionSumByDate(
        profileId: Long, debitOrCredit: Boolean,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<DateAmountSummary> {
        val aggregateDate = TRANSACTION.DATED_AT.`as`("aggregateDate")
        var selectStep = mainContext.select(aggregateDate, DSL.sum(TRANSACTION.AMOUNT).`as`("sum")).from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .innerJoin(TRANSACTION_ITEM).on(itemToTransactionPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
        if(fromDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.greaterOrEqual(fromDate))
        }
        if(toDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.lessOrEqual(toDate))
        }
        if(accountIds.isNotEmpty()) {
            selectStep = selectStep.and(ACCOUNT.ID.`in`(accountIds))
        }
        if(dates.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.`in`(dates))
        }
        if(categories.isNotEmpty()) {
            selectStep = selectStep.and(CATEGORY.ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(SELLER.ID.`in`(sellers))
        }
        val seekStep = selectStep.groupBy(aggregateDate).orderBy(aggregateDate)
            .limit(DEFAULT_FETCH_LIMIT)
        val result = seekStep.fetch().into(DateAmountSummary::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getMaxSortKeyInDayForAccount(profileId: Long, accountId: Long, date: LocalDate): Short? {
        return getMaxSortKeyInDayForAccountTrx(mainContext, profileId, accountId, date)
    }

    fun getMaxSortKeyInDayForAccountTrx(trxContext: DSLContext, profileId: Long, accountId: Long, date: LocalDate): Short? {
        val selectStep = trxContext.select(DSL.max(TRANSACTION.SORT_KEY))
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .where(ACCOUNT.ID.eq(accountId))
            .and(TRANSACTION.DATED_AT.eq(date))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.value1()
    }


    fun getMaxOrdinalSortKeyAndOccupancySlot(profileId: Long, accountId: Long): OrdinalSortKeyOccupancySlot {
        return getMaxOrdinalSortKeyAndOccupancySlotTrx(mainContext, profileId, accountId)
    }


    fun getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext: DSLContext, profileId: Long, accountId: Long): OrdinalSortKeyOccupancySlot {
        val selectStep = trxContext.select(
            DSL.max(TRANSACTION.ORDINAL).`as`("ordinal"),
            DSL.max(TRANSACTION.SORT_KEY).`as`("sortKey"),
            DSL.max(TRANSACTION.OCCUPANCY_SLOT).`as`("occupancySlot"),
        ).from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .where(ACCOUNT.ID.eq(accountId))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(OrdinalSortKeyOccupancySlot::class.java)!!
    }

    fun getAllOccupancySlotsTrx(trxContext: DSLContext, profileId: Long, accountId: Long): List<Short> {
        val selectStep = trxContext.select(
            TRANSACTION.OCCUPANCY_SLOT
        ).from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .where(ACCOUNT.ID.eq(accountId))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))

        return selectStep.fetch().map { it.value1() }
    }


    fun getIdsAndSortKeysByProfileIdAndAccountIdAndDateTrx(trxContext: DSLContext, profileId: Long, accountId: Long, date: LocalDate): List<IdAndSortKey> {
        val selectStep = trxContext.select(TRANSACTION.ID, TRANSACTION.SORT_KEY)
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .where(ACCOUNT.ID.eq(accountId))
            .and(TRANSACTION.DATED_AT.eq(date))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
        val result = selectStep.fetch().into(IdAndSortKey::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun moveSortKeyTrx(trxContext: DSLContext, profileId: Long, categoryId: Long, accountId: Long, date: LocalDate, sortKey: Short): Int {

        val updateStep = trxContext.update(TRANSACTION.innerJoin(ACCOUNT).on(accountToTransactionPath))
            .set(TRANSACTION.SORT_KEY, sortKey)
            .where(TRANSACTION.ID.eq(categoryId))
            .and(TRANSACTION.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.DATED_AT.eq(date))
            .and(ACCOUNT.ID.eq(accountId))

        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }


    fun getByProfileIdAndId(profileId: Long, id: Long): TransactionPojo? {
        val selectStep = mainContext.select(TRANSACTION.asterisk())
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.ID.eq(id))

        return selectStep.fetchOne()?.into(TransactionPojo::class.java)
    }

    //TODO Do I actually need this? Seems I only need it for testing. Consider moving
    fun getTransactions(
        profileId: Long,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<TransactionPojo> {

        var selectStep = mainContext.select(TRANSACTION.asterisk())
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .innerJoin(TRANSACTION_ITEM).on(itemToTransactionPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId)) //TODO candidate for replacement
        if(fromDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.greaterOrEqual(fromDate))
        }
        if(toDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.lessOrEqual(toDate))
        }
        if(accountIds.isNotEmpty()) { //TODO AccountIds should contain at least 1 account. Verify from the frontend
            selectStep = selectStep.and(ACCOUNT.ID.`in`(accountIds))
        }
        if(dates.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.`in`(dates))
        }
        if(categories.isNotEmpty()) {
            selectStep = selectStep.and(CATEGORY.ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(SELLER.ID.`in`(sellers))
        }
        val seekStep = selectStep.orderBy(TRANSACTION.DATED_AT)
            .limit(DEFAULT_FETCH_LIMIT)
        val result = seekStep.fetch().into(TransactionPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    //TODO add filter test. Consider moving all statistics methods to new class
    fun getTransactionsWithItems(
        profileId: Long,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<TransactionWithItemAndAccount> {
        val transactionItemId = TRANSACTION_ITEM.ID.`as`("itemId")
        var selectStep = mainContext.select(
            TRANSACTION.ID.`as`("transactionId"),
            TRANSACTION.AMOUNT.`as`("transactionAmount"),
            TRANSACTION.DEBIT_OR_CREDIT.`as`("transactionDebitOrCredit"),
            TRANSACTION.DATED_AT.`as`("transactionDatedAt"),
            TRANSACTION.TIME.`as`("transactionTime"),
            TRANSACTION.MODE.`as`("transactionMode"),
            TRANSACTION.NOTE.`as`("transactionNote"),
                (mainContext.select(DSL.count(DOCUMENT.ID)).from(DOCUMENT).where(DOCUMENT.TRANSACTION_ID.eq(TRANSACTION.ID))).asField<Int>("documentCount"),
            ACCOUNT.ID.`as`("accountId"),
            ACCOUNT.PROFILE_ID.`as`("accountProfileId"),
            ACCOUNT.CURRENCY_CODE.`as`("accountCurrencyCode"),
            ACCOUNT.NAME.`as`("accountName"),
            ACCOUNT.IS_DEFAULT.`as`("accountIsDefault"),
            transactionItemId,
            (
                    mainContext.select(DSL.count(IMAGE.ID))
                        .from(IMAGE)
                        .innerJoin(TRANSACTION_ITEM_IMAGES)
                        .on(TRANSACTION_ITEM_IMAGES.IMAGE_ORDINAL.eq(IMAGE.ORDINAL).and(TRANSACTION_ITEM_IMAGES.PROFILE_ID.eq(IMAGE.PROFILE_ID)))
                        .where(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ORDINAL.eq(TRANSACTION_ITEM.ORDINAL).and(TRANSACTION_ITEM_IMAGES.TRANSACTION_ORDINAL.eq(TRANSACTION_ITEM.TRANSACTION_ORDINAL)))
                        .asField<Int>("itemImageCount")
                    ),
            TRANSACTION_ITEM.DESCRIPTION.`as`("itemDescription"),
            TRANSACTION_ITEM.AMOUNT.`as`("itemAmount"),
            TRANSACTION_ITEM.QUANTITY.`as`("itemQuantity"),
            TRANSACTION_ITEM.BRAND.`as`("itemBrand"),
            TRANSACTION_ITEM.VARIATION.`as`("itemVariation"),
            TRANSACTION_ITEM.IS_REDUCTION.`as`("itemIsReduction"), //TODO lateral join replacement
            (mainContext.select(CATEGORY.ID).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL.eq(CATEGORY.ORDINAL).and(TRANSACTION_ITEM.PROFILE_ID.eq(CATEGORY.PROFILE_ID))).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<Long>("categoryId")),
            (mainContext.select(CATEGORY.NAME).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL.eq(CATEGORY.ORDINAL).and(TRANSACTION_ITEM.PROFILE_ID.eq(CATEGORY.PROFILE_ID))).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<String>("categoryName")),
            (mainContext.select(CATEGORY.IS_CUSTOM).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL.eq(CATEGORY.ORDINAL).and(TRANSACTION_ITEM.PROFILE_ID.eq(CATEGORY.PROFILE_ID))).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<Byte>("categoryIsCustom")),
            (mainContext.select(CATEGORY.STRING_ID).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL.eq(CATEGORY.ORDINAL).and(TRANSACTION_ITEM.PROFILE_ID.eq(CATEGORY.PROFILE_ID))).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<String>("categoryStringId")),
            (mainContext.select(CATEGORY.ICON).from(CATEGORY).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL.eq(CATEGORY.ORDINAL).and(TRANSACTION_ITEM.PROFILE_ID.eq(CATEGORY.PROFILE_ID))).where(TRANSACTION_ITEM.ID.eq(transactionItemId)).asField<String>("categoryIcon")),
        )
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .innerJoin(TRANSACTION_ITEM).on(itemToTransactionPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
        if(fromDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.greaterOrEqual(fromDate))
        }
        if(toDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.lessOrEqual(toDate))
        }
        if(accountIds.isNotEmpty()) {
            selectStep = selectStep.and(ACCOUNT.ID.`in`(accountIds))
        }
        if(dates.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.`in`(dates))
        }
        if(categories.isNotEmpty()) {
            selectStep = selectStep.and(CATEGORY.ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(SELLER.ID.`in`(sellers))
        }
        val seekStep = selectStep.orderBy(TRANSACTION.DATED_AT, TRANSACTION.ID)
            .limit(DEFAULT_FETCH_LIMIT)
        val result = seekStep.fetch().into(TransactionWithItemAndAccount::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }



    fun getEarliestTransactionDate(
        profileId: Long,
        accountIds: List<Long>
    ): LocalDate? {
        var selectStep = mainContext.select(
            DSL.min(TRANSACTION.DATED_AT)
        )
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))

        if (accountIds.isNotEmpty()) {
            selectStep = selectStep.and(ACCOUNT.ID.`in`(accountIds))
        }
        return selectStep.fetchOne()?.value1()
    }

    //TODO add filter test. Consider moving all statistics methods to new class
    fun getTransactionCumSumByDate(
        profileId: Long, debitOrCredit: Boolean,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<DateAmountSummary> {

        val window = DSL.orderBy(TRANSACTION.DATED_AT).rangeBetweenUnboundedPreceding().andCurrentRow()
        var selectStep = mainContext.select(
            TRANSACTION.DATED_AT.`as`("aggregateDate"),
            DSL.sum(TRANSACTION.AMOUNT).over(window)
                .`as`("sum"))
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .innerJoin(TRANSACTION_ITEM).on(itemToTransactionPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
        if(fromDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.greaterOrEqual(fromDate))
        }
        if(toDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.lessOrEqual(toDate))
        }
        if(accountIds.isNotEmpty()) {
            selectStep = selectStep.and(ACCOUNT.ID.`in`(accountIds))
        }
        if(dates.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.`in`(dates))
        }
        if(categories.isNotEmpty()) {
            selectStep = selectStep.and(CATEGORY.ID.`in`(categories))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(SELLER.ID.`in`(sellers))
        }
        val alias = selectStep.asTable("x")
            val selectFrom = mainContext.select(
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


    //TODO add filter test. Consider moving all statistics methods to new class
    fun getTopNExpensesByCategory(
        profileId: Long, debitOrCredit: Boolean, n: Int,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<TopItemSumByCategory> {

        val window = DSL.partitionBy(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL, TRANSACTION_ITEM.PROFILE_ID).orderBy(TRANSACTION_ITEM.AMOUNT.desc())
            .rowsBetweenUnboundedPreceding().andUnboundedFollowing()
        val sum = DSL.sum(TRANSACTION_ITEM.AMOUNT).over(window).`as`("sum")

        val rank = DSL.denseRank().over(window).`as`("denseRank")
        var selectStep = mainContext.select(
            TRANSACTION_ITEM.ID.`as`("itemId"),TRANSACTION_ITEM.DESCRIPTION.`as`("itemDescription"),
            TRANSACTION_ITEM.AMOUNT.`as`("itemAmount"), rank,
            CATEGORY.ID.`as`("categoryId"),
            CATEGORY.STRING_ID, CATEGORY.IS_CUSTOM, CATEGORY.NAME,
            sum,
            CATEGORY.ICON.`as`("categoryIcon")
        )
            .from(TRANSACTION_ITEM)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .innerJoin(TRANSACTION).on(itemToTransactionPath)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId)) //TODO Try replacing this specific clause with noCondition, it should yield the same because of the rework
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
        if(fromDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.greaterOrEqual(fromDate))
        }
        if(toDate != null) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.lessOrEqual(toDate))
        }
        if(accountIds.isNotEmpty()) {
            selectStep = selectStep.and(ACCOUNT.ID.`in`(accountIds))
        }
        if(dates.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.DATED_AT.`in`(dates))
        }
        if(categories.isNotEmpty()) {
            selectStep = selectStep.and(CATEGORY.ID.`in`(categories))

        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(SELLER.ID.`in`(sellers))
        }

        val seekStep = selectStep
            .orderBy(sum.desc())
        val alias = seekStep.asTable()
        val finalSelect = mainContext.selectFrom(alias).where(rank.lessOrEqual(n))
        return finalSelect.fetch().into(TopItemSumByCategory::class.java)
    }

    fun getAllByIds(profileId: Long, ids: List<Long>): List<TransactionPojo> {
        val selectStep = mainContext.select(TRANSACTION.asterisk())
            .from(TRANSACTION)
            .where(TRANSACTION.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.ID.`in`(ids))
            .orderBy(TRANSACTION.DATED_AT, TRANSACTION.ACCOUNT_ORDINAL, TRANSACTION.SORT_KEY, TRANSACTION.ID)

        val result = selectStep.fetch().into(TransactionPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun getDistinctAccountIdsByIds(profileId: Long, ids: List<Long>): List<Long> {
        val selectStep = mainContext.selectDistinct(ACCOUNT.ID)
            .from(ACCOUNT)
            .innerJoin(TRANSACTION).on(accountToTransactionPath)
            .where(TRANSACTION.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.ID.`in`(ids))

        val result = selectStep.fetch().map { it.value1() }
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getExcludedTransactionItemStats(profileId: Long, itemIds: List<Long>, excludeCharges: Boolean): List<StatisticsTransactionFilterData> {
        var itemChargesCondition = DSL.noCondition()
        var chargesCondition = DSL.noCondition()
        if(excludeCharges) {
            itemChargesCondition = TRANSACTION_ITEM.IS_CHARGE.eq(FALSE_BYTE)
            chargesCondition = TRANSACTION_ITEM.IS_CHARGE.eq(TRUE_BYTE)
        }
        val selectStep = mainContext
            .select(
                TRANSACTION.PROFILE_ID,
                TRANSACTION.ID,
                DSL.table("lat").asterisk(),
                DSL.table("lat2").asterisk()
            ).from(TRANSACTION,
                DSL.lateral(mainContext.select(
                    DSL.count(TRANSACTION_ITEM.ID).`as`("excludedItemCount"),
                    DSL.ifnull(DSL.sum(TRANSACTION_ITEM.AMOUNT), BigDecimal.ZERO).`as`("excludedItemAmount")
                )
                    .from(TRANSACTION_ITEM)
                    .where(itemToTransactionPath)
                    .and(TRANSACTION_ITEM.ID.notIn(itemIds))
                    .and(TRANSACTION_ITEM.IS_REDUCTION.eq(FALSE_BYTE))
                    .and(itemChargesCondition)
                ).`as`("lat"),
                DSL.lateral(mainContext.select(
                    DSL.ifnull(DSL.sum(TRANSACTION_ITEM.AMOUNT), BigDecimal.ZERO).`as`("excludedChargesAmount") //TODO This is a prototype. Add a new test when I fully implement charges
                )
                    .from(TRANSACTION_ITEM)
                    .where(itemToTransactionPath)
                    .and(TRANSACTION_ITEM.IS_CHARGE.eq(TRUE_BYTE))
                    .and(DSL.condition(excludeCharges))
                    .and(itemChargesCondition)
                ).`as`("lat2")
                )

            .where(TRANSACTION.PROFILE_ID.eq(profileId))

        val result = selectStep.fetch().into(StatisticsTransactionFilterData::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    //endregion

    //region Delete


    //endregion


    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionRepository::class.java)
    }
}