package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Image
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.User
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import org.jooq.DSLContext
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

    fun getAllById(transactionId: Long): List<TransactionItemPojo> {
        return dslContext.selectFrom(TRANSACTION_ITEM)
            .where(TRANSACTION_ITEM.TRANSACTION_ID.eq(transactionId))
            .fetch().into(TransactionItemPojo::class.java)
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(TRANSACTION_ITEM).where("true")
        return delete.execute()
    }
}