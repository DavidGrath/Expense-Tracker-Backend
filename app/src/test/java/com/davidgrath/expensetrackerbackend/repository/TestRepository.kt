package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.*
import com.davidgrath.expensetrackerbackend.service.StatisticsService
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDate

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

    fun getTransactions(
        profileId: Long,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<TransactionPojo> {

        var selectStep = dslContext.select(TRANSACTION.asterisk())
            .from(TRANSACTION)
            .innerJoin(ACCOUNT).on(accountToTransactionPath)
            .innerJoin(TRANSACTION_ITEM).on(itemToTransactionPath)
            .innerJoin(CATEGORY).on(categoryToItemPath)
            .leftJoin(SELLER).on(sellerToTransactionPath)
            .where(TRANSACTION.PROFILE_ID.eq(profileId))
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
        val seekStep = selectStep.orderBy(TRANSACTION.DATED_AT)
            .limit(DEFAULT_FETCH_LIMIT)
        val result = seekStep.fetch().into(TransactionPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(TestRepository::class.java)
    }
}