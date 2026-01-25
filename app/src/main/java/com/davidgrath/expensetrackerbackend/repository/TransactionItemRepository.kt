package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.views.StringFieldWithTimestamp
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class TransactionItemRepository {

    @Autowired
    open lateinit var dslContext: DSLContext
    open var TRANSACTION_ITEM: TransactionItem = TransactionItem.TRANSACTION_ITEM

    fun insertTransactionItem(item: TransactionItemPojo): Long {
        val itemDb = dslContext.newRecord(TRANSACTION_ITEM, item)
        itemDb.store()
        return itemDb.id!!
    }

    fun getAllByTransactionId(profileId: Long, transactionId: Long): List<TransactionItemPojo> {
        val selectStep = dslContext.select(TRANSACTION_ITEM.asterisk())
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
        val selectStep = dslContext
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
        val selectStep = dslContext
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
        val selectStep = dslContext
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

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(TRANSACTION_ITEM).where("true")
        return delete.execute()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionItemRepository::class.java)
    }
}