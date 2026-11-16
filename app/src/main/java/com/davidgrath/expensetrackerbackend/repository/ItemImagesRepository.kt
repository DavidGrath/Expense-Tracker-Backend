package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.dtos.ImageWithItemId
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
            .innerJoin(TRANSACTION_ITEM_IMAGES).on(imageToItemImagesPath)
            .innerJoin(TRANSACTION_ITEM).on(itemToItemImagesPath)
            .where(IMAGE.PROFILE_ID.eq(profileId))
            .and(TRANSACTION_ITEM.ID.eq(itemId))
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
            TRANSACTION_ITEM.ID.`as`("itemId"), IMAGE.asterisk()
        )
            .from(IMAGE)
            .innerJoin(TRANSACTION_ITEM_IMAGES).on(imageToItemImagesPath)
            .innerJoin(TRANSACTION_ITEM).on(itemToItemImagesPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .innerJoin(TRANSACTION).on(itemToTransactionPath)
            .innerJoin(ACCOUNT).on(accountToTransactionPath) //TODO elimination candidate
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