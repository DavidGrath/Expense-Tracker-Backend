package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.ImageWithItemId
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemImagesPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.*
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
open class ItemImagesRepository {

    @Autowired
    open lateinit var mainContext: DSLContext

    fun insertItemImage(transactionItemImages: TransactionItemImagesPojo): Long {
        val itemImageDb = mainContext.newRecord(TRANSACTION_ITEM_IMAGES, transactionItemImages)
        itemImageDb.store()
        return itemImageDb.id!!
    }

    fun insertItemImageTrx(trxContext: DSLContext, transactionItemImages: TransactionItemImagesPojo): Long {
        val itemImageDb = trxContext.newRecord(TRANSACTION_ITEM_IMAGES, transactionItemImages)
        itemImageDb.store()
        return itemImageDb.id!!
    }

    fun findByItemId(profileId: Long, itemId: Long): List<ImagePojo> {
        val selectStep = mainContext.select(IMAGE.asterisk())
            .from(IMAGE)
            .innerJoin(TRANSACTION_ITEM_IMAGES).on(TRANSACTION_ITEM_IMAGES.IMAGE_ID.eq(IMAGE.ID))
            .where(IMAGE.PROFILE_ID.eq(profileId))
            .and(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ID.eq(itemId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(ImagePojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun findAllFiltered(profileId: Long,
                        fromDate: LocalDate? = null, toDate: LocalDate? = null,
                        accountIds: List<Long>,
                        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<ImageWithItemId> {
        var selectStep = mainContext.select(
            TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ID.`as`("itemId"), IMAGE.asterisk()
        )
            .from(IMAGE)
            .innerJoin(TRANSACTION_ITEM_IMAGES).on(TRANSACTION_ITEM_IMAGES.IMAGE_ID.eq(IMAGE.ID))
            .innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.ID.eq(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ID))
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
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
                mainContext.select(TRANSACTION.ID).from(TRANSACTION).innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.TRANSACTION_ID.eq(TRANSACTION.ID)).where(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.`in`(categories))
            ))
        }
        if(modes.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.MODE.`in`(modes))
        }
        if(sellers.isNotEmpty()) {
            selectStep = selectStep.and(TRANSACTION.SELLER_ID.`in`(sellers))
        }
        val seekStep = selectStep.orderBy(TRANSACTION.ID, TRANSACTION.DATED_AT)
            .limit(DEFAULT_FETCH_LIMIT)
        val result = seekStep.fetch().into(ImageWithItemId::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(ItemImagesRepository::class.java)
    }
}