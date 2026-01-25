package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.*
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class TestRepository {

    @Autowired
    open lateinit var dslContext: DSLContext

    fun deleteAllUserDrafts(): Int {
        val delete = dslContext.deleteFrom(USER_DRAFT).where("true")
        return delete.execute()
    }

    fun deleteAllItemImages(): Int {
        val delete = dslContext.deleteFrom(TRANSACTION_ITEM_IMAGES).where("true")
        return delete.execute()
    }

    fun deleteAllItems(): Int {
        val delete = dslContext.deleteFrom(TRANSACTION_ITEM).where("true")
        return delete.execute()
    }


    fun deleteAllImages(): Int {
        val delete = dslContext.deleteFrom(IMAGE).where("true")
        return delete.execute()
    }

    fun deleteAllCategories(): Int {
        val delete = dslContext.deleteFrom(CATEGORY).where("true")
        return delete.execute()
    }


    fun deleteAllDocuments(): Int {
        val delete = dslContext.deleteFrom(DOCUMENT).where("true")
        return delete.execute()
    }

    fun deleteAllTransactions(): Int {
        val delete = dslContext.deleteFrom(TRANSACTION).where("true")
        return delete.execute()
    }


    fun deleteAllAccounts(): Int {
        val delete = dslContext.deleteFrom(ACCOUNT).where("true")
        return delete.execute()
    }


    fun deleteAllSellerLocations(): Int {
        val delete = dslContext.deleteFrom(SELLER_LOCATION).where("true")
        return delete.execute()
    }


    fun deleteAllSellers(): Int {
        val delete = dslContext.deleteFrom(SELLER).where("true")
        return delete.execute()
    }


    fun deleteAllProfiles(): Int {
        val delete = dslContext.deleteFrom(PROFILE).where("true")
        return delete.execute()
    }


    fun deleteAllUsers(): Int {
        val delete = dslContext.deleteFrom(USER).where("true")
        return delete.execute()
    }
}