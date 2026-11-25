package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.entities.dtos.DateAmountSummary
import com.davidgrath.expensetrackerbackend.entities.dtos.ItemSumByCategory
import com.davidgrath.expensetrackerbackend.entities.dtos.StatisticsTransactionItem
import com.davidgrath.expensetrackerbackend.entities.dtos.TopItemSumByCategory
import com.davidgrath.expensetrackerbackend.entities.dtos.TransactionAndItemCount
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.*
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import com.davidgrath.expensetrackerbackend.utils.applyFilters
import com.davidgrath.expensetrackerbackend.utils.applyFiltersNoDateRange
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Record
import org.jooq.SelectJoinStep
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class StatisticsRepository {


    @Autowired
    lateinit var mainContext: DSLContext


    fun getSumByCategory(
        profileId: Long, debitOrCredit: Boolean,
        statisticsFilter: StatisticsFilterDb
    ): List<ItemSumByCategory> {

        var selectStep = mainContext.select(
            CATEGORY.ID.`as`("categoryId"),
            CATEGORY.STRING_ID,
            CATEGORY.IS_CUSTOM,
            CATEGORY.NAME,
            DSL.sum(TRANSACTION_ITEM.AMOUNT).`as`("sum"),
            CATEGORY.ICON.`as`("categoryIcon")
        )
            .from(TRANSACTION_ITEM)
        val joinOnCondition =
            statisticsSelectJoinStep(selectStep)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
        val selectConditionStep = applyFilters(joinOnCondition, statisticsFilter)

        val seekStep =
            selectConditionStep.groupBy(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL, TRANSACTION_ITEM.PROFILE_ID).orderBy(CATEGORY.SORT_KEY)
                .limit(DEFAULT_FETCH_LIMIT)

        val result = seekStep.fetch().into(ItemSumByCategory::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getTransactionAndItemCount(
        profileId: Long,
        statisticsFilter: StatisticsFilterDb,

        ): TransactionAndItemCount {
        var selectStep = mainContext.select(
            DSL.countDistinct(TRANSACTION.ID).`as`("transactionCount"),
            DSL.count(TRANSACTION_ITEM.ID).`as`("itemCount")
        )
            .from(TRANSACTION)
            .innerJoin(TRANSACTION_ITEM).on(itemToTransactionPath)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .leftJoin(SELLER_LOCATION).on(transactionToSellerLocationPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
        selectStep = applyFilters(selectStep, statisticsFilter)

        return selectStep.fetchOne()?.into(TransactionAndItemCount::class.java)!!
    }


    fun getTransactionItemDebitSum(
        profileId: Long,
        statisticsFilter: StatisticsFilterDb
    ): BigDecimal {
        var selectStep = mainContext
            .select(DSL.sum(TRANSACTION_ITEM.AMOUNT))
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(itemToTransactionPath)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .leftJoin(SELLER_LOCATION).on(sellerLocationToSellerPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and( TRANSACTION.DEBIT_OR_CREDIT.eq(TRUE_BYTE))
            .and( TRANSACTION_ITEM.IS_REDUCTION.eq(FALSE_BYTE))

        selectStep = applyFilters(selectStep, statisticsFilter)

        return selectStep.fetchOne()?.value1()?: BigDecimal.ZERO
    }

    fun getTransactionItemCreditSum(
        profileId: Long,
        statisticsFilter: StatisticsFilterDb
    ): BigDecimal {
        var selectStep = mainContext
            .select(DSL.sum(TRANSACTION_ITEM.AMOUNT))
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(itemToTransactionPath)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .leftJoin(SELLER_LOCATION).on(sellerLocationToSellerPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and( TRANSACTION.DEBIT_OR_CREDIT.eq(FALSE_BYTE))
            .and( TRANSACTION_ITEM.IS_REDUCTION.eq(FALSE_BYTE))

        selectStep = applyFilters(selectStep, statisticsFilter)
        return selectStep.fetchOne()?.value1()?: BigDecimal.ZERO
    }

    private fun <T: Record> statisticsSelectJoinStep(selectStep: SelectJoinStep<T>): SelectOnConditionStep<T> {
        return selectStep
            .innerJoin(TRANSACTION).on(itemToTransactionPath)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .leftJoin(SELLER_LOCATION).on(transactionToSellerLocationPath)
    }

    fun getItems(
        profileId: Long,
        statisticsFilter: StatisticsFilterDb
    ): List<StatisticsTransactionItem> {

        var selectStep = mainContext.select(
            TRANSACTION_ITEM.PROFILE_ID,
            TRANSACTION_ITEM.ID,
            TRANSACTION.ID.`as`("transactionId"),
            TRANSACTION_ITEM.AMOUNT,
            TRANSACTION_ITEM.DESCRIPTION,
            CATEGORY.ID.`as`("categoryId"),
            TRANSACTION_ITEM.QUANTITY,
            TRANSACTION_ITEM.BRAND,
            TRANSACTION_ITEM.VARIATION,
            TRANSACTION_ITEM.WEIGHT,
            TRANSACTION_ITEM.WEIGHT_PREFIX,
            TRANSACTION_ITEM.WEIGHT_UNIT,
            TRANSACTION_ITEM.VOLUME,
            TRANSACTION_ITEM.VOLUME_UNIT,
            TRANSACTION_ITEM.VOLUME_PREFIX,
            TRANSACTION_ITEM.IS_REDUCTION,
            TRANSACTION_ITEM.IS_CHARGE,
            DSL.field(DSL.name("lat","firstImageId"), SQLDataType.BIGINT)
        )
            .from(TRANSACTION_ITEM)
            .innerJoin(TRANSACTION).on(itemToTransactionPath)
                .innerJoin(ACCOUNT).on(accountToTransactionPath)
                .innerJoin(CATEGORY).on(categoryToItemPath)
                .leftJoin(SELLER).on(sellerToTransactionPath)
                .leftJoin(SELLER_LOCATION).on(transactionToSellerLocationPath)
            .leftJoin(
                DSL.lateral(
                    mainContext.select(IMAGE.ID.`as`("firstImageId")).from(IMAGE)
                        .innerJoin(TRANSACTION_ITEM_IMAGES).on(imageToItemImagesPath)
                        .where(itemToItemImagesPath)
                        .orderBy(TRANSACTION_ITEM_IMAGES.SORT_KEY)
                        .limit(1)
                ).`as`("lat")
            ).on(DSL.trueCondition())

                .where(ACCOUNT.PROFILE_ID.eq(profileId))
        val selectConditionStep = applyFilters(selectStep, statisticsFilter)



        val result = selectConditionStep.fetch().into(StatisticsTransactionItem::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun getTransactionCumSumByDateRecursive(
        profileId: Long, debitOrCredit: Boolean,
        fromDate: LocalDate, toDate: LocalDate,
        statisticsFilter: StatisticsFilterDb
    ): List<DateAmountSummary> {

        val aggregateDate = TRANSACTION.DATED_AT.`as`("aggregateDate")
        val recursive = mainContext
            .withRecursive(
                DSL.name("date_sequence").fields("d")
                    .`as`(
                        DSL
                            .select(DSL.localDate(fromDate!!))
                            .unionAll(
                                DSL.select(DSL.localDateAdd(DSL.field("d", SQLDataType.LOCALDATE), 1, DatePart.DAY) )
                                    .from(DSL.table(DSL.name("date_sequence")))
                                    .where(DSL.localDateAdd(DSL.field("d", SQLDataType.LOCALDATE), 1, DatePart.DAY).lessOrEqual(toDate!!))
                            )
                    )
            )
            .with(
                DSL.name("main_query")
                    .`as`(
                        applyFiltersNoDateRange(
                            mainContext.select(aggregateDate, DSL.sum(TRANSACTION_ITEM.AMOUNT).`as`("sum"))
                                .from(TRANSACTION_ITEM)
                                .innerJoin(TRANSACTION).on(itemToTransactionPath)
                                .innerJoin(ACCOUNT).on(accountToTransactionPath)
                                .innerJoin(CATEGORY).on(categoryToItemPath)
                                .leftJoin(SELLER).on(sellerToTransactionPath)
                                .leftJoin(SELLER_LOCATION).on(transactionToSellerLocationPath)
                                .where(ACCOUNT.PROFILE_ID.eq(profileId))
                                .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
                                .and(TRANSACTION.DATED_AT.greaterOrEqual(fromDate))
                                .and(TRANSACTION.DATED_AT.lessOrEqual(toDate))
                            , statisticsFilter
                        )
                            .groupBy(aggregateDate)
                            .orderBy(aggregateDate)
                    )
            )
            .with(DSL.name("joined_sums")
                .`as`(
                    mainContext.select(
                        DSL.field(DSL.name("date_sequence","d"))!!.`as`("aggregateDate"),
                        DSL.ifnull(DSL.field(DSL.name("main_query", "sum")), BigDecimal.ZERO).`as`("aggregateAmount")
                    )
                        .from(DSL.table("date_sequence"))
                        .leftJoin(DSL.table("main_query"))
                        .on(DSL.field(DSL.name("main_query", "aggregateDate"), SQLDataType.LOCALDATE)!!.eq(DSL.field(DSL.name("date_sequence", "d"), SQLDataType.LOCALDATE))
                        ))
            )
            .select(
                DSL.field(DSL.name("joined_sums", "aggregateDate"), SQLDataType.LOCALDATE),
                DSL.sum(DSL.field("aggregateAmount", SQLDataType.DECIMAL)).over(DSL.orderBy(DSL.field("aggregateDate")).rangeBetweenUnboundedPreceding().andCurrentRow()).`as`("sum")
            )
            .from(DSL.table("joined_sums"))
            .orderBy(DSL.field("aggregateDate"))

        val result = recursive.fetch().into(DateAmountSummary::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getTopNExpensesByCategory(
        profileId: Long, debitOrCredit: Boolean, n: Int,
        statisticsFilter: StatisticsFilterDb
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
            .leftJoin(SELLER_LOCATION).on(transactionToSellerLocationPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId)) //TODO Try replacing this specific clause with noCondition, it should yield the same because of the rework
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte())) //TODO Factor reductions

        selectStep = applyFilters(selectStep, statisticsFilter)

        val seekStep = selectStep
            .orderBy(sum.desc())
        val alias = seekStep.asTable()
        val finalSelect = mainContext.selectFrom(alias).where(rank.lessOrEqual(n))
        return finalSelect.fetch().into(TopItemSumByCategory::class.java)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsRepository::class.java)

    }
}