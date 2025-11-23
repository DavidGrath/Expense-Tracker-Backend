package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItemImages
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.User
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemImagesPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class ItemImagesRepository {

    @Autowired
    lateinit var dslContext: DSLContext
    val TRANSACTION_ITEM_IMAGES: TransactionItemImages = TransactionItemImages.TRANSACTION_ITEM_IMAGES

    fun insertItemImage(transactionItemImages: TransactionItemImagesPojo): Long {
        val itemImageDb = dslContext.newRecord(TRANSACTION_ITEM_IMAGES, transactionItemImages)
        itemImageDb.store()
        return itemImageDb.id!!
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(TRANSACTION_ITEM_IMAGES).where("true")
        return delete.execute()
    }
}