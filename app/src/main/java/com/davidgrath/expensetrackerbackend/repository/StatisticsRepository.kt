package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.entities.views.ItemSumByCategory
import com.davidgrath.expensetrackerbackend.entities.views.TransactionAndItemCount
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.CATEGORY
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER_LOCATION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import com.davidgrath.expensetrackerbackend.utils.applyFilters
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class StatisticsRepository {


    @Autowired
    lateinit var mainContext: DSLContext

    //TODO add filter test.
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
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .innerJoin(TRANSACTION).on(itemToTransactionPath)
            .innerJoin(SELLER).on(sellerToTransactionPath)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(TRANSACTION.DEBIT_OR_CREDIT.eq(debitOrCredit.toByte()))
        selectStep = applyFilters(selectStep, statisticsFilter)

        val seekStep =
            selectStep.groupBy(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL, TRANSACTION_ITEM.PROFILE_ID).orderBy(CATEGORY.SORT_KEY)
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
            .innerJoin(SELLER).on(sellerToTransactionPath)
            .leftJoin(SELLER_LOCATION).on(transactionToSellerLocationPath)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))  //TODO candidate for replacement
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
            .where(ACCOUNT.PROFILE_ID.eq(profileId)) //TODO candidate for replacement
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
            .where(ACCOUNT.PROFILE_ID.eq(profileId)) //TODO candidate for removal
            .and( TRANSACTION.DEBIT_OR_CREDIT.eq(FALSE_BYTE))
            .and( TRANSACTION_ITEM.IS_REDUCTION.eq(FALSE_BYTE))

        selectStep = applyFilters(selectStep, statisticsFilter)
        return selectStep.fetchOne()?.value1()?: BigDecimal.ZERO
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsRepository::class.java)

    }
}